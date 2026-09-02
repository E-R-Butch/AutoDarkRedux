package me.ranko.autodark.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ranko.autodark.R
import timber.log.Timber
import java.util.Locale

/** A city coordinate bundled with the app; no runtime geocoding is required. */
data class CityReference(
    val id: String,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val population: Long = 0L,
    val aliases: String = ""
) {
    val displayTimeZone: String
        get() = timeZoneId.replace('_', ' ')

    fun toLocation(provider: String) = android.location.Location(provider).apply {
        latitude = this@CityReference.latitude
        longitude = this@CityReference.longitude
        accuracy = 25_000f
        time = System.currentTimeMillis()
    }
}

/**
 * Lazy, offline city search. The main catalog is a reduced GeoNames snapshot;
 * IANA time-zone reference cities are merged so small time-zone capitals remain
 * selectable even when they fall below the population threshold.
 */
internal class CityCatalog(context: Context) {
    private companion object {
        private const val DEFAULT_RESULT_LIMIT = 40
        private const val MAX_RESULT_LIMIT = 80
    }

    private val appContext = context.applicationContext
    private val timeZoneReferences = TimeZoneLocationFallback(appContext)
    private val cities: List<CityReference> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadCities()
    }

    suspend fun search(query: String, limit: Int = DEFAULT_RESULT_LIMIT): List<CityReference> =
        withContext(Dispatchers.Default) {
            val safeLimit = limit.coerceIn(1, MAX_RESULT_LIMIT)
            val needle = query.trim().lowercase(Locale.ROOT)
            if (needle.isEmpty()) {
                return@withContext cities.asSequence()
                    .sortedByDescending(CityReference::population)
                    .take(safeLimit)
                    .toList()
            }

            cities.asSequence()
                .mapNotNull { city ->
                    val score = matchScore(city, needle) ?: return@mapNotNull null
                    Triple(score, -city.population, city)
                }
                .sortedWith(
                    compareBy<Triple<Int, Long, CityReference>> { it.first }
                        .thenBy { it.second }
                        .thenBy { it.third.name.lowercase(Locale.ROOT) }
                )
                .take(safeLimit)
                .map(Triple<Int, Long, CityReference>::third)
                .toList()
        }

    private fun matchScore(city: CityReference, needle: String): Int? {
        val name = city.name.lowercase(Locale.ROOT)
        val zone = city.timeZoneId.lowercase(Locale.ROOT).replace('_', ' ')
        val aliases = city.aliases.lowercase(Locale.ROOT)
        val country = city.countryCode.lowercase(Locale.ROOT)
        return when {
            name == needle -> 0
            name.startsWith(needle) -> 1
            aliases.split('|').any { it == needle } -> 2
            aliases.split('|').any { it.startsWith(needle) } -> 3
            zone.contains(needle) -> 4
            name.contains(needle) -> 5
            aliases.contains(needle) -> 6
            country == needle -> 7
            else -> null
        }
    }

    private fun loadCities(): List<CityReference> {
        val geonames = try {
            appContext.resources.openRawResource(R.raw.cities_compact)
                .bufferedReader(Charsets.UTF_8)
                .useLines { lines -> lines.mapNotNull(::parseGeoNamesRow).toList() }
        } catch (e: Exception) {
            Timber.e(e, "Unable to load bundled city catalog")
            emptyList()
        }

        val seen = geonames.asSequence()
            .map { "${it.countryCode}:${it.name.lowercase(Locale.ROOT)}" }
            .toMutableSet()
        val iana = timeZoneReferences.references().mapNotNull { reference ->
            val name = reference.zoneId.substringAfterLast('/').replace('_', ' ')
            val key = "${reference.countryCode}:${name.lowercase(Locale.ROOT)}"
            if (!seen.add(key)) return@mapNotNull null
            CityReference(
                id = "iana:${reference.zoneId}",
                name = name,
                countryCode = reference.countryCode,
                latitude = reference.latitude,
                longitude = reference.longitude,
                timeZoneId = reference.zoneId,
                aliases = reference.zoneId.replace('_', ' ')
            )
        }
        return geonames + iana
    }

    private fun parseGeoNamesRow(line: String): CityReference? {
        val columns = line.split('\t', limit = 9)
        if (columns.size < 9) return null
        val latitude = columns[4].toDoubleOrNull() ?: return null
        val longitude = columns[5].toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return CityReference(
            id = "geonames:${columns[0]}",
            name = columns[1],
            countryCode = columns[3],
            latitude = latitude,
            longitude = longitude,
            timeZoneId = columns[6],
            population = columns[7].toLongOrNull() ?: 0L,
            aliases = columns[8]
        )
    }
}
