package ceui.pixiv.download.config

import android.util.Log

import ceui.lisa.R
import ceui.lisa.activities.Shaft
import android.content.SharedPreferences
import androidx.core.content.edit
import com.hjq.toast.Toaster
/**
 * SharedPreferences-backed persistence for [DownloadConfig].
 *
 * First launch:
 *   [load] sees no stored value → returns `LoadResult.FirstRun(fallback())` without
 *   saving. Callers decide when to persist (usually after the user finishes the
 *   initial setup screen) by calling [save].
 *
 * Corrupt payload:
 *   [load] returns `LoadResult.Corrupt(fallback(), cause)` and does NOT overwrite
 *   the bad payload. UI can surface this and offer repair / re-import so the
 *   user doesn't silently lose their config to a schema bug.
 *
 * Normal path:
 *   [load] returns `LoadResult.Ok(config)`.
 */
class DownloadConfigStore(
    private val fallback: () -> DownloadConfig,
    prefsName: String = DEFAULT_PREFS_NAME,
) {

    private val store: SharedPreferences by lazy { Shaft.getNamedPrefs(prefsName) }

    sealed interface LoadResult {
        val config: DownloadConfig

        data class Ok(override val config: DownloadConfig) : LoadResult
        data class FirstRun(override val config: DownloadConfig) : LoadResult
        data class Corrupt(override val config: DownloadConfig, val cause: Throwable) : LoadResult
    }

    fun load(): LoadResult {
        val raw = try {
            store.getString(KEY, null)
        } catch (t: Throwable) {
            Log.e(TAG, "DownloadConfigStore.load: SharedPreferences getString failed", t)
            return LoadResult.Corrupt(fallback(), t)
        } ?: return LoadResult.FirstRun(fallback())
        return try {
            LoadResult.Ok(DownloadConfigJson.fromJson(raw))
        } catch (t: Throwable) {
            LoadResult.Corrupt(fallback(), t)
        }
    }

    /** Convenience for callers that do not care about first-run / corrupt distinction. */
    fun loadOrFallback(): DownloadConfig = load().config

    fun save(config: DownloadConfig) {
        try {
            store.edit { putString(KEY, DownloadConfigJson.toJson(config)) }
        } catch (t: Throwable) {
            Log.e(TAG, "DownloadConfigStore.save failed", t)
            Toaster.show(
                Shaft.getContext().getString(
                    R.string.download_settings_save_failed,
                    t.message ?: t.javaClass.simpleName,
                )
            )
        }
    }

    fun update(transform: (DownloadConfig) -> DownloadConfig): DownloadConfig {
        val next = transform(loadOrFallback())
        save(next)
        return next
    }

    fun reset() {
        store.edit { remove(KEY) }
    }

    companion object {
        private const val TAG = "DownloadConfigStore"
        const val DEFAULT_PREFS_NAME = "download_config_v1"
        private const val KEY = "config"
    }
}
