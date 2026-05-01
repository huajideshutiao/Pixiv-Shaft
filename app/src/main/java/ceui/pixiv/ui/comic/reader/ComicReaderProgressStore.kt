package ceui.pixiv.ui.comic.reader

import android.content.SharedPreferences
import androidx.core.content.edit
import ceui.lisa.activities.Shaft

/** 每个 illust 的阅读进度（最后一次的页面索引 + 时间戳）。 */
object ComicReaderProgressStore {

    private const val PREFS_NAME = "comic_reader_v3_progress"
    private val store: SharedPreferences by lazy { Shaft.getNamedPrefs(PREFS_NAME) }

    fun savePage(illustId: Long, pageIndex: Int, totalPages: Int) {
        store.edit {
            putInt("page_$illustId", pageIndex)
            putInt("total_$illustId", totalPages)
            putLong("time_$illustId", System.currentTimeMillis())
        }
    }

    fun lastPage(illustId: Long): Int = store.getInt("page_$illustId", 0)

    fun lastReadTime(illustId: Long): Long = store.getLong("time_$illustId", 0L)

    fun clear(illustId: Long) {
        store.edit {
            remove("page_$illustId")
            remove("total_$illustId")
            remove("time_$illustId")
        }
    }
}
