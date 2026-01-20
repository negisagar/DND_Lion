package com.example.dndlion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DndModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(Constants.LogTags.DND_RECEIVER, "DndModeReceiver: onReceive called with action: $action")

        when (action) {
            Constants.Actions.ACTION_START_DND -> {
                Log.d(Constants.LogTags.DND_RECEIVER, "Attempting to enable DND mode")
                setDndMode(context, true)
            }
            Constants.Actions.ACTION_END_DND -> {
                Log.d(Constants.LogTags.DND_RECEIVER, "Attempting to disable DND mode")
                setDndMode(context, false)
            }
        }
    }

    private fun setDndMode(context: Context, enable: Boolean) {
        val notificationManager = ContextCompat.getSystemService(context, android.app.NotificationManager::class.java)

        if (notificationManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
                Log.d(Constants.LogTags.DND_RECEIVER, "ACCESS_NOTIFICATION_POLICY permission not granted.")
                return
            }

            try {
                notificationManager.setInterruptionFilter(
                    if (enable) android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                    else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                )

                // Update isActive state in DataStore
                CoroutineScope(Dispatchers.IO).launch {
                    val current = DataStoreManager.getSettingsFlow(context).first()
                    DataStoreManager.updateSettings(
                        context,
                        current.copy(isActive = enable)
                    )
                }

                if (enable) {
                    Log.d(Constants.LogTags.DND_RECEIVER, "DND mode enabled successfully and is_active set to true")
                    showToast(context, "DND mode enabled")
                    scheduleNextWeekAlarms(context)
                } else {
                    Log.d(Constants.LogTags.DND_RECEIVER, "DND mode disabled successfully and is_active set to false")
                    showToast(context, "DND mode disabled")
                }
            } catch (e: SecurityException) {
                Log.e(Constants.LogTags.DND_RECEIVER, "SecurityException when setting DND mode", e)
            } catch (e: Exception) {
                Log.e(Constants.LogTags.DND_RECEIVER, "Error while setting DND mode", e)
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleNextWeekAlarms(context: Context) {
        // Logic to reschedule alarms for next week
        Log.d(Constants.LogTags.DND_RECEIVER, "Scheduled next week's alarms")
    }
}
