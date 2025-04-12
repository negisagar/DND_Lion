package com.example.dndlion

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val TAG = Constants.LogTags.ALARM_SCHEDULER
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val SCHEDULE_AHEAD_TIME = 10000L // 10 seconds ahead to compensate for system delays

    fun scheduleDndMode(startTime: Calendar, endTime: Calendar, days: Set<String>): Boolean {
        try {
            // Cancel any existing alarms first
            cancelExistingAlarms(days)

            for (day in days) {
                // 1) Build the correct calendars for each day’s start/end
                val dayOfWeek = TimeUtils.getDayOfWeek(day) // e.g. "Mon" -> Calendar.MONDAY

                val adjustedStartTime = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // If it's still in the past, jump forward 1 week
                    if (before(Calendar.getInstance())) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                // ------ End Time ------
                // Do the same for the end time
                val adjustedEndTime = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // 2) If user’s endTime is *before* user’s startTime on the same day, assume overnight → add 1 day
                if (adjustedEndTime.before(adjustedStartTime)) {
                    adjustedEndTime.add(Calendar.DAY_OF_YEAR, 1)
                }

                // 3) Subtract the schedule-ahead offset from both
                adjustedStartTime.timeInMillis -= SCHEDULE_AHEAD_TIME
                adjustedEndTime.timeInMillis -= SCHEDULE_AHEAD_TIME

                // 4) Create PendingIntents
                val startIntent = createPendingIntent(Constants.Actions.ACTION_START_DND, day.hashCode())
                val endIntent = createPendingIntent(Constants.Actions.ACTION_END_DND, day.hashCode() + 1)

                // 5) Check for exact alarm permission on Android S+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                        return false
                    }
                }

                // 6) Schedule the alarms
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(adjustedStartTime.timeInMillis, null),
                    startIntent
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(adjustedEndTime.timeInMillis, null),
                    endIntent
                )

                Log.d(TAG, "Scheduled alarms for $day: start=${adjustedStartTime.time}, end=${adjustedEndTime.time}")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling DND mode", e)
            return false
        }
    }

    /**
     * Helper method to compute the next valid occurrence of start/end time
     * for a given day of week (Mon, Tue, etc.).
     */
    private fun getNextOccurrenceForDay(
        dayOfWeek: Int,
        userSelectedTime: Calendar
    ): Calendar {
        val now = Calendar.getInstance()

        // Create a fresh Calendar for the target day.
        val target = Calendar.getInstance().apply {
            // 1) Set the day of week to the user’s chosen day (Mon, Tue, etc.).
            set(Calendar.DAY_OF_WEEK, dayOfWeek)

            // 2) Set the hour/minute from userSelectedTime (parsed from UI).
            set(Calendar.HOUR_OF_DAY, userSelectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, userSelectedTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time is before "now," move it ahead by 1 week.
        if (target.before(now)) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return target
    }


    fun cancelExistingAlarms(days: Set<String>) {
        for (day in days) {
            val startIntent = createPendingIntent(Constants.Actions.ACTION_START_DND, day.hashCode())
            val endIntent = createPendingIntent(Constants.Actions.ACTION_END_DND, day.hashCode() + 1)

            alarmManager.cancel(startIntent)
            alarmManager.cancel(endIntent)
            Log.d(TAG, "Cancelled alarms for $day")
        }
    }

    private fun createPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, DndModeReceiver::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).also {
            Log.d(TAG, "Created PendingIntent for action: $action with requestCode: $requestCode")
        }
    }

    fun rescheduleForNextWeek(originalIntent: Intent) {
        Log.d(TAG, "Rescheduling for next week")
        // Implementation for rescheduling
    }

    fun stopDndMode(days: Set<String>) {
        try {
            cancelExistingAlarms(days)
            Log.d(TAG, "Successfully cancelled all alarms")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping DND mode", e)
        }
    }
}
