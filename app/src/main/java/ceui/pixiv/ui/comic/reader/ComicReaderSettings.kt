package ceui.pixiv.ui.comic.reader

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ceui.lisa.activities.Shaft

/**
 * 漫画 reader 的持久化设置。独立 SharedPreferences 文件与小说 reader 隔离。
 * 任何字段写入后会通过 [changes] 通知 UI 即时刷新。
 */
object ComicReaderSettings {

    private const val PREFS_NAME = "comic_reader_v3"
    private val store: SharedPreferences by lazy { Shaft.getNamedPrefs(PREFS_NAME) }

    private val _changes = MutableLiveData<ChangeEvent>()
    val changes: LiveData<ChangeEvent> = _changes

    private fun emit(e: ChangeEvent) = _changes.postValue(e)

    sealed class ChangeEvent {
        object Layout : ChangeEvent()
        object Theme : ChangeEvent()
        object Brightness : ChangeEvent()
        object Interaction : ChangeEvent()
        object Image : ChangeEvent()
    }

    enum class ReadingMode { Paged, Webtoon }
    enum class PageDirection { LTR, RTL }
    enum class FitMode { FitWidth, FitScreen, FitOriginal }
    enum class FlipAnim { Slide, Cover, Depth, FlipBook }

    var readingMode: ReadingMode
        get() = runCatching {
            ReadingMode.valueOf(store.getString(K_MODE, ReadingMode.Paged.name) ?: ReadingMode.Paged.name)
        }.getOrDefault(ReadingMode.Paged)
        set(value) { store.edit { putString(K_MODE, value.name) }; emit(ChangeEvent.Layout) }

    var pageDirection: PageDirection
        get() = runCatching {
            PageDirection.valueOf(store.getString(K_DIRECTION, PageDirection.LTR.name) ?: PageDirection.LTR.name)
        }.getOrDefault(PageDirection.LTR)
        set(value) { store.edit { putString(K_DIRECTION, value.name) }; emit(ChangeEvent.Layout) }

    var fitMode: FitMode
        get() = runCatching {
            FitMode.valueOf(store.getString(K_FIT, FitMode.FitWidth.name) ?: FitMode.FitWidth.name)
        }.getOrDefault(FitMode.FitWidth)
        set(value) { store.edit { putString(K_FIT, value.name) }; emit(ChangeEvent.Image) }

    var backgroundDark: Boolean
        get() = store.getBoolean(K_BG_DARK, true)
        set(value) { store.edit { putBoolean(K_BG_DARK, value) }; emit(ChangeEvent.Theme) }

    var useSystemBrightness: Boolean
        get() = store.getBoolean(K_SYS_BRIGHTNESS, true)
        set(value) { store.edit { putBoolean(K_SYS_BRIGHTNESS, value) }; emit(ChangeEvent.Brightness) }

    var customBrightness: Float
        get() = store.getFloat(K_BRIGHTNESS, 0.5f).coerceIn(0.01f, 1f)
        set(value) { store.edit { putFloat(K_BRIGHTNESS, value.coerceIn(0.01f, 1f)) }; emit(ChangeEvent.Brightness) }

    var warmFilterStrength: Float
        get() = store.getFloat(K_WARM_FILTER, 0f).coerceIn(0f, 0.6f)
        set(value) { store.edit { putFloat(K_WARM_FILTER, value.coerceIn(0f, 0.6f)) }; emit(ChangeEvent.Theme) }

    var keepScreenOn: Boolean
        get() = store.getBoolean(K_KEEP_SCREEN_ON, true)
        set(value) { store.edit { putBoolean(K_KEEP_SCREEN_ON, value) }; emit(ChangeEvent.Interaction) }

    var immersive: Boolean
        get() = store.getBoolean(K_IMMERSIVE, true)
        set(value) { store.edit { putBoolean(K_IMMERSIVE, value) }; emit(ChangeEvent.Interaction) }

    var tapZoneReversed: Boolean
        get() = store.getBoolean(K_TAP_REVERSED, false)
        set(value) { store.edit { putBoolean(K_TAP_REVERSED, value) }; emit(ChangeEvent.Interaction) }

    var volumeKeyFlip: Boolean
        get() = store.getBoolean(K_VOLUME_FLIP, true)
        set(value) { store.edit { putBoolean(K_VOLUME_FLIP, value) }; emit(ChangeEvent.Interaction) }

    var preloadAhead: Int
        get() = store.getInt(K_PRELOAD, 2).coerceIn(0, 8)
        set(value) { store.edit { putInt(K_PRELOAD, value.coerceIn(0, 8)) }; emit(ChangeEvent.Image) }

    var showPageNumber: Boolean
        get() = store.getBoolean(K_PAGE_NUM, true)
        set(value) { store.edit { putBoolean(K_PAGE_NUM, value) }; emit(ChangeEvent.Layout) }

    var loadOriginal: Boolean
        get() = store.getBoolean(K_LOAD_ORIGINAL, true)
        set(value) { store.edit { putBoolean(K_LOAD_ORIGINAL, value) }; emit(ChangeEvent.Image) }

    var flipAnim: FlipAnim
        get() = runCatching {
            FlipAnim.valueOf(store.getString(K_FLIP_ANIM, FlipAnim.Slide.name) ?: FlipAnim.Slide.name)
        }.getOrDefault(FlipAnim.Slide)
        set(value) { store.edit { putString(K_FLIP_ANIM, value.name) }; emit(ChangeEvent.Layout) }

    var doubleTapZoomLevel: Float
        get() = store.getFloat(K_DBLTAP_ZOOM, 2.5f).coerceIn(1.5f, 5f)
        set(value) { store.edit { putFloat(K_DBLTAP_ZOOM, value.coerceIn(1.5f, 5f)) }; emit(ChangeEvent.Image) }

    /** 漫画通常右翻页（RTL）；Pixiv 多页插画一般 LTR。提供一键切换。 */
    fun toggleDirection() {
        pageDirection = if (pageDirection == PageDirection.LTR) PageDirection.RTL else PageDirection.LTR
    }

    fun snapshot(): Snapshot = Snapshot(
        readingMode = readingMode,
        pageDirection = pageDirection,
        fitMode = fitMode,
        backgroundDark = backgroundDark,
        showPageNumber = showPageNumber,
        loadOriginal = loadOriginal,
    )

    data class Snapshot(
        val readingMode: ReadingMode,
        val pageDirection: PageDirection,
        val fitMode: FitMode,
        val backgroundDark: Boolean,
        val showPageNumber: Boolean,
        val loadOriginal: Boolean,
    )

    private const val K_MODE = "c_mode"
    private const val K_DIRECTION = "c_direction"
    private const val K_FIT = "c_fit"
    private const val K_BG_DARK = "c_bg_dark"
    private const val K_SYS_BRIGHTNESS = "c_sys_brightness"
    private const val K_BRIGHTNESS = "c_brightness"
    private const val K_KEEP_SCREEN_ON = "c_keep_screen_on"
    private const val K_IMMERSIVE = "c_immersive"
    private const val K_TAP_REVERSED = "c_tap_reversed"
    private const val K_VOLUME_FLIP = "c_volume_flip"
    private const val K_PRELOAD = "c_preload"
    private const val K_PAGE_NUM = "c_page_num"
    private const val K_LOAD_ORIGINAL = "c_load_original"
    private const val K_DBLTAP_ZOOM = "c_dbltap_zoom"
    private const val K_FLIP_ANIM = "c_flip_anim"
    private const val K_WARM_FILTER = "c_warm_filter"
}
