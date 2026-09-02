package me.ranko.autodark.ui

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import me.ranko.autodark.AutoDarkApplication
import me.ranko.autodark.Constant.SP_KEY_MASTER_SWITCH
import me.ranko.autodark.Constant.SP_RESTRICTED_SILENCE
import me.ranko.autodark.R
import me.ranko.autodark.receivers.DarkModeAlarmReceiver
import me.ranko.autodark.Utils.DarkTimeUtil
import me.ranko.autodark.Utils.ViewUtil
import me.ranko.autodark.core.DarkModeSettings
import me.ranko.autodark.data.CityReference
import me.ranko.autodark.databinding.DialogBottomResstrictedBinding
import timber.log.Timber

enum class DarkSwitch(val id: Int) {
    ON(1),
    OFF(3),
    SHARE(6),
}

class MainViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {

    private val mContext = application

    val darkSettings = DarkModeSettings.getInstance(application)

    private var sp = PreferenceManager.getDefaultSharedPreferences(application)

    /**
     * Control the main switch on/off
     * Set to [DarkSwitch.OFF], all the pending alarm will be canceled.
     *
     * @see     triggerMasterSwitch
     * @see     DarkSwitch
     * */
    private val _switch = MutableLiveData(getSwitchInSP())
    val switch: LiveData<DarkSwitch>
        get() = _switch

    private val _autoMode = MutableLiveData(darkSettings.isAutoMode())
    /**
     * Control the auto mode switch
     * */
    val autoMode: LiveData<Boolean>
        get() = _autoMode

    private val _manualCity = MutableLiveData(darkSettings.getManualCity())
    val manualCity: LiveData<CityReference?>
        get() = _manualCity

    private val _citySearchResults = MutableLiveData<List<CityReference>>(emptyList())
    val citySearchResults: LiveData<List<CityReference>>
        get() = _citySearchResults

    private val _citySearchInProgress = MutableLiveData(false)
    val citySearchInProgress: LiveData<Boolean>
        get() = _citySearchInProgress

    private var citySearchJob: Job? = null

    /**
     * An observable summary text message
     * Allow UI receive messages from the view model
     * */
    private val _summaryText = MutableLiveData<Summary?>()
    val summaryText: LiveData<Summary?>
        get() = _summaryText

    /**
     * A dark mode or wallpaper changes will cause configuration change.
     * Update summary message on [onResume]
     * */
    private var delayedSummary: Summary? = null

    /**
     * Action button for user to trigger dark mode manually
     * while showing summary message
     * */
    private val summaryAction by lazy(LazyThreadSafetyMode.NONE) {
        View.OnClickListener {
            val isDarkMode = darkSettings.isDarkMode() ?: return@OnClickListener
            if (!darkSettings.setDarkMode(isDarkMode.not()))
                _summaryText.value = newSummary(R.string.dark_mode_permission_denied)
        }
    }

    private val _requirePermission = MutableLiveData<Boolean>()
    /**
     * Control permission dialog
     * */
    val requirePermission: LiveData<Boolean>
        get() = _requirePermission

    val isRestricted:Boolean by lazy(LazyThreadSafetyMode.NONE) {!AutoDarkApplication.isComponentEnabled(application, DarkModeAlarmReceiver::class.java) }

    private var isDialogShowed = false

    /**
     * Called when fab on main activity has been clicked
     * */
    fun onFabClicked() = when (_switch.value ?: DarkSwitch.OFF) {
        DarkSwitch.ON -> triggerMasterSwitch(false)

        DarkSwitch.OFF -> triggerMasterSwitch(true)

        DarkSwitch.SHARE -> AboutFragment.shareApp(mContext)
    }

