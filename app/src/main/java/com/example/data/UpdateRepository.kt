package com.example.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GitHubApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Represents the current state of the update verification and installation flow.
 */
sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String
    ) : UpdateUiState()
    data class NoUpdateAvailable(val currentVersion: String) : UpdateUiState()
    data class Downloading(val progress: Float) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
    object InstallReady : UpdateUiState()
}

/**
 * Repository layer handling APK update checking via Retrofit.
 * Triggers manual updates through the system browser and closes the app to allow clean updates.
 */
class UpdateRepository(private val application: Application) {

    companion object {
        private const val TAG = "UpdateRepository"
    }

    private val apiService = GitHubApiService.create()
    private val repositoryScope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Checks if a new release version is available on GitHub.
     */
    suspend fun checkForUpdates() {
        _uiState.value = UpdateUiState.Checking
        val currentVersion = BuildConfig.VERSION_NAME
        try {
            Log.d(TAG, "checkForUpdates: Current app version name = $currentVersion")

            val latestRelease = withContext(Dispatchers.IO) {
                apiService.getLatestRelease()
            }

            val latestVersionName = latestRelease.tagName
            Log.d(TAG, "checkForUpdates: Latest GitHub version name = $latestVersionName")

            val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
            if (apkAsset == null) {
                Log.e(TAG, "checkForUpdates: No APK file asset found in the latest GitHub release.")
                _uiState.value = UpdateUiState.Error("No APK found in latest release assets.")
                return
            }

            val hasUpdate = isNewerVersion(currentVersion, latestVersionName)
            if (hasUpdate) {
                Log.d(TAG, "checkForUpdates: Newer update version is available.")
                _uiState.value = UpdateUiState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = latestVersionName,
                    releaseNotes = latestRelease.body ?: "No release notes provided.",
                    downloadUrl = apkAsset.browserDownloadUrl
                )
            } else {
                Log.d(TAG, "checkForUpdates: Current version is up to date.")
                _uiState.value = UpdateUiState.NoUpdateAvailable(currentVersion)
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Log.d(TAG, "checkForUpdates: GitHub release API returned 404 (no releases found or private repo). Treating as no update available.")
                _uiState.value = UpdateUiState.NoUpdateAvailable(currentVersion)
            } else {
                Log.e(TAG, "checkForUpdates: Retrofit HTTP exception encountered", e)
                _uiState.value = UpdateUiState.Error("Failed to check for updates: HTTP ${e.code()} ${e.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdates: Network or parsing exception encountered", e)
            _uiState.value = UpdateUiState.Error("Failed to check for updates: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Resets the update check UI state back to idle.
     */
    fun resetState() {
        _uiState.value = UpdateUiState.Idle
    }

    /**
     * Opens the download URL in the system browser and exits the app process.
     * This bypasses direct installation prompts and lets the phone handle it manually.
     */
    fun startDownload(downloadUrl: String) {
        try {
            Log.d(TAG, "startDownload: Redirecting to browser for manual update: $downloadUrl")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            application.startActivity(intent)

            // Short delay to let the system register and launch the browser, then exit the app
            repositoryScope.launch {
                delay(500)
                Log.d(
                    TAG,
                    "startDownload: Exiting app process to allow manual update installation."
                )
                kotlin.system.exitProcess(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startDownload: Failed to open browser update", e)
            _uiState.value = UpdateUiState.Error("Failed to open browser: ${e.localizedMessage}")
        }
    }

    /**
     * Stub implementation of the legacy APK installation to maintain compatibility.
     */
    fun triggerApkInstall() {
        Log.w(TAG, "triggerApkInstall: Manual update is now handled via browser. Stub invoked.")
    }

    /**
     * Semantic version comparison algorithm.
     * Compares standard formats, e.g., '1.0' vs 'v1.1'.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.dropWhile { !it.isDigit() }.trim()
        val cleanLatest = latest.dropWhile { !it.isDigit() }.trim()

        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentVal = currentParts.getOrElse(i) { 0 }
            val latestVal = latestParts.getOrElse(i) { 0 }
            if (latestVal > currentVal) return true
            if (currentVal > latestVal) return false
        }
        return false
    }
}
