package com.example.dndlion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel to expose DND settings via DataStore and handle updates.
 */
class DndViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    /** Flow of current DND settings */
    val settings: StateFlow<DataStoreManager.DndSettings> =
        DataStoreManager.getSettingsFlow(context)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                DataStoreManager.DndSettings()
            )

    /** Update all settings in DataStore */
    fun updateSettings(settings: DataStoreManager.DndSettings) {
        viewModelScope.launch {
            DataStoreManager.updateSettings(context, settings)
        }
    }

    /** Clear all settings in DataStore */
    fun clearSettings() {
        viewModelScope.launch {
            DataStoreManager.clearSettings(context)
        }
    }
}