    /**
     * Turn main switch on or off
     *
     * @see    DarkModeSettings.setAllAlarm
     * @see    DarkModeSettings.cancelAllAlarm
     * */
    private fun triggerMasterSwitch(status: Boolean) {
        if (!AutoDarkApplication.checkSecurePermission(mContext)) {
            // Redux: try Shizuku auto-grant first (mature route)
            // Redux: unified Shizuku (includes root-start) - single mature route
            viewModelScope.launch {
                if (me.ranko.autodark.core.ShizukuApi.unifiedGrant(mContext)) {
                    triggerMasterSwitch(status)
                    return@launch
                }
                _requirePermission.value = true
            }
            return
        }

        _switch.value = if (status) DarkSwitch.ON else DarkSwitch.OFF
        val oldNightMode: Boolean = darkSettings.isDarkMode() ?: false

        // delay 360ms to let button animation finish
        viewModelScope.launch {
            saveSwitch(status)
            delay(360L)
            if (status) {
                darkSettings.setAllAlarm()
            } else {
                darkSettings.cancelAllAlarm()
            }

            if (darkSettings.isDarkMode() != oldNightMode) {
                // change wallpaper too - via WallpaperRepository
                viewModelScope.launch {
                    try {
                        me.ranko.autodark.data.WallpaperRepository(mContext).apply(oldNightMode.not())
                    } catch (_: Exception) {}
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    delay(1200L)
                    _summaryText.value = makeTriggeredSummary()
                    return@launch
                }
                delayedSummary = makeTriggeredSummary()
            } else {
                // show summary message now
                makeTriggeredSummary()?.apply { _summaryText.value = this }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        delayedSummary?.let {
            _summaryText.value = it
            delayedSummary = null
        }
    }

    /**
     * Returns Summary message to show when dark mode triggered
     *
     * @return  A Summary message shows to the user.
     *          **Null** when UIModeManager returned error
     *
     * @see     UiModeManager.getNightMode
     * */
    private fun makeTriggeredSummary(): Summary? {
        when {
            switch.value == DarkSwitch.OFF -> return newSummary(R.string.dark_mode_disabled)

            darkSettings.isAutoMode() -> return newSummary(R.string.dark_mode_summary_auto_on)

            else -> {
                val isDarkMode = darkSettings.isDarkMode() ?: return null
                val displayTime: String
                val textRes: Int = if (isDarkMode) {
                    displayTime = DarkTimeUtil.getDisplayFormattedString(darkSettings.getEndTime())
                    R.string.dark_mode_summary_will_off
                } else {
                    displayTime = DarkTimeUtil.getDisplayFormattedString(darkSettings.getStartTime())
                    R.string.dark_mode_summary_will_on
                }

                val actionStr = mContext.getString(R.string.dark_mode_summary_action)
                return Summary(mContext.getString(textRes, displayTime), actionStr, summaryAction)
            }
        }
    }

    fun onAboutPageChanged(isShowing: Boolean) {
        if (isShowing) {
            _switch.value = DarkSwitch.SHARE
        } else {
            _switch.value = getSwitchInSP()
        }
    }

    /**
     * Called when auto mode is clicked
     * */
    fun onAutoModeClicked() = viewModelScope.launch(Dispatchers.Main) {
        val old = darkSettings.isDarkMode() ?: false
        val result = darkSettings.triggerAutoMode()
        if (result) {
            // send delay message if dark mode changed
            if (old.xor(darkSettings.isDarkMode() == true)) {
                delayedSummary = makeTriggeredSummary()
            } else {
                _summaryText.value = makeTriggeredSummary()
            }
        } else {
            _summaryText.value = newSummary(R.string.app_location_failed)
        }

        // send auto mode status as result
        _autoMode.value = darkSettings.isAutoMode()
    }

    fun searchCities(query: String) {
        citySearchJob?.cancel()
        citySearchJob = viewModelScope.launch {
            _citySearchInProgress.value = true
            delay(120L)
            _citySearchResults.value = darkSettings.searchCities(query)
            _citySearchInProgress.value = false
        }
    }

    fun selectManualCity(city: CityReference) {
        updateManualCity(city, R.string.manual_city_selected)
    }

    fun restoreAutomaticLocation() {
        updateManualCity(null, R.string.automatic_location_restored)
    }

    private fun updateManualCity(city: CityReference?, @StringRes successMessage: Int) {
        viewModelScope.launch {
            if (darkSettings.setManualCity(city)) {
                _manualCity.value = darkSettings.getManualCity()
                _summaryText.value = newSummary(successMessage)
            } else {
                _summaryText.value = newSummary(R.string.manual_city_update_failed)
            }
        }
    }

    fun onRequirePermissionConsumed() {
        _requirePermission.value = false
    }

    fun onLocationPermissionResult(granted: Boolean) {
        // A denial still proceeds through private cache and the coarse bundled
        // time-zone reference. Permission only controls the framework-provider leg.
        onAutoModeClicked()
    }

    fun onSecurePermissionResult() {
        if (AutoDarkApplication.checkSecurePermission(getApplication())) {
            darkSettings.overrideIfNeeded()
            showPermissionSummary(true)
        } else {
            showPermissionSummary(false)
        }
    }

    private fun showPermissionSummary(granted: Boolean) {
        val summary = if (granted) R.string.permission_granted else R.string.permission_failed
        _summaryText.value = newSummary(summary)
    }

    fun onForceDarkClicked(preference: SwitchPreference, scope: CoroutineScope) = scope.launch(Dispatchers.Main) {
        val start = System.currentTimeMillis()
        preference.isEnabled = false

        val succeed = DarkModeSettings.setForceDark(preference.isChecked)
        // wait switch animation finish
        if (System.currentTimeMillis() - start < 500L) delay(600L)

        if (!succeed && isActive) {
            preference.isChecked = preference.isChecked.not()
            _summaryText.value = newSummary(R.string.root_check_failed)
        }
        preference.isEnabled = true
    }

    private fun newSummary(@StringRes message: Int) = Summary(mContext.getString(message))

    /**
     * Some optimize app or OEM performance boost function can disable boot receiver
     * Notify user if this happened and disable __do not show again__ button.
     *
     * */
    fun getRestrictedDialog(activity: AppCompatActivity): BottomSheetDialog? {
        val silence = sp.getBoolean(SP_RESTRICTED_SILENCE, isDialogShowed)
        if (silence && !isRestricted) return null

        // show only once on normal case
        if(isDialogShowed && !isRestricted) return null

        isDialogShowed = true

        return BottomSheetDialog(activity, R.style.AppTheme_BottomSheetDialogDayNight).apply {
            val binding = DialogBottomResstrictedBinding.inflate(
                LayoutInflater.from(context),
                activity.window!!.decorView.rootView as ViewGroup,
                false
            )

            binding.title.text = mContext.getString(
                if (isRestricted) R.string.app_restricted_warning else R.string.app_restricted_title
            )
            binding.btnShutup.isEnabled = !isRestricted

            binding.btnLater.setOnClickListener { dismiss() }

            // add strike font style when restricted
            if (isRestricted) {
                Timber.d("Receiver is disabled!")
                ViewUtil.setStrikeFontStyle(binding.btnShutup, true)
            }

            binding.btnShutup.setOnClickListener {
                sp.edit().putBoolean(SP_RESTRICTED_SILENCE, true).apply()
                dismiss()
            }

            setContentView(binding.root)

            val displayMetrics = activity.resources.displayMetrics
            val screenSize = android.graphics.Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
            val mBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)
            setOnShowListener { mBehavior.peekHeight = screenSize.y }
        }
    }

    private fun getSwitchInSP():DarkSwitch {
        return if (sp.getBoolean(SP_KEY_MASTER_SWITCH, false)) DarkSwitch.ON else DarkSwitch.OFF
    }

    private fun saveSwitch(status: Boolean) {
        sp.edit().putBoolean(SP_KEY_MASTER_SWITCH, status).apply()
    }

    companion object {
        class Factory(private val application: Application) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(application) as T
                }
                throw IllegalArgumentException("Unable to construct viewModel")
            }
        }

        /**
         * Summary message to show when dark mode changed
         * */
        data class Summary(
            val message: String,
            val actionStr: String? = null,
            /**
             * Action button for snack bar
             * */
            val action: View.OnClickListener? = null
        )
    }
}