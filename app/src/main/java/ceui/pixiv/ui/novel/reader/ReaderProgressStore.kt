package ceui.pixiv.ui.novel.reader

import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences-backed per-novel progress storage. Serves as the bridge layer
 * until the full Room-backed reading stats are wired up; callers get the same
 * API so switching later is a one-line change.
 */
object ReaderProgressStore {

    private const val PREFS_NAME = "novel_reader_v3_progress"

    private val store: SharedPreferences by lazy { ceui.lisa.activities.Shaft.getNamedPrefs(PREFS_NAME) }

    fun saveProgress(novelId: Long, charIndex: Int, pageIndex: Int, totalPages: Int) {
        store.edit()
            .putInt("char_$novelId", charIndex)
            .putInt("page_$novelId", pageIndex)
            .putInt("total_$novelId", totalPages)
            .putLong("time_$novelId", System.currentTimeMillis())
            .apply()
    }

    fun loadCharIndex(novelId: Long): Int = store.getInt("char_$novelId", 0)

    fun loadLastPageIndex(novelId: Long): Int = store.getInt("page_$novelId", 0)

    fun loadLastReadTime(novelId: Long): Long = store.getLong("time_$novelId", 0L)

    fun clear(novelId: Long) {
        store.edit()
            .remove("char_$novelId")
            .remove("page_$novelId")
            .remove("total_$novelId")
            .remove("time_$novelId")
            .apply()
    }
}
