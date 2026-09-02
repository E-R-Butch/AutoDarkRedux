package me.ranko.autodark.ui.Preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference

class DarkSwitchPreference : SwitchPreference {

    private var switchView: View? = null

    var isSwitchable: Boolean = false
        set(value) {
            field = value
            switchView?.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        switchView = holder.findViewById(android.R.id.switch_widget)
        if (!isSwitchable) {
            switchView?.visibility = View.GONE
        }
    }
}
