package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHubUpdateChecker handles in-app update checks and downloads directly from GitHub Releases.
 * It queries the repository's latest release, compares semantic versions, downloads the APK to the
 * secure app cache, and launches the standard Android package installer via FileProvider.
 */
object GitHubUpdateChecker {
    private const val TAG = "GitHubUpdateChecker"
    private const val GITHUB_REPO = "LuckyBoy587/PaperBundleApp"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    sealed class UpdateResult {
        data class Success(
            val latestVersion: String,
            val downloadUrl: String,
            val releaseNotes: String,
            val isUpdateAvailable: Boolean,
            val releaseUrl: String
        ) : UpdateResult()

        data class Error(val exception: Throwable) : UpdateResult()
    }

    /**
     * Checks if there's a newer release on GitHub compared to [currentVersionName].
     * Runs asynchronously on Dispatchers.IO.
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "PaperBundleApp-Updater")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "").trim()
                val releaseNotes = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "")

                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (tagName.isEmpty()) {
                    return@withContext UpdateResult.Error(Exception("No release tag name found in GitHub response."))
                }

                val hasUpdate = isNewerVersion(currentVersionName, tagName)
                UpdateResult.Success(
                    latestVersion = tagName,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isUpdateAvailable = hasUpdate,
                    releaseUrl = htmlUrl
                )
            } else {
                UpdateResult.Error(Exception("Failed to fetch updates. HTTP response code: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateResult.Error(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Helper algorithm to compare two semantic versions.
     * Works with prefixes like "v" (e.g., "v1.0.1" vs "1.0.0").
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()

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

    /**
     * Downloads the APK file from [downloadUrl] into the app's secure cache directory.
     * Reports progress back through [onProgress] (0.0f to 1.0f).
     * Runs asynchronously on Dispatchers.IO.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (downloadUrl.isEmpty()) {
            Log.e(TAG, "Download URL is empty.")
            return@withContext null
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val url = URL(downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                return@withContext null
            }

            val fileLength = connection.contentLength
            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength)
                }
                outputStream.write(data, 0, count)
            }
            outputStream.flush()
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK from $downloadUrl", e)
            null
        } finally {
            outputStream?.close()
            inputStream?.close()
            connection?.disconnect()
        }
    }

    /**
     * Triggers the package installer to prompt the user to install the downloaded APK.
     * Handles Android 7.0+ FileProvider URI matching.
     */
    fun triggerInstall(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Log.e(TAG, "APK file does not exist or is empty.")
                return false
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer intent", e)
            false
        }
    }
}
