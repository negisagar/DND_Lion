package com.example.dndlion

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DndModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            "START_DND_MODE" -> {
                Log.d("DndModeReceiver", "Starting DND mode")
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                setDndModeStatus(context, true)
            }
            "END_DND_MODE" -> {
                Log.d("DndModeReceiver", "Ending DND mode")
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                setDndModeStatus(context, false)
            }
        }
    }

    private fun setDndModeStatus(context: Context, isActive: Boolean) {
        val sharedPreferences = context.getSharedPreferences("DND_PREFS", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("DND_ACTIVE", isActive)
        editor.apply()
    }
}
