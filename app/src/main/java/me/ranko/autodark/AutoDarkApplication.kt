package me.ranko.autodark

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import org.lsposed.hiddenapibypass.HiddenApiBypass
import me.ranko.autodark.core.DebugTree
import me.ranko.autodark.core.ReleaseTree
import me.ranko.autodark.services.DarkModeTileService
import rikka.sui.Sui
import timber.log.Timber

class AutoDarkApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        HiddenApiBypass.addHiddenApiExemptions("L")
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(ReleaseTree)
        }

        // Material 3 Dynamic Color (Monet) - apply wallpaper-based theming if available (Android 12+)
        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
        } catch (_: Exception) {}

        DarkModeTileService.setUp(this)
    }

    companion object {
        @JvmField
        val isSui: Boolean = Sui.init(BuildConfig.APPLICATION_ID)

        @JvmStatic
        fun isOnePlus(): Boolean = Build.BRAND.uppercase().contains(Constant.BRAND_ONE_PLUS)

        @JvmStatic
        fun checkSecurePermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        @JvmStatic
        fun isLineageOS(): Boolean = Build.DISPLAY.startsWith("lineage")

        @JvmStatic
        fun isComponentEnabled(context: Context, target: Class<*>): Boolean {
            val component = ComponentName(context.packageName, target.name)
            val status = context.packageManager.getComponentEnabledSetting(component)
            return status != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
    }
}
