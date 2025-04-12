package com.example.dndlion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG = Constants.LogTags.MAIN_ACTIVITY

    // Managers and Utilities
    private lateinit var views: ViewHolder
    private lateinit var stateManager: StateManager
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var uiManager: MainActivityUI

    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received broadcast: ${intent?.action}")
            when (intent?.action) {
                Constants.Actions.ACTION_UPDATE_UI -> {
                    Log.d(TAG, "Received UI update broadcast")
                    uiManager.resetUIState()

                    // Only clear state if DND is being deactivated
                    if (!stateManager.isStateActive) {
                        stateManager.clearState()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate started")

        // Initialize managers and setup app
        initializeManagers()

        // Register UI update receiver after initializing managers
        registerUiReceiver()

        setupApplication()
        handleIntent(intent)

        // Restore previous state if exists
        restorePreviousState()
    }

    private fun registerUiReceiver() {
        try {
            val filter = IntentFilter(Constants.Actions.ACTION_UPDATE_UI)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(uiUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag") // Suppressing warning for older Android versions
                registerReceiver(uiUpdateReceiver, filter)
            }
            Log.d(TAG, "UI receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering UI receiver", e)
        }
    }

    private fun restorePreviousState() {
        try {
            if (stateManager.hasValidState()) {
                Log.d(TAG, "Restoring previous state")
                stateManager.restoreState(views)

                // If the schedule was active, update UI accordingly
                if (stateManager.isStateActive) {
                    uiManager.updateUIForActiveState()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring previous state", e)
        }
    }

    private fun initializeManagers() {
        try {
            Log.d(TAG, "Initializing managers")
            views = ViewHolder(this)
            stateManager = StateManager(this)
            alarmScheduler = AlarmScheduler(this)
            permissionHandler = PermissionHandler(this)

            uiManager = MainActivityUI(
                activity = this,
                views = views,
                stateManager = stateManager,
                alarmScheduler = alarmScheduler,
                permissionHandler = permissionHandler
            )
            Log.d(TAG, "Managers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing managers", e)
        }
    }

    private fun setupApplication() {
        try {
            Log.d(TAG, "Setting up application")
            uiManager.setupUI()
            checkRequiredPermissions()
            Log.d(TAG, "Application setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in application setup", e)
        }
    }

    private fun checkRequiredPermissions() {
        Log.d(TAG, "Checking required permissions")
        permissionHandler.checkAndRequestPermissions()
        permissionHandler.checkAndRequestAlarmPermission()
        checkDndPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent received with action: ${intent.action}")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "Handling intent with action: ${intent.action}")
        when (intent.action) {
            Constants.Actions.ACTION_RESCHEDULE -> {
                Log.d(TAG, "Handling reschedule action")
                val originalIntent = intent.getParcelableExtra<Intent>("ORIGINAL_INTENT")
                originalIntent?.let {
                    alarmScheduler.rescheduleForNextWeek(it)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Handling boot completed")
                restoreScheduleAfterBoot()
            }
        }
    }

    private fun restoreScheduleAfterBoot() {
        if (stateManager.isStateActive) {
            Log.d(TAG, "Restoring schedule after boot")
            val startTime = stateManager.getStoredStartTime()
            val endTime = stateManager.getStoredEndTime()
            val days = stateManager.getStoredDays()

            if (!startTime.isNullOrEmpty() && !endTime.isNullOrEmpty() && days.isNotEmpty()) {
                val startCalendar = TimeUtils.parseTime(startTime)
                val endCalendar = TimeUtils.parseTime(endTime)

                if (startCalendar != null && endCalendar != null) {
                    alarmScheduler.scheduleDndMode(startCalendar, endCalendar, days)
                    Log.d(TAG, "Schedule restored successfully")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "Permission result received: $requestCode")

        when (requestCode) {
            Constants.Permissions.PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
                    Log.d(TAG, "All permissions granted")
                } else {
                    Log.w(TAG, "Some permissions were denied")
                    permissionHandler.showPermissionExplanationDialog(
                        "Permissions Required",
                        Constants.Messages.PERMISSION_REQUIRED
                    ) {
                        permissionHandler.checkAndRequestPermissions()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        checkDndPermission()
    }

    private fun checkDndPermission() {
        Log.d(TAG, "Checking DND permission")
        if (!permissionHandler.isNotificationPolicyAccessGranted()) {
            Log.d(TAG, "DND permission not granted, showing explanation")
            permissionHandler.showPermissionExplanationDialog(
                title = "DND Permission Required",
                message = Constants.Messages.DND_PERMISSION_REQUIRED,
                onPositiveClick = { permissionHandler.requestNotificationPolicyAccess() }
            )
        } else {
            Log.d(TAG, "DND permission already granted")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "Saving instance state")
        try {
            stateManager.saveState(
                startTime = views.startTimeInput.text.toString(),
                endTime = views.endTimeInput.text.toString(),
                selectedDays = views.getSelectedDays(),
                smsMessage = views.smsMessageInput.text.toString(),
                isActive = views.stopDndButton.isEnabled
            )
            Log.d(TAG, "Instance state saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving instance state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        try {
            unregisterReceiver(uiUpdateReceiver)
            Log.d(TAG, "Successfully unregistered UI update receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    companion object {
        private const val INITIAL_DELAY = 1000L // 1 second delay for certain operations if needed
    }
}
