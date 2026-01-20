package com.example.dndlion

import java.util.Calendar
import java.util.Locale

object TimeUtils {
    fun parseTime(timeString: String?): Calendar? {
        if (timeString.isNullOrEmpty()) return null

        try {
            val parts = timeString.split(" ", ":")
            if (parts.size != 3) return null

            var hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            val period = parts[2]

            if (period.equals("PM", true) && hour != 12) {
                hour += 12
            } else if (period.equals("AM", true) && hour == 12) {
                hour = 0
            }

            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
        val period = if (hour >= 12) "PM" else "AM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, period)
    }

    fun getDayOfWeek(day: String): Int {
        return when (day) {
            "Mon" -> Calendar.MONDAY
            "Tue" -> Calendar.TUESDAY
            "Wed" -> Calendar.WEDNESDAY
            "Thu" -> Calendar.THURSDAY
            "Fri" -> Calendar.FRIDAY
            "Sat" -> Calendar.SATURDAY
            "Sun" -> Calendar.SUNDAY
            else -> throw IllegalArgumentException("Invalid day: $day")
        }
    }
    
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> throw IllegalArgumentException("Invalid day of week: $dayOfWeek")
        }
    }
}
