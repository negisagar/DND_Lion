package com.example.dndlion

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "dnd_prefs"
val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

object DataStoreManager {
    private val START_TIME_KEY = stringPreferencesKey(Constants.PreferenceKeys.KEY_START_TIME)
    private val END_TIME_KEY = stringPreferencesKey(Constants.PreferenceKeys.KEY_END_TIME)
    private val SELECTED_DAYS_KEY = stringSetPreferencesKey(Constants.PreferenceKeys.KEY_SELECTED_DAYS)
    private val SMS_MESSAGE_KEY = stringPreferencesKey(Constants.PreferenceKeys.KEY_SMS_MESSAGE)
    private val IS_ACTIVE_KEY = booleanPreferencesKey(Constants.PreferenceKeys.KEY_IS_ACTIVE)

    data class DndSettings(
        val startTime: String = "",
        val endTime: String = "",
        val selectedDays: Set<String> = emptySet(),
        val smsMessage: String = Constants.Messages.DEFAULT_SMS_MESSAGE,
        val isActive: Boolean = false
    )

    fun getSettingsFlow(context: Context): Flow<DndSettings> {
        return context.dataStore.data.map { prefs ->
            DndSettings(
                startTime = prefs[START_TIME_KEY] ?: "",
                endTime = prefs[END_TIME_KEY] ?: "",
                selectedDays = prefs[SELECTED_DAYS_KEY] ?: emptySet(),
                smsMessage = prefs[SMS_MESSAGE_KEY] ?: Constants.Messages.DEFAULT_SMS_MESSAGE,
                isActive = prefs[IS_ACTIVE_KEY] ?: false
            )
        }
    }

    suspend fun updateSettings(context: Context, settings: DndSettings) {
        context.dataStore.edit { prefs ->
            prefs[START_TIME_KEY] = settings.startTime
            prefs[END_TIME_KEY] = settings.endTime
            prefs[SELECTED_DAYS_KEY] = settings.selectedDays
            prefs[SMS_MESSAGE_KEY] = settings.smsMessage
            prefs[IS_ACTIVE_KEY] = settings.isActive
        }
    }

    suspend fun clearSettings(context: Context) {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
