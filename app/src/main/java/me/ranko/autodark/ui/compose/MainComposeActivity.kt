package me.ranko.autodark.ui.compose

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import me.ranko.autodark.AutoDarkApplication
import me.ranko.autodark.core.ShizukuStatus
import me.ranko.autodark.ui.MainActivity
import me.ranko.autodark.ui.MainViewModel
import me.ranko.autodark.ui.PermissionActivity
import me.ranko.autodark.ui.ShizukuViewModel
import java.util.Locale

class MainComposeActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Companion.Factory(application) }
    private val shizukuViewModel: ShizukuViewModel by viewModels()
    private val securePermissionGranted = mutableStateOf(false)
    private val shizukuStatus = mutableStateOf(ShizukuStatus.DEAD)

    private val locationPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            viewModel.onLocationPermissionResult(granted)
        }

    private val securePermissionLauncher =
        registerForActivityResult(StartActivityForResult()) {
            refreshSystemPermission()
            viewModel.onSecurePermissionResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        lifecycle.addObserver(shizukuViewModel)
        shizukuViewModel.status.observe(this) { status ->
            shizukuStatus.value = status
        }
        refreshSystemPermission()
        viewModel.requirePermission.observe(this) { required ->
            if (required) {
                openSecurePermission()
                viewModel.onRequirePermissionConsumed()
            }
        }
        viewModel.switch.observe(this) {
            refreshSystemPermission()
        }
        setContent {
            MainScreen(
                viewModel = viewModel,
                hasSecurePermission = securePermissionGranted.value,
                shizukuStatus = shizukuStatus.value,
                onPermissionClicked = ::openSecurePermission,
                onPickStartTime = { showTimePicker(isStart = true) },
                onPickEndTime = { showTimePicker(isStart = false) },
                onAutoModeClicked = { onAutoModeClicked() },
                onAdvancedSettingsClicked = {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemPermission()
    }

    private fun refreshSystemPermission() {
        securePermissionGranted.value = AutoDarkApplication.checkSecurePermission(this)
    }

    private fun openSecurePermission() {
        securePermissionLauncher.launch(Intent(this, PermissionActivity::class.java))
    }

    private fun onAutoModeClicked() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (viewModel.autoMode.value == true || viewModel.manualCity.value != null || hasFine || hasCoarse) {
            viewModel.onAutoModeClicked()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showTimePicker(isStart: Boolean) {
        val current = if (isStart) viewModel.darkSettings.getStartTime() else viewModel.darkSettings.getEndTime()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                // Save through the same PreferenceManager instance used by the legacy settings screen.
                val prefKey = if (isStart) "dark_mode_time_start" else "dark_mode_time_end"
                val defaultSp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                defaultSp.edit()
                    .putString(prefKey, String.format(Locale.ROOT, "%02d:%02d", hour, minute))
                    .apply()
                viewModel.darkSettings.setAllAlarm()
            },
            current.hour,
            current.minute,
            true
        ).show()
    }
}
