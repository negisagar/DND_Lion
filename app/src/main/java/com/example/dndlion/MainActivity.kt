package com.example.dndlion

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            button.setTextColor(ContextCompat.getColor(this, R.color.button_normal)) // Adjust text color if needed
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_normal))
            button.setTextColor(ContextCompat.getColor(this, R.color.white)) // Adjust text color if needed
        }
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

    private fun toggleDaySelection(day: String, button: MaterialButton) {
        if (selectedDays.contains(day)) {
            selectedDays.remove(day)
            toggleButtonState(button, false)
        } else {
            selectedDays.add(day)
            toggleButtonState(button, true)
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

        // Load preferences and update UI
        loadDndPreferences()
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

    fun scheduleDndMode(view: View) {
        if (!isNotificationPolicyAccessGranted()) {
            requestNotificationPolicyAccess()
            return
        }

        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, getHour(startTimeInput.text.toString()))
            set(Calendar.MINUTE, getMinute(startTimeInput.text.toString()))
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val endTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, getHour(endTimeInput.text.toString()))
            set(Calendar.MINUTE, getMinute(endTimeInput.text.toString()))
            set(Calendar.SECOND, 0)
        }.timeInMillis

        // Save schedule to shared preferences
        val sharedPreferences = getSharedPreferences("DND_PREFS", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("START_HOUR", getHour(startTimeInput.text.toString()))
        editor.putInt("START_MINUTE", getMinute(startTimeInput.text.toString()))
        editor.putInt("END_HOUR", getHour(endTimeInput.text.toString()))
        editor.putInt("END_MINUTE", getMinute(endTimeInput.text.toString()))
        editor.putBoolean("DND_ACTIVE", true)

        val smsMessage = findViewById<TextInputEditText>(R.id.sms_message_input).text.toString()
        if (smsMessage.isNotEmpty()) {
            editor.putString("SMS_MESSAGE", smsMessage)
        } else {
            editor.remove("SMS_MESSAGE")
        }

        editor.apply()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(this, DndModeReceiver::class.java).apply {
            action = "START_DND_MODE"
        }
        val endIntent = Intent(this, DndModeReceiver::class.java).apply {
            action = "END_DND_MODE"
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val endPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime, startPendingIntent)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, endPendingIntent)
            Toast.makeText(this, "DND schedule set successfully", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Please grant the Schedule Exact Alarm permission in the system settings.", Toast.LENGTH_LONG).show()
        }
    }


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
        // Update DND status in shared preferences
        val sharedPreferences = getSharedPreferences("DND_PREFS", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("DND_ACTIVE", false)
        editor.apply()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        Toast.makeText(this, "DND mode stopped", Toast.LENGTH_SHORT).show()
    }
}
