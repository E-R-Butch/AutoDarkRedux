package me.ranko.autodark.data

internal data class LocationRuntimeState(
    val hasPermission: Boolean,
    val managerAvailable: Boolean,
    val locationEnabled: Boolean,
    val hasEnabledProviders: Boolean
)

internal enum class LocationLookupStep {
    PLATFORM_CACHE,
    PRIVATE_CACHE,
    PLATFORM_UPDATE,
    TIME_ZONE_REFERENCE
}

/** Defines a deterministic lookup order without making fallbacks depend on providers. */
internal object LocationResolutionPolicy {
    fun plan(state: LocationRuntimeState): List<LocationLookupStep> {
        val platformUsable = state.hasPermission &&
            state.managerAvailable &&
            state.locationEnabled &&
            state.hasEnabledProviders
        return if (platformUsable) {
            listOf(
                LocationLookupStep.PLATFORM_CACHE,
                LocationLookupStep.PRIVATE_CACHE,
                LocationLookupStep.PLATFORM_UPDATE,
                LocationLookupStep.TIME_ZONE_REFERENCE
            )
        } else {
            listOf(
                LocationLookupStep.PRIVATE_CACHE,
                LocationLookupStep.TIME_ZONE_REFERENCE
            )
        }
    }
}

/** Validates and orders Android framework fixes without depending on Android types. */
internal object PlatformLocationPolicy {
    const val MAX_AGE_MILLIS = 24 * 60 * 60 * 1_000L

    fun isUsable(timestamp: Long, now: Long): Boolean {
        if (timestamp <= 0L) return false
        return now - timestamp in 0..MAX_AGE_MILLIS
    }

    fun <T : Any> selectBest(
        candidates: Iterable<T>,
        now: Long,
        timestampOf: (T) -> Long,
        accuracyOf: (T) -> Float
    ): T? {
        var best: T? = null
        for (candidate in candidates) {
            val candidateTimestamp = timestampOf(candidate)
            if (!isUsable(candidateTimestamp, now)) continue

            val current = best
            if (current == null) {
                best = candidate
                continue
            }

            val currentTimestamp = timestampOf(current)
            if (
                candidateTimestamp > currentTimestamp ||
                candidateTimestamp == currentTimestamp &&
                accuracyOf(candidate).compareTo(accuracyOf(current)) < 0
            ) {
                best = candidate
            }
        }
        return best
    }
}

/** Only precise framework fixes are eligible for the private 24-hour cache. */
internal object PrivateLocationCachePolicy {
    const val SOURCE_PLATFORM = "platform"
    const val MAX_AGE_MILLIS = PlatformLocationPolicy.MAX_AGE_MILLIS

    fun isUsable(source: String?, savedAt: Long, now: Long): Boolean {
        return source == SOURCE_PLATFORM && PlatformLocationPolicy.isUsable(savedAt, now)
    }
}
