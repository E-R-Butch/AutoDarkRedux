package me.ranko.autodark.Utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DarkTimeUtilTest {
    @Test
    fun `manual city time is converted to device time for the same instant`() {
        val converted = DarkTimeUtil.convertLocalTimeBetweenZones(
            date = LocalDate.of(2026, 9, 2),
            time = LocalTime.of(6, 30),
            sourceZone = ZoneId.of("America/New_York"),
            destinationZone = ZoneId.of("Asia/Shanghai")
        )

        assertEquals(LocalTime.of(18, 30), converted)
    }

    @Test
    fun `manual city evening may become next device calendar day`() {
        val converted = DarkTimeUtil.convertLocalTimeBetweenZones(
            date = LocalDate.of(2026, 9, 2),
            time = LocalTime.of(18, 45),
            sourceZone = ZoneId.of("America/New_York"),
            destinationZone = ZoneId.of("Asia/Shanghai")
        )

        assertEquals(LocalTime.of(6, 45), converted)
    }

    @Test
    fun `polar day without sunrise or sunset returns null`() {
        val result = DarkTimeUtil.getDarkTimeStringForDate(
            latitude = -77.8419,
            longitude = 166.6863,
            sourceDate = LocalDate.of(2026, 1, 2),
            sourceZone = ZoneId.of("Antarctica/McMurdo"),
            destinationZone = ZoneId.of("Asia/Shanghai")
        )

        assertNull(result)
    }
}
