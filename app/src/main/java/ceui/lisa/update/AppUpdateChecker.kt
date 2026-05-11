package ceui.lisa.update

import android.content.SharedPreferences
import androidx.core.content.edit
import ceui.lisa.activities.Shaft
import ceui.lisa.BuildConfig
import ceui.lisa.core.NetCallback
import ceui.lisa.core.executeCall
import ceui.lisa.http.Retro
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.function.Function

object AppUpdateChecker {

    private const val KEY_LAST_CHECK_TIME = "update_last_check_time"
    private const val KEY_SKIPPED_VERSION = "update_skipped_version"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    private val api: GitHubApi by lazy {
        val client = Retro.getLogClient()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .build()
                chain.proceed(request)
            }
            .build()
        Retrofit.Builder()
            .baseUrl(GitHubApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
            .create(GitHubApi::class.java)
    }

    fun fetchAllReleases(callback: (List<GitHubRelease>?, Throwable?) -> Unit) {
        executeCall(
            api.getReleases(GitHubApi.OWNER, GitHubApi.REPO), Function.identity(),
            object : NetCallback<List<GitHubRelease>>() {
                override fun onSuccess(t: List<GitHubRelease>) {
                    callback(t, null)
                }

                override fun onError(e: Throwable) {
                    callback(null, e)
                }
            })
    }

    fun checkForUpdate(callback: (UpdateResult?, Throwable?) -> Unit) {
        executeCall(
            api.getLatestRelease(GitHubApi.OWNER, GitHubApi.REPO), Function.identity(),
            object : NetCallback<GitHubRelease>() {
                override fun onSuccess(release: GitHubRelease) {
                    val remoteVersion = release.tagName.removePrefix("v").removePrefix("V")
                    val currentVersion = BuildConfig.VERSION_NAME
                    if (isNewerVersion(remoteVersion, currentVersion)) {
                        callback(UpdateResult.UpdateAvailable(release), null)
                    } else {
                        callback(UpdateResult.NoUpdate(remoteVersion), null)
                    }
                }

                override fun onError(e: Throwable) {
                    callback(null, e)
                }
            })
    }

    fun shouldAutoCheck(): Boolean {
        if (BuildConfig.UPDATE_CHANNEL != "github") return false
        val prefs = Shaft.getDefaultPrefs()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        return System.currentTimeMillis() - lastCheck > CHECK_INTERVAL_MS
    }

    fun markChecked() {
        Shaft.getDefaultPrefs().edit { putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()) }
    }

    fun skipVersion(version: String) {
        Shaft.getDefaultPrefs().edit { putString(KEY_SKIPPED_VERSION, version) }
    }

    fun isVersionSkipped(version: String): Boolean {
        return Shaft.getDefaultPrefs().getString(KEY_SKIPPED_VERSION, "") == version
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun findApkAsset(release: GitHubRelease): GitHubAsset? {
        val assets = release.assets ?: return null
        return assets.firstOrNull {
            it.name.endsWith(".apk") && it.name.contains("github", ignoreCase = true)
        } ?: assets.firstOrNull {
            it.name.endsWith(".apk") && it.name.contains("release", ignoreCase = true)
        } ?: assets.firstOrNull {
            it.name.endsWith(".apk")
        }
    }

    sealed class UpdateResult {
        data class UpdateAvailable(val release: GitHubRelease) : UpdateResult()
        data class NoUpdate(val remoteVersion: String) : UpdateResult()
    }
}