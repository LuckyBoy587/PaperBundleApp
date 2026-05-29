package com.example.ui.screens.settings

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UpdateRepository
import com.example.data.UpdateUiState
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val updateRepository: UpdateRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("PaperBundlePrefs", Context.MODE_PRIVATE)

    // Current language (saved to Prefs)
    val curLanguage = MutableStateFlow(
        Language.valueOf(prefs.getString("Language", Language.EN.name) ?: Language.EN.name)
    )

    // Selected profile owner (defaults to user's UID or empty)
    val curProfile = MutableStateFlow(
        prefs.getString("Profile", "") ?: ""
    )

    // Expose family members State Flow
    val familyMembers = FirebaseSyncManager.familyMembers

    // Expose lifecycle-aware Update UI State flow directly from Repository
    val uiState: StateFlow<UpdateUiState> = updateRepository.uiState

    fun setLanguage(language: Language) {
        curLanguage.value = language
        prefs.edit { putString("Language", language.name) }
    }

    fun logout(context: Context, onLogoutComplete: () -> Unit) {
        Log.d("SettingsViewModel", "SettingsViewModel: logout() called")
        viewModelScope.launch {
            FirebaseSyncManager.saveSession(context, null)
            onLogoutComplete()
        }
    }

    fun checkForUpdates() {
        Log.d("SettingsViewModel", "checkForUpdates: Initialized update check trigger")
        viewModelScope.launch {
            updateRepository.checkForUpdates()
        }
    }

    fun startDownload(downloadUrl: String) {
        Log.d("SettingsViewModel", "startDownload: Initiating download for URL: $downloadUrl")
        updateRepository.startDownload(downloadUrl)
    }

    fun resetState() {
        Log.d("SettingsViewModel", "resetState: Clearing update checking state flow")
        updateRepository.resetState()
    }

    fun installApk() {
        Log.d("SettingsViewModel", "installApk: Forcing installation intent")
        updateRepository.triggerApkInstall()
    }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val updateRepository: UpdateRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, updateRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
