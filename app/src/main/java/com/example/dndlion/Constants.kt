package com.example.dndlion

object Constants {
    // Nested Actions object
    object Actions {
        const val ACTION_START_DND = "START_DND_MODE"
        const val ACTION_END_DND = "END_DND_MODE"
        const val ACTION_RESCHEDULE = "RESCHEDULE_DND"
        const val ACTION_UPDATE_UI = "UPDATE_UI_STATE"
    }

    object LogTags {
        const val MAIN_ACTIVITY = "MainActivity"
        const val STATE_MANAGER = "StateManager"
        const val ALARM_SCHEDULER = "AlarmScheduler"
        const val DND_RECEIVER = "DndReceiver"
        const val PERMISSION_HANDLER = "PermissionHandler"
        const val CALL_RECEIVER = "CallReceiver"
    }

    object Permissions {
        const val PERMISSION_REQUEST_CODE = 123
        // Add any other permission-related constants here
    }

    object Messages {
        const val DEFAULT_SMS_MESSAGE = "I'm currently unavailable. Will call you back later."
        const val PERMISSION_REQUIRED = "This app requires certain permissions to function properly."
        const val DND_PERMISSION_REQUIRED = "Please grant DND access to enable Do Not Disturb mode."
        const val ALARM_PERMISSION_REQUIRED = "Please grant alarm permission to schedule DND mode."
    }

    object PreferenceKeys {
        const val PREFS_NAME = "DNDLionPrefs"
        const val KEY_START_TIME = "start_time"
        const val KEY_END_TIME = "end_time"
        const val KEY_SELECTED_DAYS = "selected_days"
        const val KEY_SMS_MESSAGE = "sms_message"
        const val KEY_IS_ACTIVE = "is_active"
    }
}
