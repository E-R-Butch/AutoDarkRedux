package me.ranko.autodark.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.function.Consumer
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Provides a usable device location for sunrise/sunset calculation.
 *
 * The repository owns permission and provider checks so callers do not need
 * to invoke a permission-annotated framework API directly.
 */
class LocationRepository(context: Context) {
    private companion object {
        private const val LOCATION_TIMEOUT_MILLIS = 30_000L
        private const val LOCATION_UPDATE_INTERVAL_MILLIS = 1_000L
    }

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

    fun isEnabled(): Boolean {
        val manager = locationManager ?: return false
        return manager.isLocationEnabled && manager.getProviders(true).isNotEmpty()
    }

    /**
     * Returns the newest cached location, or waits for a fresh location when
     * the system has no cached result yet.
     *
     * A device can have location permission and enabled providers while still
     * having no last-known location. In that case, registering a request and
     * immediately removing it makes auto mode fail deterministically. This
     * method keeps the request alive until the first callback or the bounded
     * timeout, and always removes the listener when collection ends.
     */
    suspend fun getLastLocation(): Location? {
        if (!hasLocationPermission()) {
            Timber.i("Location unavailable: permission not granted")
            return null
        }

        val manager = locationManager ?: run {
            Timber.i("Location unavailable: LocationManager is null")
            return null
        }

        val enabledProviders = try {
            manager.getProviders(true)
        } catch (e: SecurityException) {
            Timber.i(e, "Location unavailable: cannot enumerate providers")
            return null
        }

        val cached = readLastKnownLocations(manager, enabledProviders)
        if (cached != null) {
            Timber.i(
                "Using cached location from ${cached.provider ?: "unknown"} " +
                    "(time=${cached.time})"
            )
            return cached
        }

        val requestProviders = enabledProviders
            .asSequence()
            .filterNot { it == LocationManager.PASSIVE_PROVIDER }
            .distinct()
            .toList()
        if (requestProviders.isEmpty()) {
            Timber.i("Location unavailable: no active providers are enabled")
            return null
        }

        Timber.i(
            "No cached location; waiting up to ${LOCATION_TIMEOUT_MILLIS}ms " +
                "for providers $requestProviders"
        )
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            locationUpdates(manager, requestProviders).firstOrNull()
        }

        if (fresh == null) {
            Timber.i("Location unavailable: timed out waiting for a location callback")
        } else {
            Timber.i(
                "Received fresh location from ${fresh.provider ?: "unknown"} " +
                    "(time=${fresh.time})"
            )
        }
        return fresh
    }

    @SuppressLint("MissingPermission")
    private fun readLastKnownLocations(
        manager: LocationManager,
        providers: List<String>
    ): Location? {
        var latest: Location? = null
        for (provider in providers) {
            try {
                val candidate = manager.getLastKnownLocation(provider)
                if (candidate != null && candidate.time > (latest?.time ?: Long.MIN_VALUE)) {
                    latest = candidate
                }
            } catch (_: SecurityException) {
                // A provider may require a permission that was revoked between
                // the initial check and this query.
            } catch (_: IllegalArgumentException) {
                // The provider may disappear while the query is running.
            }
        }
        return latest
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates(
        manager: LocationManager,
        providers: List<String>
    ): Flow<Location> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        currentLocationUpdates(manager, providers)
    } else {
        legacyLocationUpdates(manager, providers)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun currentLocationUpdates(
        manager: LocationManager,
        providers: List<String>
    ) = callbackFlow<Location> {
        val executor = ContextCompat.getMainExecutor(appContext)
        val cancellationSignals = mutableListOf<CancellationSignal>()
        var registered = false

        for (provider in providers) {
            val signal = CancellationSignal()
            cancellationSignals += signal
            try {
                val callback = Consumer<Location?> { location ->
                    if (location == null) {
                        Timber.d("Current location returned null for provider=$provider")
                    } else {
                        trySend(location)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val request = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL_MILLIS)
                        .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                        .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                        .setMaxUpdates(1)
                        .build()
                    manager.getCurrentLocation(provider, request, signal, executor, callback)
                } else {
                    manager.getCurrentLocation(provider, signal, executor, callback)
                }
                registered = true
                Timber.d("Requested current location for provider=$provider")
            } catch (e: SecurityException) {
                Timber.w(e, "Cannot request current location for provider=$provider")
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Provider disappeared while requesting current location: $provider")
            }
        }

        if (!registered) {
            close()
        }

        awaitClose {
            cancellationSignals.forEach { it.cancel() }
            Timber.d("Cancelled current location requests")
        }
    }

    @SuppressLint("MissingPermission")
    private fun legacyLocationUpdates(
        manager: LocationManager,
        providers: List<String>
    ) = callbackFlow<Location> {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }
        }

        var registered = false
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL_MILLIS,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
                registered = true
                Timber.d("Registered location updates for provider=$provider")
            } catch (e: SecurityException) {
                Timber.w(e, "Cannot request location updates for provider=$provider")
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Provider disappeared while requesting updates: $provider")
            }
        }

        if (!registered) {
            close()
        }

        awaitClose {
            try {
                manager.removeUpdates(listener)
                Timber.d("Removed location update listener")
            } catch (e: SecurityException) {
                Timber.w(e, "Cannot remove location update listener")
            }
        }
    }
}
