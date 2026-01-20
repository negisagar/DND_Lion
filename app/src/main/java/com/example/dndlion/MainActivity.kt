package com.example.dndlion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.dndlion.ui.screens.MainScreen
import com.example.dndlion.ui.theme.DNDLionTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = Constants.LogTags.MAIN_ACTIVITY

    // Managers and Utilities
    private val viewModel: DndViewModel by viewModels()
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        
        // Add exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Scale up and fade out animation
            splashScreenView.iconView?.animate()
                ?.scaleX(1.5f)
                ?.scaleY(1.5f)
                ?.alpha(0f)
                ?.setDuration(300L)
                ?.setInterpolator(AccelerateDecelerateInterpolator())
                ?.withEndAction {
                    splashScreenView.remove()
                }
                ?.start()
            
            // Fade out the background
            splashScreenView.view.animate()
                .alpha(0f)
                .setDuration(300L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate started")

        // Initialize managers
        initializeManagers()

        setContent {
            DNDLionTheme {
                MainScreen(
                    viewModel = viewModel,
                    alarmScheduler = alarmScheduler,
                    permissionHandler = permissionHandler
                )
            }
        }
        
        setupApplication()
        handleIntent(intent)
    }

    private fun initializeManagers() {
        try {
            Log.d(TAG, "Initializing managers")
            alarmScheduler = AlarmScheduler(this)
            permissionHandler = PermissionHandler(this)
            Log.d(TAG, "Managers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing managers", e)
        }
    }

    private fun setupApplication() {
        try {
            Log.d(TAG, "Setting up application")
            checkRequiredPermissions()
            Log.d(TAG, "Application setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in application setup", e)
        }
    }

    private fun checkRequiredPermissions() {
        Log.d(TAG, "Checking required permissions")
        // We let the UI handle the interactive request, but we can do an initial check here if needed
        // For now, MainScreen handles the interactive part.
        // We might want to check exact alarm permission though
        permissionHandler.checkAndRequestAlarmPermission()
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
        lifecycleScope.launch {
            viewModel.settings.collectLatest { settings ->
                if (settings.isActive) {
                    Log.d(TAG, "Restoring schedule after boot")
                    val startTime = settings.startTime
                    val endTime = settings.endTime
                    val days = settings.selectedDays
                    if (startTime.isNotEmpty() && endTime.isNotEmpty() && days.isNotEmpty()) {
                        val startCalendar = TimeUtils.parseTime(startTime)
                        val endCalendar = TimeUtils.parseTime(endTime)
                        if (startCalendar != null && endCalendar != null) {
                            alarmScheduler.scheduleDndMode(startCalendar, endCalendar, days)
                            Log.d(TAG, "Schedule restored successfully")
                        }
                    }
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
                    // The UI will handle showing specific rationale if needed via PermissionHandler
                    // or we can show a toast here
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        // Check permissions again in case user granted them in settings
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    companion object {
        private const val INITIAL_DELAY = 1000L 
    }
}
