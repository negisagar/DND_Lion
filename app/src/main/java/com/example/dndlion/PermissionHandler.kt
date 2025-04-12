package com.example.dndlion

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionHandler(private val activity: AppCompatActivity) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    }

    fun checkAndRequestPermissions() {
        if (!checkPermissions()) {
            requestPermissions()
        }
    }

    fun checkAndRequestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showAlarmPermissionDialog()
            }
        }
    }

    private fun showAlarmPermissionDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permission Required")
            .setMessage("This app needs permission to schedule exact alarms for DND mode to work properly.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    activity.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    })
                } catch (e: Exception) {
                    Toast.makeText(activity, "Could not open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    fun isNotificationPolicyAccessGranted(): Boolean {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        activity.startActivity(intent)
    }

    fun canScheduleExactAlarms(): Boolean {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onPositiveClick: () -> Unit
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ -> onPositiveClick() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun handlePermissionDenial() {
        showPermissionExplanationDialog(
            "Permissions Required",
            Constants.Messages.PERMISSION_REQUIRED
        ) {
            checkAndRequestPermissions()
        }
    }
}
