package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UpdateRepository
import com.example.data.UpdateUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating update checking, downloading, and UI state management for the settings updates screen.
 */
class UpdateViewModel(
    application: Application,
    private val repository: UpdateRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    // Expose lifecycle-aware Update UI State flow directly from Repository
    val uiState: StateFlow<UpdateUiState> = repository.uiState

    /**
     * Checks GitHub API asynchronously for available application updates.
     */
    fun checkForUpdates() {
        Log.d(TAG, "checkForUpdates: Initialized update check trigger")
        viewModelScope.launch {
            repository.checkForUpdates()
        }
    }

    /**
     * Triggers the APK file download from the specified release asset URL.
     */
    fun startDownload(downloadUrl: String) {
        Log.d(TAG, "startDownload: Initiating download for URL: $downloadUrl")
        repository.startDownload(downloadUrl)
    }

    /**
     * Resets the update checking flow to Idle.
     */
    fun resetState() {
        Log.d(TAG, "resetState: Clearing update checking state flow")
        repository.resetState()
    }

    /**
     * Forces standard installation invocation if needed.
     */
    fun installApk() {
        Log.d(TAG, "installApk: Forcing installation intent")
        repository.triggerApkInstall()
    }
}

/**
 * Factory class to permit clean instantiation of the UpdateViewModel with specific repository dependencies.
 */
class UpdateViewModelFactory(
    private val application: Application,
    private val repository: UpdateRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UpdateViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class. Expected UpdateViewModel.")
    }
}
