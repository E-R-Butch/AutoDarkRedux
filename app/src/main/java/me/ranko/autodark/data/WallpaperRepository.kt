package me.ranko.autodark.data

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Lightweight dual-wallpaper repository - replacement for DarkWallpaperHelper (670 lines).
 *
 * Stores 4 wallpapers (light/dark x home/lock) as files under files/dual_wallpaper/,
 * and applies them via WallpaperManager.setStream() on dark mode switch.
 * No Shizuku, no AOSP asset, no WallpaperPersister needed for static wallpapers.
 *
 * Live wallpapers still require Shizuku and are handled via optional flavor.
 */
class WallpaperRepository(private val context: Context) {
    companion object {
        private const val PREFS = "dual_wallpaper"
        private const val DIR = "dual_wallpaper"
        private const val KEY_LIGHT_HOME = "light_home"
        private const val KEY_LIGHT_LOCK = "light_lock"
        private const val KEY_DARK_HOME = "dark_home"
        private const val KEY_DARK_LOCK = "dark_lock"
        private const val TAG = "WallpaperRepo"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val wm by lazy { WallpaperManager.getInstance(context) }
    private val dir by lazy { File(context.filesDir, DIR).apply { if (!exists()) mkdirs() } }

    fun hasDualWallpaper(): Boolean =
        prefs.contains(KEY_DARK_HOME) || prefs.contains(KEY_DARK_LOCK)

    fun hasAnyWallpaper(): Boolean =
        prefs.contains(KEY_LIGHT_HOME) || prefs.contains(KEY_DARK_HOME)

    /**
     * Save a wallpaper from [uri] for the given [isDark] mode and [which] flag.
     * Copies the content to internal storage for reliable apply after reboot.
     */
    suspend fun saveWallpaper(uri: Uri, isDark: Boolean, which: Int): String = withContext(Dispatchers.IO) {
        val key = keyFor(isDark, which)
        val destName = "${if (isDark) "dark" else "light"}_${flagName(which)}_${System.currentTimeMillis()}.jpg"
        val dest = File(dir, destName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Cannot open $uri")

        // Clean old file for same key
        prefs.getString(key, null)?.let { oldPath ->
            File(oldPath).takeIf { it.exists() && it != dest }?.delete()
        }

        prefs.edit { putString(key, dest.absolutePath) }
        Log.i(TAG, "Saved $key -> $destName")
        dest.absolutePath
    }

    /**
     * Apply wallpapers for [isDark] mode. Falls back to light if dark not set.
     * Uses FLAG_SYSTEM and FLAG_LOCK separately to preserve user choice.
     */
    suspend fun apply(isDark: Boolean) = withContext(Dispatchers.IO) {
        if (!hasAnyWallpaper()) {
            Log.v(TAG, "No dual wallpaper set, skip apply")
            return@withContext
        }

        for (which in listOf(WallpaperManager.FLAG_SYSTEM, WallpaperManager.FLAG_LOCK)) {
            val path = prefs.getString(keyFor(isDark, which), null)
                ?: prefs.getString(keyFor(false, which), null) // fallback light
                ?: continue

            val file = File(path)
            if (!file.exists()) {
                Log.w(TAG, "Wallpaper file missing: $path")
                continue
            }

            try {
                FileInputStream(file).use { input ->
                    val whichFlag = which
                    val id = wm.setStream(input, null, false, whichFlag)
                    Log.i(TAG, "Applied ${flagName(which)} isDark=$isDark id=$id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply wallpaper $which isDark=$isDark", e)
            }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        prefs.edit { clear() }
        Log.i(TAG, "Cleared all dual wallpapers")
    }

    fun getWallpaperPath(isDark: Boolean, which: Int): String? =
        prefs.getString(keyFor(isDark, which), null)

    private fun keyFor(isDark: Boolean, which: Int): String = when {
        isDark && which == WallpaperManager.FLAG_SYSTEM -> KEY_DARK_HOME
        isDark && which == WallpaperManager.FLAG_LOCK -> KEY_DARK_LOCK
        !isDark && which == WallpaperManager.FLAG_SYSTEM -> KEY_LIGHT_HOME
        else -> KEY_LIGHT_LOCK
    }

    private fun flagName(which: Int): String = when (which) {
        WallpaperManager.FLAG_SYSTEM -> "home"
        WallpaperManager.FLAG_LOCK -> "lock"
        else -> "both"
    }
}
