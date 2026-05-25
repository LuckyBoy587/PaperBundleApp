package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

/**
 * Data model representing an asset in a GitHub Release (e.g. an APK file).
 */
data class GitHubAsset(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "size") val size: Long
)

/**
 * Data model representing the latest GitHub Release returned from the REST API.
 */
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "body") val body: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "assets") val assets: List<GitHubAsset>
)

/**
 * Retrofit API Service to communicate with the GitHub REST API.
 */
interface GitHubApiService {

    /**
     * Fetches the latest published release for the repository.
     */
    @GET("repos/LuckyBoy587/PaperBundleApp/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        /**
         * Factory function to instantiate the Retrofit service with Moshi parsing and custom timeouts.
         */
        fun create(): GitHubApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "PaperBundleApp-M3Updater")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}
