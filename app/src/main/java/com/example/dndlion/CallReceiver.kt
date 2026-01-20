package com.example.dndlion

import android.Manifest
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    private val TAG = "CallReceiver" // Consider moving to Constants.LogTags.CALL_RECEIVER

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.d(TAG, "Phone state changed: $state, Incoming number: $incomingNumber")

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            if (!incomingNumber.isNullOrEmpty()) {
                handleIncomingCall(context, incomingNumber)
            } else {
                // Use a delay to allow time for the call log to be updated as a fallback
                Handler(Looper.getMainLooper()).postDelayed({
                    val fallbackNumber = getLastIncomingCallNumber(context)
                    if (!fallbackNumber.isNullOrEmpty()) {
                        handleIncomingCall(context, fallbackNumber)
                    } else {
                        Log.d(TAG, "Fallback number is null or empty. SMS will not be sent.")
                    }
                }, 500) // Reduced delay to 500 ms for faster SMS handling
            }
        }
    }

    private fun handleIncomingCall(context: Context, incomingNumber: String?) {
        Log.d(TAG, "Handling incoming number: $incomingNumber")

        if (!incomingNumber.isNullOrEmpty()) {
            // Using CoroutineScope to read DataStore since it's a suspending function
            val goAsync = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val settings = DataStoreManager.getSettingsFlow(context).first()
                    val isDndActive = settings.isActive
                    Log.d(TAG, "Is DND active: $isDndActive")

                    if (isDndActive) {
                        Log.d(TAG, "DND is active. Preparing to send SMS to: $incomingNumber")
                        val smsMessage = settings.smsMessage
                        sendSms(context, incomingNumber, smsMessage)
                    } else {
                        Log.d(TAG, "DND is not active, SMS will not be sent")
                    }
                } finally {
                    goAsync.finish()
                }
            }
        } else {
            Log.d(TAG, "Incoming number is null or empty, SMS will not be sent")
        }
    }

    private fun getLastIncomingCallNumber(context: Context): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_CALL_LOG permission not granted.")
            return null
        }

        val contentResolver = context.contentResolver
        var incomingNumber: String? = null

        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.INCOMING_TYPE.toString(), (System.currentTimeMillis() - 20000).toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    if (numberIndex != -1) {
                        incomingNumber = it.getString(numberIndex)
                        Log.d(TAG, "Retrieved call from Call Log - Number: $incomingNumber")
                    }
                } else {
                    Log.d(TAG, "No recent incoming calls found in Call Log.")
                }
                Unit // Ensure block returns Unit
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying call log", e)
        }

        return incomingNumber
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (message.isBlank()) {
            Log.d(TAG, "SMS message is blank. SMS will not be sent.")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "SEND_SMS permission not granted.")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            Log.d(TAG, "Attempting to send SMS to $phoneNumber with message: $message")
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
        }
    }
}
