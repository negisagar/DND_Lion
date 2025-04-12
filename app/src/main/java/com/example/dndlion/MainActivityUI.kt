package com.example.dndlion

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class MainActivityUI(
    private val activity: AppCompatActivity,
    private val views: ViewHolder,
    private val stateManager: StateManager,
    private val alarmScheduler: AlarmScheduler,
    private val permissionHandler: PermissionHandler
) {
    private val TAG = Constants.LogTags.MAIN_ACTIVITY
    private var isScheduleActive = false

    fun setupUI() {
        setupTimeInputs()
        setupButtons()
        setupDayChips()
        restoreState()
    }

    fun updateUIForActiveState() {
        try {
            Log.d(TAG, "Updating UI for active state")
            views.stopDndButton.isEnabled = true
            views.startTimeInput.isEnabled = false
            views.endTimeInput.isEnabled = false
            views.smsMessageInput.isEnabled = false

            // Disable day selection chips
            views.dayChips.values.forEach { chip ->
                chip.isEnabled = false
            }

            Log.d(TAG, "UI updated for active state successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI for active state", e)
        }
    }

    fun resetUIState() {
        Log.d(TAG, "Resetting UI state")
        isScheduleActive = false
        views.apply {
            enableInputs()
            scheduleButton.isEnabled = validateInputs()
            stopDndButton.isEnabled = false
        }
        updateButtonStates()
        Log.d(TAG, "UI state reset completed")
    }

    private fun setupTimeInputs() {
        views.startTimeInput.setOnClickListener { showTimePicker(true) }
        views.endTimeInput.setOnClickListener { showTimePicker(false) }

        // Make inputs read-only
        views.startTimeInput.keyListener = null
        views.endTimeInput.keyListener = null
    }

    private fun setupButtons() {
        views.scheduleButton.setOnClickListener { handleScheduleButton() }
        views.stopDndButton.setOnClickListener { handleStopButton() }
        updateButtonStates()
    }

    private fun setupDayChips() {
        views.dayChips.values.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                validateAndUpdateButtons()
            }
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val currentCalendar = Calendar.getInstance()

        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentCalendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(currentCalendar.get(Calendar.MINUTE))
            .setTitleText(if (isStartTime) "Select Start Time" else "Select End Time")
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    val timeString = TimeUtils.formatTime(hour, minute)
                    if (isStartTime) {
                        views.startTimeInput.setText(timeString)
                    } else {
                        views.endTimeInput.setText(timeString)
                    }
                    validateAndUpdateButtons()
                }
            }
            .show(activity.supportFragmentManager, "TimePicker")
    }

    private fun handleScheduleButton() {
        if (!validateInputs()) {
            Toast.makeText(activity, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!permissionHandler.checkPermissions()) {
            permissionHandler.showPermissionExplanationDialog(
                "Permissions Required",
                "This app needs permissions to manage DND mode and handle calls/messages.",
                { permissionHandler.checkAndRequestPermissions() }
            )
            return
        }

        if (!permissionHandler.isNotificationPolicyAccessGranted()) {
            permissionHandler.requestNotificationPolicyAccess()
            return
        }

        val startTime = TimeUtils.parseTime(views.startTimeInput.text.toString())
        val endTime = TimeUtils.parseTime(views.endTimeInput.text.toString())

        if (startTime == null || endTime == null) {
            Toast.makeText(activity, "Invalid time format", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = views.getSelectedDays()
        if (alarmScheduler.scheduleDndMode(startTime, endTime, selectedDays)) {
            isScheduleActive = true
            updateButtonStates()
            saveState()
//            enableDndMode()
            views.disableInputs()
            Log.d(TAG, "DND scheduled with SMS: ${views.smsMessageInput.text}")
        }
    }

    private fun handleStopButton() {
        val selectedDays = views.getSelectedDays()
        alarmScheduler.stopDndMode(selectedDays)
        isScheduleActive = false
        updateButtonStates()
        disableDndMode()
        views.enableInputs()
        stateManager.clearState()
        Log.d(TAG, "DND mode stopped and state cleared")
    }

    private fun validateInputs(): Boolean {
        return views.validateInputs()
    }

    private fun validateAndUpdateButtons() {
        views.scheduleButton.isEnabled = validateInputs() && !isScheduleActive
    }

    private fun updateButtonStates() {
        views.updateButtonStates(isScheduleActive)
    }

    private fun saveState() {
        stateManager.saveState(
            startTime = views.startTimeInput.text.toString(),
            endTime = views.endTimeInput.text.toString(),
            selectedDays = views.getSelectedDays(),
            smsMessage = views.smsMessageInput.text.toString(),
            isActive = isScheduleActive
        )
    }

    private fun restoreState() {
        if (stateManager.hasValidState()) {
            stateManager.restoreState(views)
            isScheduleActive = stateManager.isStateActive
            if (isScheduleActive) {
//                enableDndMode()
                views.disableInputs()
            }
            updateButtonStates()
            Log.d(TAG, "State restored successfully")
        }
    }

    private fun enableDndMode() {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
        Log.d(TAG, "DND mode enabled successfully")
    }

    private fun disableDndMode() {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
        Log.d(TAG, "DND mode disabled successfully")
    }
}
