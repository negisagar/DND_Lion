package com.example.dndlion

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var startTimeInput: TextInputEditText
    private lateinit var endTimeInput: TextInputEditText
    private lateinit var smsMessageInput: TextInputEditText
    private lateinit var scheduleButton: Button
    private lateinit var stopDndButton: Button

    private lateinit var buttonMon: MaterialButton
    private lateinit var buttonTue: MaterialButton
    private lateinit var buttonWed: MaterialButton
    private lateinit var buttonThu: MaterialButton
    private lateinit var buttonFri: MaterialButton
    private lateinit var buttonSat: MaterialButton
    private lateinit var buttonSun: MaterialButton

    private val selectedDays = mutableSetOf<String>()


    private val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )

    companion object {
        const val PREFS_NAME = "DND_PREFS"
        const val KEY_START_TIME = "START_TIME"
        const val KEY_END_TIME = "END_TIME"
        const val KEY_SMS_MESSAGE = "SMS_MESSAGE"
        const val KEY_DND_ACTIVE = "DND_ACTIVE"
        const val KEY_SELECTED_DAYS = "SELECTED_DAYS"

    }

    private fun saveDndPreferences() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_START_TIME, startTimeInput.text.toString())
        editor.putString(KEY_END_TIME, endTimeInput.text.toString())
        editor.putString(KEY_SMS_MESSAGE, smsMessageInput.text.toString())
        editor.putStringSet(KEY_SELECTED_DAYS, selectedDays)
        editor.putBoolean(KEY_DND_ACTIVE, true)
        editor.apply()
    }

    private fun loadDndPreferences() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        startTimeInput.setText(sharedPreferences.getString(KEY_START_TIME, ""))
        endTimeInput.setText(sharedPreferences.getString(KEY_END_TIME, ""))
        smsMessageInput.setText(sharedPreferences.getString(KEY_SMS_MESSAGE, getString(R.string.default_sms_message)))

        val savedDays = sharedPreferences.getStringSet(KEY_SELECTED_DAYS, setOf())
        savedDays?.forEach { day ->
            selectedDays.add(day)
            when (day) {
                "Mon" -> toggleButtonState(buttonMon, true)
                "Tue" -> toggleButtonState(buttonTue, true)
                "Wed" -> toggleButtonState(buttonWed, true)
                "Thu" -> toggleButtonState(buttonThu, true)
                "Fri" -> toggleButtonState(buttonFri, true)
                "Sat" -> toggleButtonState(buttonSat, true)
                "Sun" -> toggleButtonState(buttonSun, true)
            }
        }
    }

    private fun toggleButtonState(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_selected))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_normal))
        }
    }


    private val requestCodePermissions = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startTimeInput = findViewById(R.id.start_time_input)
        endTimeInput = findViewById(R.id.end_time_input)
        smsMessageInput = findViewById(R.id.sms_message_input)
        scheduleButton = findViewById(R.id.schedule_button)
        stopDndButton = findViewById(R.id.stop_dnd_button)

        buttonMon = findViewById(R.id.button_mon)
        buttonTue = findViewById(R.id.button_tue)
        buttonWed = findViewById(R.id.button_wed)
        buttonThu = findViewById(R.id.button_thu)
        buttonFri = findViewById(R.id.button_fri)
        buttonSat = findViewById(R.id.button_sat)
        buttonSun = findViewById(R.id.button_sun)

        // Add click listeners for day buttons
        buttonMon.setOnClickListener { toggleDaySelection("Mon", buttonMon) }
        buttonTue.setOnClickListener { toggleDaySelection("Tue", buttonTue) }
        buttonWed.setOnClickListener { toggleDaySelection("Wed", buttonWed) }
        buttonThu.setOnClickListener { toggleDaySelection("Thu", buttonThu) }
        buttonFri.setOnClickListener { toggleDaySelection("Fri", buttonFri) }
        buttonSat.setOnClickListener { toggleDaySelection("Sat", buttonSat) }
        buttonSun.setOnClickListener { toggleDaySelection("Sun", buttonSun) }

        requestPermissions()

        startTimeInput.setOnClickListener { showTimePickerDialog(startTimeInput) }
        endTimeInput.setOnClickListener { showTimePickerDialog(endTimeInput) }

        scheduleButton.setOnClickListener {
            saveDndPreferences()
            scheduleDndMode(scheduleButton)
        }
        stopDndButton.setOnClickListener {
            stopDndMode(stopDndButton)
            saveDndPreferences()
        }

        loadDndPreferences()
    }

    private fun toggleDaySelection(day: String, button: MaterialButton) {
        if (selectedDays.contains(day)) {
            selectedDays.remove(day)
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_normal))
        } else {
            selectedDays.add(day)
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_selected))
        }
    }



    override fun onPause() {
        super.onPause()
        saveDndPreferences()
    }

    override fun onStop() {
        super.onStop()
        saveDndPreferences()
    }

    private fun requestPermissions() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, requestCodePermissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions are required for the app to function", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTimePickerDialog(timeInput: TextInputEditText) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H) // This line was replaced to always use 12-hour format
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        picker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")

        picker.addOnPositiveButtonClickListener {
            val hour = if (picker.hour == 0 || picker.hour == 12) 12 else picker.hour % 12
            val minute = picker.minute
            val period = if (picker.hour >= 12) "PM" else "AM"
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, period)
            timeInput.setText(formattedTime)
        }
    }

    private fun isNotificationPolicyAccessGranted(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        startActivity(intent)
    }

    fun canScheduleExactAlarms(): Boolean {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    //schedule DND mode function below

    fun scheduleDndMode(view: View) {
        if (!isNotificationPolicyAccessGranted()) {
            requestNotificationPolicyAccess()
            return
        }

        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission()
            return
        }

        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, getHour(startTimeInput.text.toString()))
            set(Calendar.MINUTE, getMinute(startTimeInput.text.toString()))
            set(Calendar.SECOND, 0)
        }

        val endTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, getHour(endTimeInput.text.toString()))
            set(Calendar.MINUTE, getMinute(endTimeInput.text.toString()))
            set(Calendar.SECOND, 0)
        }

        val daysOfWeek = mapOf(
            "Mon" to Calendar.MONDAY,
            "Tue" to Calendar.TUESDAY,
            "Wed" to Calendar.WEDNESDAY,
            "Thu" to Calendar.THURSDAY,
            "Fri" to Calendar.FRIDAY,
            "Sat" to Calendar.SATURDAY,
            "Sun" to Calendar.SUNDAY
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (day in selectedDays) {
            val dayOfWeek = daysOfWeek[day] ?: continue

            // Set the start time alarm
            val startIntent = Intent(this, DndModeReceiver::class.java).apply {
                action = "START_DND_MODE"
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                this,
                dayOfWeek, // Use dayOfWeek as request code to differentiate alarms
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val startAlarmTime = startTime.clone() as Calendar
            startAlarmTime.set(Calendar.DAY_OF_WEEK, dayOfWeek)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startAlarmTime.timeInMillis, startPendingIntent)

            // Set the end time alarm
            val endIntent = Intent(this, DndModeReceiver::class.java).apply {
                action = "END_DND_MODE"
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                this,
                dayOfWeek + 1000, // Use a different request code to differentiate start and end alarms
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val endAlarmTime = endTime.clone() as Calendar
            endAlarmTime.set(Calendar.DAY_OF_WEEK, dayOfWeek)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, endAlarmTime.timeInMillis, endPendingIntent)
        }

        Toast.makeText(this, "DND schedule set successfully for selected days", Toast.LENGTH_SHORT).show()
    }



    //schedule DND mode function above
    private fun getHour(time: String): Int {
        val parts = time.split(" ", ":")
        var hour = parts[0].toInt()
        val period = parts[2]
        if (period.equals("PM", true) && hour != 12) {
            hour += 12
        } else if (period.equals("AM", true) && hour == 12) {
            hour = 0
        }
        return hour
    }

    private fun getMinute(time: String): Int {
        val parts = time.split(" ", ":")
        return parts[1].toInt()
    }

    fun stopDndMode(view: View) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("DND_ACTIVE", false)
        editor.apply()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        Toast.makeText(this, "DND mode stopped", Toast.LENGTH_SHORT).show()
    }
}
