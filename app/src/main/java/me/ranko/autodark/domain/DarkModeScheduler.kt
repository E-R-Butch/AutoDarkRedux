package me.ranko.autodark.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import me.ranko.autodark.Utils.DarkTimeUtil
import me.ranko.autodark.receivers.DarkModeAlarmReceiver
import timber.log.Timber
import java.time.LocalTime

/**
 * Owns the alarm lifecycle for scheduled dark-mode changes.
 *
 * The scheduler deliberately knows nothing about UI, permissions, or wallpaper
 * persistence. The caller supplies the current-mode reader and the mode setter.
 */
class DarkModeScheduler(
    context: Context,
    private val currentMode: () -> Boolean?,
    private val applyMode: (Boolean) -> Boolean
) {
    companion object {
        const val START_TYPE = "dark_mode_time_start"
        const val END_TYPE = "dark_mode_time_end"

        private const val PARAM_ALARM_TYPE = "ALARM_TYPE"
        private const val PARAM_ALARM_TIME = "ALARM_TIME"
        private const val REQUEST_ALARM_START = 0x00B0
        private const val REQUEST_ALARM_END = REQUEST_ALARM_START.shl(1)
    }

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        ?: error("AlarmManager is unavailable")

    /**
     * Adjusts the current mode if necessary and schedules both daily alarms.
     *
     * @return true when the current mode needed to be adjusted.
     */
    fun schedule(startTime: LocalTime, endTime: LocalTime): Boolean {
        val current = currentMode() == true
        val inRange = DarkTimeUtil.isInTime(startTime, endTime, LocalTime.now())
        val adjusted = inRange.xor(current)
        if (adjusted) {
            applyMode(inRange)
        }

        scheduleNext(startTime, START_TYPE)
        scheduleNext(endTime, END_TYPE)
        Timber.v("User currently %s mode range", if (inRange) "in" else "not in")
        return adjusted
    }

    /**
     * Turns dark mode off and cancels both daily alarms.
     *
     * @return true when the current time was inside the configured range.
     */
    fun cancel(startTime: LocalTime, endTime: LocalTime): Boolean {
        applyMode(false)

        val startMillis = DarkTimeUtil.getTodayOrNextDay(startTime)
        val endMillis = DarkTimeUtil.getTodayOrNextDay(endTime)
        alarmManager.cancel(pendingAlarm(startMillis, START_TYPE))
        alarmManager.cancel(pendingAlarm(endMillis, END_TYPE))

        Timber.v("Cancel start job: %s: %s", DarkTimeUtil.getPersistFormattedString(startTime), startMillis)
        Timber.v("Cancel end job: %s: %s", DarkTimeUtil.getPersistFormattedString(endTime), endMillis)
        return DarkTimeUtil.isInTime(startTime, endTime, LocalTime.now())
    }

    /**
     * Handles one scheduled alarm, applies the requested mode, and renews it
     * for the following day. The returned value is the requested mode and is
     * used by the caller to apply its side effects, such as wallpaper changes.
     */
    fun onAlarm(intent: Intent): Boolean {
        Timber.v("Dark alarm broadcast received")
        val type = requireNotNull(intent.getStringExtra(PARAM_ALARM_TYPE)) {
            "Dark-mode alarm type is missing"
        }
        require(type == START_TYPE || type == END_TYPE) {
            "Unknown dark-mode alarm type: $type"
        }

        val requestedTime = intent.getLongExtra(PARAM_ALARM_TIME, -1L)
        require(requestedTime >= 0L) { "Dark-mode alarm time is missing" }

        val nextAlarm = DarkTimeUtil.toNextDayAlarmMillis(requestedTime)
        val isDark = type == START_TYPE
        applyMode(isDark)
        alarmManager.set(
            AlarmManager.RTC,
            nextAlarm,
            pendingAlarm(nextAlarm, type)
        )
        Timber.v("Dark job %s finished, pending next alarm: %s", type, nextAlarm)
        return isDark
    }

    private fun scheduleNext(time: LocalTime, type: String) {
        val alarmTime = DarkTimeUtil.getTodayOrNextDay(time)
        alarmManager.set(AlarmManager.RTC, alarmTime, pendingAlarm(alarmTime, type))
        Timber.v("Set %s alarm: %s : %s", type, DarkTimeUtil.getPersistFormattedString(time), alarmTime)
    }

    private fun pendingAlarm(time: Long, type: String): PendingIntent {
        val intent = Intent(appContext, DarkModeAlarmReceiver::class.java).apply {
            putExtra(PARAM_ALARM_TYPE, type)
            putExtra(PARAM_ALARM_TIME, time)
        }
        val requestCode = if (type == START_TYPE) REQUEST_ALARM_START else REQUEST_ALARM_END
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
}
