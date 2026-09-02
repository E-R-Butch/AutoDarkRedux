package me.ranko.autodark.ui

import android.graphics.drawable.Animatable2
import android.view.View
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.marcdonald.simplelicensedisplay.SimpleLicenseDisplay
import me.ranko.autodark.R

fun setActiveImg(fab: FloatingActionButton, state: DarkSwitch) {
    val lastState = fab.tag as? DarkSwitch
    val iconRes = if (lastState == null) {
        when (state) {
            DarkSwitch.ON -> R.drawable.ic_on
            DarkSwitch.OFF -> R.drawable.ic_off
            DarkSwitch.SHARE -> R.drawable.ic_send
        }
    } else {
        val nextStatus = lastState.id - state.id
        when (nextStatus) {
            DarkSwitch.ON.id - DarkSwitch.OFF.id -> R.drawable.ic_on_to_off_anim
            DarkSwitch.OFF.id - DarkSwitch.ON.id -> R.drawable.ic_off_to_on_anim
            DarkSwitch.ON.id - DarkSwitch.SHARE.id -> R.drawable.ic_on_to_share_anim
            DarkSwitch.OFF.id - DarkSwitch.SHARE.id -> R.drawable.ic_off_to_share_anim
            DarkSwitch.SHARE.id - DarkSwitch.ON.id -> R.drawable.ic_share_to_on_anim
            DarkSwitch.SHARE.id - DarkSwitch.OFF.id -> R.drawable.ic_share_to_off_anim
            else -> throw IllegalArgumentException("Unknown status: $nextStatus, $state")
        }
    }

    fab.setImageResource(iconRes)
    fab.tag = state
    if (lastState != null && fab.drawable is Animatable2) {
        (fab.drawable as Animatable2).start()
    }
}

fun setLicense(v: SimpleLicenseDisplay, license: License) {
    val licenseView = v.findViewById<TextView>(com.marcdonald.simplelicensedisplay.R.id.txt_license_license)
    licenseView.text = license.license
    licenseView.visibility = View.VISIBLE

    val titleView = v.findViewById<TextView>(com.marcdonald.simplelicensedisplay.R.id.txt_license_title)
    titleView.text = license.name
    titleView.visibility = View.VISIBLE
}
