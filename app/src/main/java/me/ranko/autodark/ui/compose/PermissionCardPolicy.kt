package me.ranko.autodark.ui.compose

import me.ranko.autodark.core.ShizukuStatus

/** Shizuku is onboarding; the persistent system permission is the final state. */
internal object PermissionCardPolicy {
    fun shouldShow(
        hasSecurePermission: Boolean,
        shizukuStatus: ShizukuStatus
    ): Boolean {
        if (hasSecurePermission) return false
        return when (shizukuStatus) {
            ShizukuStatus.AVAILABLE,
            ShizukuStatus.DEAD,
            ShizukuStatus.UNAUTHORIZED,
            ShizukuStatus.NOT_INSTALL -> true
        }
    }
}
