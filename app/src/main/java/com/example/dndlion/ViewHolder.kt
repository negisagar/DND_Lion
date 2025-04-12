package com.example.dndlion

import android.widget.Button
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText

class ViewHolder(activity: MainActivity) {
    val startTimeInput: TextInputEditText = activity.findViewById(R.id.start_time_input)
    val endTimeInput: TextInputEditText = activity.findViewById(R.id.end_time_input)
    val smsMessageInput: TextInputEditText = activity.findViewById(R.id.sms_message_input)
    val scheduleButton: Button = activity.findViewById(R.id.schedule_button)
    val stopDndButton: Button = activity.findViewById(R.id.stop_dnd_button)
    val dayChips: Map<String, Chip> = mapOf(
        "Mon" to activity.findViewById(R.id.chip_mon),
        "Tue" to activity.findViewById(R.id.chip_tue),
        "Wed" to activity.findViewById(R.id.chip_wed),
        "Thu" to activity.findViewById(R.id.chip_thu),
        "Fri" to activity.findViewById(R.id.chip_fri),
        "Sat" to activity.findViewById(R.id.chip_sat),
        "Sun" to activity.findViewById(R.id.chip_sun)
    )

    fun updateButtonStates(isScheduleActive: Boolean) {
        scheduleButton.isEnabled = !isScheduleActive
        stopDndButton.isEnabled = isScheduleActive
    }

    fun clearInputs() {
        startTimeInput.text?.clear()
        endTimeInput.text?.clear()
        smsMessageInput.text?.clear()
        dayChips.values.forEach { chip -> chip.isChecked = false }
    }

    fun getSelectedDays(): Set<String> {
        return dayChips.entries
            .filter { it.value.isChecked }
            .map { it.key }
            .toSet()
    }

    fun setSelectedDays(days: Set<String>) {
        dayChips.forEach { (day, chip) ->
            chip.isChecked = day in days
        }
    }

    fun disableInputs() {
        startTimeInput.isEnabled = false
        endTimeInput.isEnabled = false
        smsMessageInput.isEnabled = false
        dayChips.values.forEach { it.isEnabled = false }
    }

    fun enableInputs() {
        startTimeInput.isEnabled = true
        endTimeInput.isEnabled = true
        smsMessageInput.isEnabled = true
        dayChips.values.forEach { it.isEnabled = true }
    }

    fun validateInputs(): Boolean {
        val startTime = startTimeInput.text.toString()
        val endTime = endTimeInput.text.toString()
        val selectedDays = getSelectedDays()

        return startTime.isNotEmpty() &&
                endTime.isNotEmpty() &&
                selectedDays.isNotEmpty()
    }
}
