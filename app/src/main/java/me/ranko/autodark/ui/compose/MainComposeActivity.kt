package me.ranko.autodark.ui.compose

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import me.ranko.autodark.ui.MainViewModel
import java.time.LocalTime
import java.util.Locale

class MainComposeActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Companion.Factory(application) }

    private val locationPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            viewModel.onLocationPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                viewModel = viewModel,
                onPickStartTime = { showTimePicker(isStart = true) },
                onPickEndTime = { showTimePicker(isStart = false) },
                onAutoModeClicked = { onAutoModeClicked() }
            )
        }
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

        if (hasFine && hasCoarse) {
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
