package me.ranko.autodark.ui.compose

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import me.ranko.autodark.ui.MainViewModel
import java.time.LocalTime

class MainComposeActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Companion.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                viewModel = viewModel,
                onPickStartTime = { showTimePicker(isStart = true) },
                onPickEndTime = { showTimePicker(isStart = false) }
            )
        }
    }

    private fun showTimePicker(isStart: Boolean) {
        val current = if (isStart) viewModel.darkSettings.getStartTime() else viewModel.darkSettings.getEndTime()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val newTime = LocalTime.of(hour, minute)
                val key = if (isStart) "dark_mode_time_start" else "dark_mode_time_end"
                // Find the preference and trigger change via DarkModeSettings
                val pref = viewModel.darkSettings.let {
                    // Use a dummy preference to trigger onPreferenceChange
                    // Directly save and set alarm
                    it.getStartTime() // just to avoid unused
                }
                // Simplest: save to SharedPreferences and update via DarkModeSettings
                val sp = getSharedPreferences("me.ranko.autodark_preferences", MODE_PRIVATE)
                // The actual pref keys are from MainFragment
                val prefKey = if (isStart) "dark_mode_time_start" else "dark_mode_time_end"
                // Use DarkModeSettings internal: we call onPreferenceChange via a fake Preference
                // Easier: just update via DarkModeSettings.setAllAlarm after saving
                // For now, just delegate to viewModel's darkSettings via reflection of save
                // As fallback, use androidx.preference.PreferenceManager
                val defaultSp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                defaultSp.edit().putString(prefKey, String.format("%02d:%02d", hour, minute)).apply()
                viewModel.darkSettings.setAllAlarm()
            },
            current.hour,
            current.minute,
            true
        ).show()
    }
}
