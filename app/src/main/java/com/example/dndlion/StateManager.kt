package com.example.dndlion

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class StateManager(private val context: Context) {
    private val TAG = Constants.LogTags.STATE_MANAGER
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PreferenceKeys.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Saving individual state components
    fun saveStartTime(time: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_START_TIME, time).apply()
        Log.d(TAG, "Saved start time: $time")
    }

    fun saveEndTime(time: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_END_TIME, time).apply()
        Log.d(TAG, "Saved end time: $time")
    }

    fun saveSelectedDays(days: Set<String>) {
        prefs.edit().putStringSet(Constants.PreferenceKeys.KEY_SELECTED_DAYS, days).apply()
        Log.d(TAG, "Saved selected days: $days")
    }

    fun saveSmsMessage(message: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_SMS_MESSAGE, message).apply()
        Log.d(TAG, "Saved SMS message: $message")
    }

    fun saveIsActive(isActive: Boolean) {
        prefs.edit().putBoolean(Constants.PreferenceKeys.KEY_IS_ACTIVE, isActive).apply()
        Log.d(TAG, "Saved active state: $isActive")
    }

    // Saving all state at once
    fun saveState(
        startTime: String,
        endTime: String,
        selectedDays: Set<String>,
        smsMessage: String,
        isActive: Boolean
    ) {
        Log.d(TAG, "Saving full state")
        prefs.edit().apply {
            putString(Constants.PreferenceKeys.KEY_START_TIME, startTime)
            putString(Constants.PreferenceKeys.KEY_END_TIME, endTime)
            putStringSet(Constants.PreferenceKeys.KEY_SELECTED_DAYS, selectedDays)
            putString(Constants.PreferenceKeys.KEY_SMS_MESSAGE, smsMessage)
            putBoolean(Constants.PreferenceKeys.KEY_IS_ACTIVE, isActive)
            apply()
        }
        Log.d(TAG, "State saved successfully")
    }

    // Restoring state to UI
    fun restoreState(views: ViewHolder) {
        Log.d(TAG, "Restoring state")
        try {
            val startTime = getStoredStartTime()
            val endTime = getStoredEndTime()
            val smsMessage = getStoredSmsMessage()
            val storedDays = getStoredDays()

            views.startTimeInput.setText(startTime)
            views.endTimeInput.setText(endTime)
            views.smsMessageInput.setText(smsMessage)

            views.dayChips.forEach { (day, chip) ->
                chip.isChecked = storedDays.contains(day)
            }

            Log.d(TAG, "State restored successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring state", e)
        }
    }

    // Check if the state has valid data
    fun hasValidState(): Boolean {
        return getStoredStartTime() != null && getStoredEndTime() != null
    }

    // Getter methods with additional logging
    fun getStoredStartTime(): String? {
        val startTime = prefs.getString(Constants.PreferenceKeys.KEY_START_TIME, null)
        Log.d(TAG, "Retrieved start time: $startTime")
        return startTime
    }

    fun getStoredEndTime(): String? {
        val endTime = prefs.getString(Constants.PreferenceKeys.KEY_END_TIME, null)
        Log.d(TAG, "Retrieved end time: $endTime")
        return endTime
    }

    fun getStoredDays(): Set<String> {
        val days = prefs.getStringSet(Constants.PreferenceKeys.KEY_SELECTED_DAYS, emptySet()) ?: emptySet()
        Log.d(TAG, "Retrieved selected days: $days")
        return days
    }

    fun getStoredSmsMessage(): String {
        val message = prefs.getString(Constants.PreferenceKeys.KEY_SMS_MESSAGE, Constants.Messages.DEFAULT_SMS_MESSAGE) ?: Constants.Messages.DEFAULT_SMS_MESSAGE
        Log.d(TAG, "Retrieved SMS message: $message")
        return message
    }

    // Single method to check if schedule is active
    val isStateActive: Boolean
        get() = prefs.getBoolean(Constants.PreferenceKeys.KEY_IS_ACTIVE, false)

    // Clear all stored state
    fun clearState() {
        Log.d(TAG, "Clearing state")
        prefs.edit().clear().apply()
        Log.d(TAG, "State cleared successfully")
    }
}
