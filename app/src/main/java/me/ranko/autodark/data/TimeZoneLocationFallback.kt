package me.ranko.autodark.data

import android.content.Context
import android.location.Location
import me.ranko.autodark.R
import timber.log.Timber

internal data class TimeZoneReference(
    val countryCode: String,
    val zoneId: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Resolves a time zone to the public-domain IANA zone.tab reference coordinate.
 * This is deliberately coarse and is used only when Android's location
 * providers and the app's recent private cache have no result.
 */
internal class TimeZoneLocationFallback(context: Context) {
    private companion object {
        private const val REFERENCE_ACCURACY_METERS = 500_000f

        private val aliases = mapOf(
            "Asia/Chongqing" to "Asia/Shanghai",
            "Asia/Chungking" to "Asia/Shanghai",
            "Asia/Harbin" to "Asia/Shanghai",
            "PRC" to "Asia/Shanghai",
            "ROC" to "Asia/Taipei",
            "Japan" to "Asia/Tokyo"
        )
    }

    private val appContext = context.applicationContext
    private val referenceList: List<TimeZoneReference> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadReferences()
    }

    fun references(): List<TimeZoneReference> = referenceList

    fun locate(zoneId: String): Location? {
        val canonicalZoneId = aliases[zoneId] ?: zoneId
        val reference = referenceList.firstOrNull { it.zoneId == canonicalZoneId } ?: return null
        return Location("time_zone").apply {
            latitude = reference.latitude
            longitude = reference.longitude
            accuracy = REFERENCE_ACCURACY_METERS
            time = System.currentTimeMillis()
        }
    }

    private fun loadReferences(): List<TimeZoneReference> = try {
        appContext.resources.openRawResource(R.raw.zone_tab)
            .bufferedReader(Charsets.US_ASCII)
            .useLines { lines -> lines.mapNotNull(::parseRow).toList() }
    } catch (e: Exception) {
        Timber.w(e, "Unable to read bundled IANA time-zone coordinates")
        emptyList()
    }

    private fun parseRow(line: String): TimeZoneReference? {
        if (line.isBlank() || line.startsWith('#')) return null
        val columns = line.split('\t')
        if (columns.size < 3) return null
        val coordinate = parseIso6709(columns[1]) ?: return null
        return TimeZoneReference(
            countryCode = columns[0],
            zoneId = columns[2],
            latitude = coordinate.first,
            longitude = coordinate.second
        )
    }

    private fun parseIso6709(value: String): Pair<Double, Double>? {
        if (value.length < 11 || (value[0] != '+' && value[0] != '-')) return null
        val longitudeSignIndex = value.indexOfFirstFrom(1) { it == '+' || it == '-' }
        if (longitudeSignIndex < 0) return null

        val latitude = parseComponent(value.substring(0, longitudeSignIndex), 2) ?: return null
        val longitude = parseComponent(value.substring(longitudeSignIndex), 3) ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return latitude to longitude
    }

    private fun parseComponent(value: String, degreeDigits: Int): Double? {
        if (value.isEmpty() || (value[0] != '+' && value[0] != '-')) return null
        val digits = value.substring(1)
        if (digits.length != degreeDigits + 2 && digits.length != degreeDigits + 4) return null
        if (!digits.all(Char::isDigit)) return null

        val degrees = digits.substring(0, degreeDigits).toIntOrNull() ?: return null
        val minutes = digits.substring(degreeDigits, degreeDigits + 2).toIntOrNull() ?: return null
        val seconds = if (digits.length == degreeDigits + 4) {
            digits.substring(degreeDigits + 2).toIntOrNull() ?: return null
        } else {
            0
        }
        if (minutes >= 60 || seconds >= 60) return null

        val magnitude = degrees + minutes / 60.0 + seconds / 3_600.0
        return if (value[0] == '-') -magnitude else magnitude
    }

    private inline fun String.indexOfFirstFrom(
        startIndex: Int,
        predicate: (Char) -> Boolean
    ): Int {
        for (index in startIndex until length) {
            if (predicate(this[index])) return index
        }
        return -1
    }
}
