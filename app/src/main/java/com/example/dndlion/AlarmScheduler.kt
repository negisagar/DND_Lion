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

            val now = Calendar.getInstance()
            val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val todayDayName = TimeUtils.getDayName(todayDayOfWeek)
            
            // Check if we're currently in an active DND window
            if (days.contains(todayDayName)) {
                if (isCurrentlyInDndWindow(startTime, endTime)) {
                    Log.d(TAG, "Currently in DND window - enabling DND immediately")
                    enableDndModeNow()
                    
                    // Schedule the end alarm for today
                    val adjustedEndTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        
                        // If end time is before start time (overnight), add a day
                        if (endTime.get(Calendar.HOUR_OF_DAY) < startTime.get(Calendar.HOUR_OF_DAY) ||
                            (endTime.get(Calendar.HOUR_OF_DAY) == startTime.get(Calendar.HOUR_OF_DAY) &&
                             endTime.get(Calendar.MINUTE) < startTime.get(Calendar.MINUTE))) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    
                    val endIntent = createPendingIntent(Constants.Actions.ACTION_END_DND, todayDayName.hashCode() + 1)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    } else {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(adjustedEndTime.timeInMillis, null),
                            endIntent
                        )
                        Log.d(TAG, "Scheduled end alarm for today: ${adjustedEndTime.time}")
                    }
                }
            }

            // Schedule future alarms for all selected days
            for (day in days) {
                val dayOfWeek = TimeUtils.getDayOfWeek(day)

                val adjustedStartTime = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // If it's in the past (including today if we already passed it), jump forward 1 week
                    if (before(Calendar.getInstance())) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val adjustedEndTime = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If end time is before start time (overnight), add 1 day
                if (adjustedEndTime.before(adjustedStartTime) || 
                    (endTime.get(Calendar.HOUR_OF_DAY) < startTime.get(Calendar.HOUR_OF_DAY))) {
                    adjustedEndTime.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                // If end time is in the past, move to next week
                if (adjustedEndTime.before(Calendar.getInstance())) {
                    adjustedEndTime.add(Calendar.WEEK_OF_YEAR, 1)
                    adjustedStartTime.add(Calendar.WEEK_OF_YEAR, 1)
                }

                // Subtract the schedule-ahead offset
                adjustedStartTime.timeInMillis -= SCHEDULE_AHEAD_TIME

                val startIntent = createPendingIntent(Constants.Actions.ACTION_START_DND, day.hashCode())
                val endIntent = createPendingIntent(Constants.Actions.ACTION_END_DND, day.hashCode() + 1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                        return false
                    }
                }

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
     * Check if current time is within the DND window.
     */
    private fun isCurrentlyInDndWindow(startTime: Calendar, endTime: Calendar): Boolean {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute
        
        val startHour = startTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = startTime.get(Calendar.MINUTE)
        val startTotalMinutes = startHour * 60 + startMinute
        
        val endHour = endTime.get(Calendar.HOUR_OF_DAY)
        val endMinute = endTime.get(Calendar.MINUTE)
        val endTotalMinutes = endHour * 60 + endMinute
        
        return if (startTotalMinutes <= endTotalMinutes) {
            // Same day window (e.g., 10:00 AM to 6:00 PM)
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            // Overnight window (e.g., 10:00 PM to 7:00 AM)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }
    
    /**
     * Immediately enables DND mode on the device.
     */
    fun enableDndModeNow() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
                    Log.d(TAG, "DND mode enabled immediately")
                } else {
                    Log.w(TAG, "Cannot enable DND - notification policy access not granted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling DND mode", e)
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
            disableDndModeNow()
            Log.d(TAG, "Successfully cancelled all alarms and disabled DND")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping DND mode", e)
        }
    }

    /**
     * Immediately disables DND mode on the device.
     */
    fun disableDndModeNow() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                    Log.d(TAG, "DND mode disabled immediately")
                } else {
                    Log.w(TAG, "Cannot disable DND - notification policy access not granted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling DND mode", e)
        }
    }
}
