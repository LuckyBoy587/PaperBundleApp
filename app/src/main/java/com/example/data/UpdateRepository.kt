package com.example.data

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.api.GitHubApiService
import com.example.data.api.GitHubRelease
import retrofit2.HttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
 * Repository layer handling APK update checking via Retrofit and background downloading via DownloadManager.
 */
class UpdateRepository(private val application: Application) {

    companion object {
        private const val TAG = "UpdateRepository"
        private const val FILE_NAME = "PaperBundleUpdate.apk"
    }

    private val apiService = GitHubApiService.create()
    private val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val repositoryScope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var activeDownloadId: Long? = null
    private var progressJob: Job? = null
    private var downloadReceiver: BroadcastReceiver? = null

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
     * Starts downloading the APK from [downloadUrl] using DownloadManager.
     */
    fun startDownload(downloadUrl: String) {
        if (activeDownloadId != null) {
            Log.w(TAG, "startDownload: Download already in progress.")
            return
        }

        try {
            // Cleanup existing downloaded APK file to prevent duplicate names/conflicts
            cleanupPreviousDownloads()

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("PaperBundle Update")
                setDescription("Downloading the latest version...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            activeDownloadId = downloadId
            _uiState.value = UpdateUiState.Downloading(0.0f)

            // Register dynamically for completion
            registerDownloadReceiver(downloadId)

            // Monitor download progress
            startProgressMonitoring(downloadId)

            Log.d(TAG, "startDownload: Enqueued download job with ID $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "startDownload: Failed to initialize download", e)
            _uiState.value = UpdateUiState.Error("Failed to start download: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Registers dynamic BroadcastReceiver to detect completion of the update download.
     */
    private fun registerDownloadReceiver(downloadId: Long) {
        if (downloadReceiver != null) {
            unregisterDownloadReceiver()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (completedId == downloadId) {
                    Log.d(TAG, "Download complete broadcast received for ID: $downloadId")
                    handleDownloadCompletion(downloadId)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(receiver, filter)
        }
        downloadReceiver = receiver
    }

    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                application.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "unregisterDownloadReceiver: Already unregistered or failed", e)
            }
        }
        downloadReceiver = null
    }

    /**
     * Polls the DownloadManager Cursor periodically to track actual bytes loaded.
     */
    private fun startProgressMonitoring(downloadId: Long) {
        progressJob?.cancel()
        progressJob = repositoryScope.launch(Dispatchers.IO) {
            var downloading = true
            while (downloading) {
                delay(500)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                withContext(Dispatchers.Main) {
                                    _uiState.value = UpdateUiState.Downloading(progress)
                                }
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            Log.e(TAG, "Download failed with reason: $reason")
                            downloading = false
                            withContext(Dispatchers.Main) {
                                _uiState.value = UpdateUiState.Error("Download failed. (Error code: $reason)")
                                cleanupDownloadState()
                            }
                        }
                    }
                }
                cursor?.close()
            }
        }
    }

    /**
     * Checks download completion, cancels polling jobs, and launches the installer flow.
     */
    private fun handleDownloadCompletion(downloadId: Long) {
        cleanupDownloadState()

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FILE_NAME
        )

        if (file.exists() && file.length() > 0) {
            _uiState.value = UpdateUiState.InstallReady
            Log.d(TAG, "handleDownloadCompletion: APK successfully downloaded. Triggering installation intent.")
            triggerApkInstall(file)
        } else {
            Log.e(TAG, "handleDownloadCompletion: Downloaded file is missing or corrupted.")
            _uiState.value = UpdateUiState.Error("Downloaded file is missing or empty.")
        }
    }

    /**
     * Launches the Package Installer to install the downloaded APK.
     */
    fun triggerApkInstall(apkFile: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "triggerApkInstall: Target APK file does not exist at ${apkFile.absolutePath}")
                _uiState.value = UpdateUiState.Error("Apk file not found for installation.")
                return
            }

            // On Android 8.0 (API 26) or higher, check for "Install Unknown Apps" permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!application.packageManager.canRequestPackageInstalls()) {
                    Log.w(TAG, "triggerApkInstall: Install Unknown Apps permission is missing.")
                    Toast.makeText(
                        application,
                        "Please allow PaperBundle to install unknown apps.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${application.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    application.startActivity(intent)
                    return
                }
            }

            val authority = "${application.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(application, authority, apkFile)
            Log.d(TAG, "triggerApkInstall: Resolved FileProvider URI: $apkUri")

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            application.startActivity(installIntent)
            _uiState.value = UpdateUiState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "triggerApkInstall: Exception launching Package Installer", e)
            _uiState.value = UpdateUiState.Error("Failed to open package installer: ${e.localizedMessage}")
        }
    }

    private fun cleanupDownloadState() {
        progressJob?.cancel()
        progressJob = null
        unregisterDownloadReceiver()
        activeDownloadId = null
    }

    private fun cleanupPreviousDownloads() {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME
            )
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "cleanupPreviousDownloads: Existing APK deleted = $deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "cleanupPreviousDownloads: Failed to clean up", e)
        }
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
