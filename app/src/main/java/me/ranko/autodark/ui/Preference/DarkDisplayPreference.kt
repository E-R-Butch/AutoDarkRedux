package me.ranko.autodark.ui.Preference

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.preference.Preference
import java.time.LocalTime
import me.ranko.autodark.Utils.DarkTimeUtil

private fun resolvePreferenceStyle(context: Context): Int {
    val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.preferenceStyle))
    val style = attributes.getResourceId(0, android.R.attr.preferenceStyle)
    attributes.recycle()
    return style
}

/**
 * Display preference of Dark mode
 *
 * @author 0rano0P
 */
@Suppress("unused", "WeakerAccess")
class DarkDisplayPreference : Preference {

    private var dialog: Dialog? = null

    // Keep nullable to mirror Java's uninitialized field; assertions added where non-null is required
    private var mTime: LocalTime? = null

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        resolvePreferenceStyle(context)
    )

    constructor(context: Context) : this(context, null)

    override fun onDetached() {
        super.onDetached()
        dialog?.let {
            if (it.isShowing) it.dismiss()
            dialog = null
        }
    }

    override fun onClick() {
        if (dialog == null) dialog = createDialog(context)
        dialog?.show()
    }

    private fun createDialog(context: Context): Dialog {
        val use24HourFormat = android.text.format.DateFormat.is24HourFormat(context)
        val pickedTime = time
        return TimePickerDialog(context, { _, hour, minute ->
            val newTime = LocalTime.of(hour, minute)
            if (newTime != pickedTime) {
                setTime(newTime)
                callChangeListener(newTime)
            }
        }, pickedTime.hour, pickedTime.minute, use24HourFormat)
    }

    private fun setTime(time: LocalTime) {
        persistString(DarkTimeUtil.getPersistFormattedString(time))
        mTime = time
        updateSummary()
        notifyChanged()
    }

    fun getTimeText(): String = DarkTimeUtil.getPersistFormattedString(mTime!!)

    // Expose as Kotlin property `time` for callers like DarkModeSettings (`get(type).time`).
    // Keep Java getter `getTime()` via property accessor. No setter to avoid JVM clash with setTime().
    val time: LocalTime
        get() = mTime!!

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val time = DarkTimeUtil.getPersistLocalTime(getPersistedString(defaultValue as? String))
        setTime(time)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        if (isPersistent) return superState!!
        val myState = SavedState(superState)
        myState.mTime = DarkTimeUtil.getPersistFormattedString(mTime!!)
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        setTime(DarkTimeUtil.getPersistLocalTime(myState.mTime!!))
    }

    private fun updateSummary() {
        summary = DarkTimeUtil.getDisplayFormattedString(mTime!!)
    }

    private class SavedState : BaseSavedState {

        var mTime: String? = null

        constructor(source: Parcel) : super(source) {
            mTime = source.readString()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(mTime)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
