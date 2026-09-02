package me.ranko.autodark.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPolicyTest {
    private data class PlatformCandidate(
        val id: String,
        val timestamp: Long,
        val accuracy: Float
    )

    @Test
    fun `platform timestamp accepts fresh and exact max age boundaries`() {
        val now = PlatformLocationPolicy.MAX_AGE_MILLIS + 10_000L

        assertTrue(PlatformLocationPolicy.isUsable(timestamp = now, now = now))
        assertTrue(
            PlatformLocationPolicy.isUsable(
                timestamp = now - PlatformLocationPolicy.MAX_AGE_MILLIS,
                now = now
            )
        )
    }

    @Test
    fun `platform timestamp rejects nonpositive future and stale boundaries`() {
        val now = PlatformLocationPolicy.MAX_AGE_MILLIS + 10_000L

        assertFalse(PlatformLocationPolicy.isUsable(timestamp = 0L, now = now))
        assertFalse(PlatformLocationPolicy.isUsable(timestamp = -1L, now = now))
        assertFalse(PlatformLocationPolicy.isUsable(timestamp = now + 1L, now = now))
        assertFalse(
            PlatformLocationPolicy.isUsable(
                timestamp = now - PlatformLocationPolicy.MAX_AGE_MILLIS - 1L,
                now = now
            )
        )
    }

    @Test
    fun `newest eligible platform candidate wins regardless of accuracy`() {
        val now = PlatformLocationPolicy.MAX_AGE_MILLIS + 10_000L
        val candidates = listOf(
            PlatformCandidate("stale-accurate", now - PlatformLocationPolicy.MAX_AGE_MILLIS - 1L, 1f),
            PlatformCandidate("older-accurate", now - 2_000L, 2f),
            PlatformCandidate("newer-inaccurate", now - 1_000L, 500f),
            PlatformCandidate("future-accurate", now + 1L, 1f)
        )

        val selected = PlatformLocationPolicy.selectBest(
            candidates = candidates,
            now = now,
            timestampOf = PlatformCandidate::timestamp,
            accuracyOf = PlatformCandidate::accuracy
        )

        assertEquals("newer-inaccurate", selected?.id)
    }

    @Test
    fun `accuracy breaks ties between equally new platform candidates`() {
        val now = PlatformLocationPolicy.MAX_AGE_MILLIS + 10_000L
        val timestamp = now - 1_000L
        val candidates = listOf(
            PlatformCandidate("less-accurate", timestamp, 50f),
            PlatformCandidate("more-accurate", timestamp, 5f)
        )

        val selected = PlatformLocationPolicy.selectBest(
            candidates = candidates,
            now = now,
            timestampOf = PlatformCandidate::timestamp,
            accuracyOf = PlatformCandidate::accuracy
        )

        assertEquals("more-accurate", selected?.id)
    }

    @Test
    fun `permission denial still permits private cache and time zone fallback`() {
        val plan = LocationResolutionPolicy.plan(
            LocationRuntimeState(
                hasPermission = false,
                managerAvailable = true,
                locationEnabled = true,
                hasEnabledProviders = true
            )
        )

        assertEquals(
            listOf(LocationLookupStep.PRIVATE_CACHE, LocationLookupStep.TIME_ZONE_REFERENCE),
            plan
        )
    }

    @Test
    fun `disabled providers still permit private cache and time zone fallback`() {
        val plan = LocationResolutionPolicy.plan(
            LocationRuntimeState(
                hasPermission = true,
                managerAvailable = true,
                locationEnabled = false,
                hasEnabledProviders = false
            )
        )

        assertEquals(
            listOf(LocationLookupStep.PRIVATE_CACHE, LocationLookupStep.TIME_ZONE_REFERENCE),
            plan
        )
    }

    @Test
    fun `usable platform tries fresh framework data before cache and fallback`() {
        val plan = LocationResolutionPolicy.plan(
            LocationRuntimeState(
                hasPermission = true,
                managerAvailable = true,
                locationEnabled = true,
                hasEnabledProviders = true
            )
        )

        assertEquals(
            listOf(
                LocationLookupStep.PLATFORM_CACHE,
                LocationLookupStep.PRIVATE_CACHE,
                LocationLookupStep.PLATFORM_UPDATE,
                LocationLookupStep.TIME_ZONE_REFERENCE
            ),
            plan
        )
    }

    @Test
    fun `private cache accepts only recent platform fixes`() {
        val now = 100_000_000L

        assertTrue(
            PrivateLocationCachePolicy.isUsable(
                source = PrivateLocationCachePolicy.SOURCE_PLATFORM,
                savedAt = now - 1_000L,
                now = now
            )
        )
        assertFalse(
            PrivateLocationCachePolicy.isUsable(
                source = null,
                savedAt = now - 1_000L,
                now = now
            )
        )
        assertFalse(
            PrivateLocationCachePolicy.isUsable(
                source = "time_zone_reference",
                savedAt = now - 1_000L,
                now = now
            )
        )
        assertFalse(
            PrivateLocationCachePolicy.isUsable(
                source = PrivateLocationCachePolicy.SOURCE_PLATFORM,
                savedAt = now - PrivateLocationCachePolicy.MAX_AGE_MILLIS - 1L,
                now = now
            )
        )
    }

    @Test
    fun `compat listener explicitly declares every API 29 callback`() {
        val declared = CompatLocationListener::class.java.declaredMethods.map { it.name }.toSet()

        assertTrue("onLocationChanged" in declared)
        assertTrue("onStatusChanged" in declared)
        assertTrue("onProviderEnabled" in declared)
        assertTrue("onProviderDisabled" in declared)
    }
}
