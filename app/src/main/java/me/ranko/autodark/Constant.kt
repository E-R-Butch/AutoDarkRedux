package me.ranko.autodark

import android.Manifest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object Constant {

    const val ANDROID_PACKAGE = "android"

    /**
     * Available when internal storage is encrypted
     * SystemServer can initialize block list here
     * */
    @JvmField
    val APP_DATA_DIR = "/data/user_de/0/" + BuildConfig.APPLICATION_ID

    @JvmField
    val BLOCK_LIST_PATH: Path = Paths.get(APP_DATA_DIR + File.separator + "block.txt")

    @JvmField
    val BLOCK_LIST_INPUT_METHOD_CONFIG_PATH: Path = Paths.get(APP_DATA_DIR + File.separator + "hookIME")

    const val PERMISSION_RECEIVE_DARK_BROADCAST = "me.ranko0p.permission.RECEIVE_DARK_BROADCAST"
    const val PERMISSION_SEND_DARK_BROADCAST = "me.ranko0p.permission.SEND_DARK_BROADCAST"

    @JvmField
    val BRAND_ONE_PLUS = "OnePlus".uppercase()

    const val SP_KEY_MASTER_SWITCH = "switch"

    const val SP_AUTO_TIME_SUNRISE = "sunrise"
    const val SP_AUTO_TIME_SUNSET = "sunset"

    const val SP_RESTRICTED_SILENCE = "silence"

    @JvmField
    val COMMAND_GRANT_PM = "pm grant " + BuildConfig.APPLICATION_ID + " " + Manifest.permission.WRITE_SECURE_SETTINGS

    @JvmField
    val COMMAND_GRANT_ADB = "adb -d shell " + COMMAND_GRANT_PM

    /**
     * Force-dark mode.
     *
     * Return **null** when force-dark is untouched.
     */
    const val SYSTEM_PROP_FORCE_DARK = "debug.hwui.force_dark"

    const val SYSTEM_PROP_HOOK_INPUT_METHOD = "debug.hwui.hook_ime"

    const val SYSTEM_SECURE_PROP_DARK_MODE = "ui_night_mode"

    const val COMMAND_SET_FORCE_DARK_ON = "setprop " + SYSTEM_PROP_FORCE_DARK + " true"
    const val COMMAND_SET_FORCE_DARK_OFF = "setprop " + SYSTEM_PROP_FORCE_DARK + " false"
}
