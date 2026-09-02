package me.ranko.autodark.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Provides the last usable device location for sunrise/sunset calculation.
 *
 * The repository owns permission and provider checks so callers do not need
 * to invoke a permission-annotated framework API directly.
 */
class LocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isEnabled(): Boolean = locationManager?.getProviders(true)?.isNotEmpty() == true

    /**
     * Returns the most recent location among enabled providers.
     *
     * A short update request refreshes providers that support an immediate
     * cached result. Failure of one provider must not prevent another provider
     * from being used.
     */
    fun getLastLocation(): Location? {
        if (!hasLocationPermission()) return null
        val manager = locationManager ?: return null
        return readLastKnownLocations(manager)
    }

    @SuppressLint("MissingPermission")
    private fun readLastKnownLocations(manager: LocationManager): Location? {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = Unit
        }
        var latest: Location? = null

        try {
            for (provider in manager.getProviders(true)) {
                try {
                    manager.requestLocationUpdates(
                        provider,
                        50L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                    val candidate = manager.getLastKnownLocation(provider)
                    if (candidate != null && (latest == null || candidate.time > latest!!.time)) {
                        latest = candidate
                    }
                } catch (_: SecurityException) {
                    // A provider may require a permission that was revoked
                    // between the initial check and this query.
                } catch (_: IllegalArgumentException) {
                    // The provider may disappear while the query is running.
                }
            }
        } finally {
            try {
                manager.removeUpdates(listener)
            } catch (_: SecurityException) {
                // The permission may have been revoked during cleanup.
            }
        }
        return latest
    }
}
