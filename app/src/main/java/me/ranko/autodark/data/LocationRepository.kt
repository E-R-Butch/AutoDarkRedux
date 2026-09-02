package me.ranko.autodark.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** Explicit legacy callbacks keep requestLocationUpdates safe on API 29. */
internal class CompatLocationListener(
    private val onLocation: (Location) -> Unit
) : LocationListener {
    override fun onLocationChanged(location: Location) = onLocation(location)

    @Deprecated("Deprecated by Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit
}

/**
 * Provides a usable device location for sunrise/sunset calculation.
 *
 * The repository owns permission, provider, private-cache, and China-only
 * keyless fallback checks so callers receive one bounded result.
 */
class LocationRepository(context: Context) {
    private companion object {
        // Match the last known working implementation: keep regular provider
        // updates active for at most three seconds before reading the cache again.
        private const val LOCATION_TIMEOUT_MILLIS = 3_000L
        private const val LOCATION_UPDATE_INTERVAL_MILLIS = 50L
        private const val LOCATION_CACHE_PREFERENCES = "location_cache"
        private const val CACHE_LATITUDE = "latitude"
        private const val CACHE_LONGITUDE = "longitude"
        private const val CACHE_ACCURACY = "accuracy"
        private const val CACHE_TIME = "time"
        private const val CACHE_SOURCE = "source"
        private const val MANUAL_CITY_ID = "manual_city_id"
        private const val MANUAL_CITY_NAME = "manual_city_name"
        private const val MANUAL_CITY_COUNTRY = "manual_city_country"
        private const val MANUAL_CITY_LATITUDE = "manual_city_latitude"
        private const val MANUAL_CITY_LONGITUDE = "manual_city_longitude"
        private const val MANUAL_CITY_TIME_ZONE = "manual_city_time_zone"
    }

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val cache = appContext.getSharedPreferences(
        LOCATION_CACHE_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val timeZoneFallback = TimeZoneLocationFallback(appContext)
    private val cityCatalog = CityCatalog(appContext)

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

    suspend fun searchCities(query: String): List<CityReference> = cityCatalog.search(query)

    fun getManualCity(): CityReference? {
        val id = cache.getString(MANUAL_CITY_ID, null) ?: return null
        val name = cache.getString(MANUAL_CITY_NAME, null) ?: return null
        val country = cache.getString(MANUAL_CITY_COUNTRY, null) ?: return null
        val timeZone = cache.getString(MANUAL_CITY_TIME_ZONE, null) ?: return null
        if (!cache.contains(MANUAL_CITY_LATITUDE) || !cache.contains(MANUAL_CITY_LONGITUDE)) {
            return null
        }
        val latitude = Double.fromBits(cache.getLong(MANUAL_CITY_LATITUDE, Long.MIN_VALUE))
        val longitude = Double.fromBits(cache.getLong(MANUAL_CITY_LONGITUDE, Long.MIN_VALUE))
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        return CityReference(
            id = id,
            name = name,
            countryCode = country,
            latitude = latitude,
            longitude = longitude,
            timeZoneId = timeZone
        )
    }

    fun setManualCity(city: CityReference?) {
        val editor = cache.edit()
        if (city == null) {
            editor
                .remove(MANUAL_CITY_ID)
                .remove(MANUAL_CITY_NAME)
                .remove(MANUAL_CITY_COUNTRY)
                .remove(MANUAL_CITY_LATITUDE)
                .remove(MANUAL_CITY_LONGITUDE)
                .remove(MANUAL_CITY_TIME_ZONE)
                .apply()
            return
        }
        editor
            .putString(MANUAL_CITY_ID, city.id)
            .putString(MANUAL_CITY_NAME, city.name)
            .putString(MANUAL_CITY_COUNTRY, city.countryCode)
            .putLong(MANUAL_CITY_LATITUDE, city.latitude.toBits())
            .putLong(MANUAL_CITY_LONGITUDE, city.longitude.toBits())
            .putString(MANUAL_CITY_TIME_ZONE, city.timeZoneId)
            .apply()
    }

    /**
     * Returns a location using a deterministic resolution policy. Manual city is
     * always authoritative. Framework providers are tried only when usable;
     * private cache and the coarse bundled time-zone reference remain reachable
     * when permission or providers are unavailable.
     */
    suspend fun getLastLocation(): Location? {
        getManualCity()?.let { city ->
            Timber.i("Using manually selected city")
            return city.toLocation("manual_city")
        }

        val manager = locationManager
        val hasPermission = hasLocationPermission()
        val locationEnabled = manager?.isLocationEnabled == true
        val enabledProviders = if (manager != null && hasPermission && locationEnabled) {
            try {
                manager.getProviders(true)
            } catch (e: SecurityException) {
                Timber.i(e, "Cannot enumerate location providers")
                emptyList()
            }
        } else {
            emptyList()
        }
        val plan = LocationResolutionPolicy.plan(
            LocationRuntimeState(
                hasPermission = hasPermission,
                managerAvailable = manager != null,
                locationEnabled = locationEnabled,
                hasEnabledProviders = enabledProviders.isNotEmpty()
            )
        )

        if (!hasPermission) Timber.i("Framework location skipped: permission not granted")
        if (manager == null) Timber.i("Framework location skipped: LocationManager is null")
        if (manager != null && !locationEnabled) Timber.i("Framework location skipped: system location is disabled")
        if (manager != null && hasPermission && locationEnabled && enabledProviders.isEmpty()) {
            Timber.i("Framework location skipped: no enabled providers")
        }

        for (step in plan) {
            when (step) {
                LocationLookupStep.PLATFORM_CACHE -> {
                    val platformManager = manager ?: continue
                    readBestLastKnownLocation(
                        platformManager,
                        enabledProviders,
                        System.currentTimeMillis()
                    )?.let { systemCached ->
                        Timber.i(
                            "Using Android cached location from ${systemCached.provider ?: "unknown"} " +
                                "(time=${systemCached.time}, accuracy=${systemCached.accuracy})"
                        )
                        persistPlatformLocation(systemCached)
                        return systemCached
                    }
                }

                LocationLookupStep.PRIVATE_CACHE -> {
                    readPrivateCache()?.let { appCached ->
                        Timber.i("Using recent private platform-location cache")
                        return appCached
                    }
                }

                LocationLookupStep.PLATFORM_UPDATE -> {
                    val platformManager = manager ?: continue
                    Timber.i(
                        "No cached location; requesting regular updates for up to " +
                            "${LOCATION_TIMEOUT_MILLIS}ms from $enabledProviders"
                    )
                    val callbackLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
                        locationUpdates(platformManager, enabledProviders).firstOrNull { location ->
                            PlatformLocationPolicy.isUsable(
                                location.time,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    val platformLocation = callbackLocation
                        ?: readBestLastKnownLocation(
                            platformManager,
                            enabledProviders,
                            System.currentTimeMillis()
                        )
                    if (platformLocation != null) {
                        Timber.i(
                            "Received Android location from ${platformLocation.provider ?: "unknown"} " +
                                "(time=${platformLocation.time}, accuracy=${platformLocation.accuracy})"
                        )
                        persistPlatformLocation(platformLocation)
                        return platformLocation
                    }
                }

                LocationLookupStep.TIME_ZONE_REFERENCE -> {
                    val zoneId = java.util.TimeZone.getDefault().id
                    Timber.i("Using coarse bundled time-zone reference for $zoneId")
                    val fallbackLocation = timeZoneFallback.locate(zoneId)
                    if (fallbackLocation == null) {
                        Timber.i("Location unavailable: no bundled reference for time zone $zoneId")
                        return null
                    }
                    return fallbackLocation
                }
            }
        }
        return null
    }

    private fun readPrivateCache(): Location? {
        if (!cache.contains(CACHE_LATITUDE) || !cache.contains(CACHE_LONGITUDE)) return null

        val savedAt = cache.getLong(CACHE_TIME, 0L)
        val source = cache.getString(CACHE_SOURCE, null)
        if (!PrivateLocationCachePolicy.isUsable(source, savedAt, System.currentTimeMillis())) {
            clearPrivateLocationCache()
            return null
        }

        val latitude = Double.fromBits(cache.getLong(CACHE_LATITUDE, Long.MIN_VALUE))
        val longitude = Double.fromBits(cache.getLong(CACHE_LONGITUDE, Long.MIN_VALUE))
        if (!latitude.isFinite() || latitude !in -90.0..90.0) {
            clearPrivateLocationCache()
            return null
        }
        if (!longitude.isFinite() || longitude !in -180.0..180.0) {
            clearPrivateLocationCache()
            return null
        }

        return Location("private_cache").apply {
            this.latitude = latitude
            this.longitude = longitude
            accuracy = cache.getFloat(CACHE_ACCURACY, 50_000f)
            time = savedAt
        }
    }

    private fun persistPlatformLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        if (!PlatformLocationPolicy.isUsable(location.time, System.currentTimeMillis())) return
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return

        cache.edit()
            .putLong(CACHE_LATITUDE, latitude.toBits())
            .putLong(CACHE_LONGITUDE, longitude.toBits())
            .putFloat(CACHE_ACCURACY, location.accuracy.coerceAtLeast(0f))
            .putLong(CACHE_TIME, location.time)
            .putString(CACHE_SOURCE, PrivateLocationCachePolicy.SOURCE_PLATFORM)
            .apply()
    }

    private fun clearPrivateLocationCache() {
        cache.edit()
            .remove(CACHE_LATITUDE)
            .remove(CACHE_LONGITUDE)
            .remove(CACHE_ACCURACY)
            .remove(CACHE_TIME)
            .remove(CACHE_SOURCE)
            .apply()
    }

    @SuppressLint("MissingPermission")
    private fun readBestLastKnownLocation(
        manager: LocationManager,
        providers: List<String>,
        now: Long
    ): Location? {
        val candidates = mutableListOf<Location>()
        for (provider in providers) {
            try {
                manager.getLastKnownLocation(provider)?.let { candidate ->
                    candidates += candidate
                }
            } catch (_: SecurityException) {
                // Permission can be revoked between the initial check and read.
            } catch (_: IllegalArgumentException) {
                // A provider can disappear between enumeration and read.
            }
        }
        return PlatformLocationPolicy.selectBest(
            candidates = candidates,
            now = now,
            timestampOf = Location::getTime,
            accuracyOf = Location::getAccuracy
        )
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates(
        manager: LocationManager,
        providers: List<String>
    ): Flow<Location> = callbackFlow {
        val listener = CompatLocationListener { location ->
            trySend(location)
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
                Timber.d("Registered regular location updates for provider=$provider")
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
                Timber.d("Removed regular location update listener")
            } catch (e: SecurityException) {
                Timber.w(e, "Cannot remove location update listener")
            }
        }
    }
}
