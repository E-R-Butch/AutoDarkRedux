package me.ranko.autodark.ui.compose

import me.ranko.autodark.core.ShizukuStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCardPolicyTest {
    @Test
    fun `card remains visible until system control permission is granted`() {
        assertTrue(
            PermissionCardPolicy.shouldShow(
                hasSecurePermission = false,
                shizukuStatus = ShizukuStatus.AVAILABLE
            )
        )
    }

    @Test
    fun `card stays hidden after final permission even if Shizuku stops`() {
        assertFalse(
            PermissionCardPolicy.shouldShow(
                hasSecurePermission = true,
                shizukuStatus = ShizukuStatus.DEAD
            )
        )
    }
}
