package com.example.dndlion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", "Phone state changed: $state")

            // Attempt to get the incoming number
            val incomingNumber = getIncomingNumber(context, intent)
            Log.d("CallReceiver", "Incoming number: $incomingNumber")

            if (state == TelephonyManager.EXTRA_STATE_RINGING && incomingNumber != null) {
                val sharedPreferences = context.getSharedPreferences("DND_PREFS", Context.MODE_PRIVATE)
                val isDndActive = sharedPreferences.getBoolean("DND_ACTIVE", false)
                Log.d("CallReceiver", "Is DND active: $isDndActive")

                if (isDndActive) {
                    Log.d("CallReceiver", "Incoming call from: $incomingNumber")
                    sendSms(context, incomingNumber)
                }
            }
        }
    }

    private fun getIncomingNumber(context: Context, intent: Intent): String? {
        var incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (incomingNumber == null) {
            Log.d("CallReceiver", "Incoming number is null, attempting to retrieve from Call Log")
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) == PermissionChecker.PERMISSION_GRANTED) {
                incomingNumber = getLastIncomingCallNumber(context)
            } else {
                Log.d("CallReceiver", "READ_CALL_LOG permission not granted. Requesting permission.")
                showPermissionRequestNotification(context)
            }
        }

        return incomingNumber
    }

    private fun getLastIncomingCallNumber(context: Context): String? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.INCOMING_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC"
        )

        var incomingNumber: String? = null
        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

            if (numberIndex != -1 && dateIndex != -1 && it.moveToFirst()) {
                incomingNumber = it.getString(numberIndex)
                val callDate = it.getLong(dateIndex)
                Log.d("CallReceiver", "Retrieved call from Call Log - Number: $incomingNumber, Date: $callDate")
            } else {
                Log.d("CallReceiver", "Column index not found or no call log entry found.")
            }
        }
        return incomingNumber
    }

    private fun sendSms(context: Context, phoneNumber: String) {
        val sharedPreferences = context.getSharedPreferences("DND_PREFS", Context.MODE_PRIVATE)
        val customMessage = sharedPreferences.getString("SMS_MESSAGE", context.getString(R.string.default_sms_message))

        if (customMessage.isNullOrEmpty()) {
            Log.d("CallReceiver", "No SMS message to send")
            return
        }

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            Log.d("CallReceiver", "Attempting to send SMS to $phoneNumber with message: $customMessage")
            smsManager.sendTextMessage(phoneNumber, null, customMessage, null, null)
            Log.d("CallReceiver", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("CallReceiver", "Failed to send SMS", e)
        }
    }

    private fun showPermissionRequestNotification(context: Context) {
        // Notification logic to request permission
    }
}
