package ceui.pixiv.ui.novel.reader.settings

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ceui.pixiv.ui.novel.reader.model.FlipMode
import ceui.pixiv.ui.novel.reader.model.ReadingDirection
import ceui.pixiv.ui.novel.reader.model.ImagePlacement
import ceui.pixiv.ui.novel.reader.model.ImageScaleMode
import ceui.pixiv.ui.novel.reader.model.ScreenOrientation
import androidx.core.content.edit

/**
 * Persistent reader settings. Backed by its own SharedPreferences instance so it stays
 * isolated from the rest of the app. All mutations publish to [changes] so
 * UI can react immediately and the paginator can re-layout.
 */
object ReaderSettings {

    private const val PREFS_NAME = "novel_reader_v3"

    private val store: SharedPreferences by lazy { ceui.lisa.activities.Shaft.getNamedPrefs(PREFS_NAME) }

    private val _changes = MutableLiveData<ChangeEvent>()
    val changes: LiveData<ChangeEvent> = _changes

    private fun emit(event: ChangeEvent) {
        _changes.postValue(event)
    }

    sealed class ChangeEvent {
        object Layout : ChangeEvent()
        object Theme : ChangeEvent()
        object Brightness : ChangeEvent()
        object Flip : ChangeEvent()
        object Tts : ChangeEvent()
        object Interaction : ChangeEvent()
        object Image : ChangeEvent()
        object Reminder : ChangeEvent()
    }

    // ---------- Typography ----------
    var fontSizeSp: Int
        get() = store.getInt(K_FONT_SIZE, 18).coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)
        set(value) {
            store.edit { putInt(K_FONT_SIZE, value.coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)) }
            emit(ChangeEvent.Layout)
        }

    var lineSpacing: Float
        get() = store.getFloat(K_LINE_SPACING, 1.6f).coerceIn(1.0f, 2.8f)
        set(value) {
            store.edit { putFloat(K_LINE_SPACING, value.coerceIn(1.0f, 2.8f)) }
            emit(ChangeEvent.Layout)
        }

    var paragraphSpacingLines: Float
        get() = store.getFloat(K_PARAGRAPH_SPACING, 0.8f).coerceIn(0f, 2.5f)
        set(value) {
            store.edit { putFloat(K_PARAGRAPH_SPACING, value.coerceIn(0f, 2.5f)) }
            emit(ChangeEvent.Layout)
        }

    var horizontalMarginDp: Int
        get() = store.getInt(K_H_MARGIN, 20).coerceIn(0, 64)
        set(value) {
            store.edit { putInt(K_H_MARGIN, value.coerceIn(0, 64)) }
            emit(ChangeEvent.Layout)
        }

    var verticalMarginDp: Int
        get() = store.getInt(K_V_MARGIN, 24).coerceIn(0, 96)
        set(value) {
            store.edit { putInt(K_V_MARGIN, value.coerceIn(0, 96)) }
            emit(ChangeEvent.Layout)
        }

    var firstLineIndent: Int
        get() = store.getInt(K_INDENT, 2).coerceIn(0, 4)
        set(value) {
            store.edit { putInt(K_INDENT, value.coerceIn(0, 4)) }
            emit(ChangeEvent.Layout)
        }

    var letterSpacing: Float
        get() = store.getFloat(K_LETTER_SPACING, 0f).coerceIn(-0.05f, 0.25f)
        set(value) {
            store.edit { putFloat(K_LETTER_SPACING, value.coerceIn(-0.05f, 0.25f)) }
            emit(ChangeEvent.Layout)
        }

    var boldText: Boolean
        get() = store.getBoolean(K_BOLD, false)
        set(value) {
            store.edit { putBoolean(K_BOLD, value) }
            emit(ChangeEvent.Layout)
        }

    var fontId: String
        get() = store.getString(K_FONT_ID, PresetFonts.SYSTEM.id) ?: PresetFonts.SYSTEM.id
        set(value) {
            store.edit { putString(K_FONT_ID, value) }
            emit(ChangeEvent.Layout)
        }

    var fontWeight: Int
        get() = store.getInt(K_FONT_WEIGHT, 400).coerceIn(100, 900)
        set(value) {
            store.edit { putInt(K_FONT_WEIGHT, value.coerceIn(100, 900)) }
            emit(ChangeEvent.Layout)
        }

    // ---------- Theme ----------
    var themeId: String
        get() = store.getString(K_THEME_ID, ReaderTheme.KRAFT.id) ?: ReaderTheme.KRAFT.id
        set(value) {
            store.edit { putString(K_THEME_ID, value) }
            emit(ChangeEvent.Theme)
        }

    var customThemeId: Int
        get() = store.getInt(K_CUSTOM_THEME_ID, 0)
        set(value) {
            store.edit { putInt(K_CUSTOM_THEME_ID, value) }
            emit(ChangeEvent.Theme)
        }

    var followSystemDarkMode: Boolean
        get() = store.getBoolean(K_FOLLOW_DARK, false)
        set(value) {
            store.edit { putBoolean(K_FOLLOW_DARK, value) }
            emit(ChangeEvent.Theme)
        }

    var backgroundImagePath: String?
        get() = store.getString(K_BG_IMAGE, null)
        set(value) {
            if (value == null) store.edit { remove(K_BG_IMAGE) } else store.edit { putString(K_BG_IMAGE, value) }
            emit(ChangeEvent.Theme)
        }

    // ---------- Brightness ----------
    var useSystemBrightness: Boolean
        get() = store.getBoolean(K_SYS_BRIGHTNESS, true)
        set(value) {
            store.edit { putBoolean(K_SYS_BRIGHTNESS, value) }
            emit(ChangeEvent.Brightness)
        }

    var customBrightness: Float
        get() = store.getFloat(K_BRIGHTNESS, 0.5f).coerceIn(0.01f, 1f)
        set(value) {
            store.edit { putFloat(K_BRIGHTNESS, value.coerceIn(0.01f, 1f)) }
            emit(ChangeEvent.Brightness)
        }

    var warmFilterStrength: Float
        get() = store.getFloat(K_WARM_FILTER, 0f).coerceIn(0f, 0.6f)
        set(value) {
            store.edit { putFloat(K_WARM_FILTER, value.coerceIn(0f, 0.6f)) }
            emit(ChangeEvent.Brightness)
        }

    // ---------- Reading direction + Flip ----------
    var readingDirection: ReadingDirection
        get() = runCatching {
            ReadingDirection.valueOf(store.getString(K_READING_DIR, ReadingDirection.Horizontal.name) ?: ReadingDirection.Horizontal.name)
        }.getOrDefault(ReadingDirection.Horizontal)
        set(value) {
            store.edit { putString(K_READING_DIR, value.name) }
            emit(ChangeEvent.Flip)
        }

    var flipMode: FlipMode
        get() = runCatching {
            FlipMode.valueOf(store.getString(K_FLIP_MODE, FlipMode.Simulation.name) ?: FlipMode.Simulation.name)
        }.getOrDefault(FlipMode.Simulation)
        set(value) {
            store.edit { putString(K_FLIP_MODE, value.name) }
            emit(ChangeEvent.Flip)
        }

    var volumeKeyFlip: Boolean
        get() = store.getBoolean(K_VOLUME_FLIP, true)
        set(value) {
            store.edit { putBoolean(K_VOLUME_FLIP, value) }
            emit(ChangeEvent.Interaction)
        }

    var tapZoneReversed: Boolean
        get() = store.getBoolean(K_TAP_REVERSED, false)
        set(value) {
            store.edit { putBoolean(K_TAP_REVERSED, value) }
            emit(ChangeEvent.Interaction)
        }

    var autoPageIntervalSec: Int
        get() = store.getInt(K_AUTO_PAGE_INTERVAL, 15).coerceIn(5, 60)
        set(value) {
            store.edit { putInt(K_AUTO_PAGE_INTERVAL, value.coerceIn(5, 60)) }
            emit(ChangeEvent.Interaction)
        }

    var screenOrientation: ScreenOrientation
        get() = runCatching {
            ScreenOrientation.valueOf(store.getString(K_ORIENTATION, ScreenOrientation.Auto.name) ?: ScreenOrientation.Auto.name)
        }.getOrDefault(ScreenOrientation.Auto)
        set(value) {
            store.edit { putString(K_ORIENTATION, value.name) }
            emit(ChangeEvent.Interaction)
        }

    var immersive: Boolean
        get() = store.getBoolean(K_IMMERSIVE, true)
        set(value) {
            store.edit { putBoolean(K_IMMERSIVE, value) }
            emit(ChangeEvent.Interaction)
        }

    var keepScreenOn: Boolean
        get() = store.getBoolean(K_KEEP_SCREEN_ON, true)
        set(value) {
            store.edit { putBoolean(K_KEEP_SCREEN_ON, value) }
            emit(ChangeEvent.Interaction)
        }

    var showTopProgress: Boolean
        get() = store.getBoolean(K_SHOW_TOP_PROGRESS, true)
        set(value) {
            store.edit { putBoolean(K_SHOW_TOP_PROGRESS, value) }
            emit(ChangeEvent.Layout)
        }

    var showBottomProgress: Boolean
        get() = store.getBoolean(K_SHOW_BOTTOM_PROGRESS, true)
        set(value) {
            store.edit { putBoolean(K_SHOW_BOTTOM_PROGRESS, value) }
            emit(ChangeEvent.Layout)
        }

    // ---------- Image ----------
    var imagePlacement: ImagePlacement
        get() = runCatching {
            ImagePlacement.valueOf(store.getString(K_IMG_PLACEMENT, ImagePlacement.Center.name) ?: ImagePlacement.Center.name)
        }.getOrDefault(ImagePlacement.Center)
        set(value) {
            store.edit { putString(K_IMG_PLACEMENT, value.name) }
            emit(ChangeEvent.Image)
        }

    var imageScaleMode: ImageScaleMode
        get() = runCatching {
            ImageScaleMode.valueOf(store.getString(K_IMG_SCALE, ImageScaleMode.Fit.name) ?: ImageScaleMode.Fit.name)
        }.getOrDefault(ImageScaleMode.Fit)
        set(value) {
            store.edit { putString(K_IMG_SCALE, value.name) }
            emit(ChangeEvent.Image)
        }

    var preloadImageAhead: Int
        get() = store.getInt(K_PRELOAD_AHEAD, 2).coerceIn(0, 8)
        set(value) {
            store.edit { putInt(K_PRELOAD_AHEAD, value.coerceIn(0, 8)) }
            emit(ChangeEvent.Image)
        }

    // ---------- TTS ----------
    var ttsSpeed: Float
        get() = store.getFloat(K_TTS_SPEED, 1f).coerceIn(0.5f, 2.0f)
        set(value) {
            store.edit { putFloat(K_TTS_SPEED, value.coerceIn(0.5f, 2.0f)) }
            emit(ChangeEvent.Tts)
        }

    var ttsPitch: Float
        get() = store.getFloat(K_TTS_PITCH, 1f).coerceIn(0.5f, 2.0f)
        set(value) {
            store.edit { putFloat(K_TTS_PITCH, value.coerceIn(0.5f, 2.0f)) }
            emit(ChangeEvent.Tts)
        }

    var ttsEngine: String?
        get() = store.getString(K_TTS_ENGINE, null)
        set(value) {
            if (value == null) store.edit { remove(K_TTS_ENGINE) } else store.edit { putString(K_TTS_ENGINE, value) }
            emit(ChangeEvent.Tts)
        }

    var ttsVoice: String?
        get() = store.getString(K_TTS_VOICE, null)
        set(value) {
            if (value == null) store.edit { remove(K_TTS_VOICE) } else store.edit { putString(K_TTS_VOICE, value) }
            emit(ChangeEvent.Tts)
        }

    var ttsSleepTimerMinutes: Int
        get() = store.getInt(K_TTS_SLEEP, 0)
        set(value) {
            store.edit { putInt(K_TTS_SLEEP, value) }
            emit(ChangeEvent.Tts)
        }

    // ---------- Misc ----------
    var eyeBreakReminderMinutes: Int
        get() = store.getInt(K_EYE_REMIND, 30)
        set(value) {
            store.edit { putInt(K_EYE_REMIND, value) }
            emit(ChangeEvent.Reminder)
        }

    var touchLocked: Boolean
        get() = store.getBoolean(K_TOUCH_LOCKED, false)
        set(value) {
            store.edit { putBoolean(K_TOUCH_LOCKED, value) }
            emit(ChangeEvent.Interaction)
        }

    var showDebugOverlay: Boolean
        get() = store.getBoolean(K_DEBUG_OVERLAY, false)
        set(value) {
            store.edit { putBoolean(K_DEBUG_OVERLAY, value) }
            emit(ChangeEvent.Layout)
        }

    var readingSpeedCharPerMin: Int
        get() = store.getInt(K_READING_SPEED, 400).coerceIn(50, 1500)
        set(value) {
            store.edit { putInt(K_READING_SPEED, value.coerceIn(50, 1500)) }
        }

    /** Emit a synthetic change event so observers can force a refresh. */
    fun notifyLayoutChanged() = emit(ChangeEvent.Layout)

    fun snapshot(): Snapshot = Snapshot(
        fontSizeSp = fontSizeSp,
        lineSpacing = lineSpacing,
        paragraphSpacingLines = paragraphSpacingLines,
        horizontalMarginDp = horizontalMarginDp,
        verticalMarginDp = verticalMarginDp,
        firstLineIndent = firstLineIndent,
        letterSpacing = letterSpacing,
        boldText = boldText,
        fontId = fontId,
        fontWeight = fontWeight,
        themeId = themeId,
        customThemeId = customThemeId,
        followSystemDarkMode = followSystemDarkMode,
        backgroundImagePath = backgroundImagePath,
        flipMode = flipMode,
        imagePlacement = imagePlacement,
        imageScaleMode = imageScaleMode,
    )

    data class Snapshot(
        val fontSizeSp: Int,
        val lineSpacing: Float,
        val paragraphSpacingLines: Float,
        val horizontalMarginDp: Int,
        val verticalMarginDp: Int,
        val firstLineIndent: Int,
        val letterSpacing: Float,
        val boldText: Boolean,
        val fontId: String,
        val fontWeight: Int,
        val themeId: String,
        val customThemeId: Int,
        val followSystemDarkMode: Boolean,
        val backgroundImagePath: String?,
        val flipMode: FlipMode,
        val imagePlacement: ImagePlacement,
        val imageScaleMode: ImageScaleMode,
    )

    const val FONT_SIZE_MIN = 12
    const val FONT_SIZE_MAX = 36

    private const val K_FONT_SIZE = "r_font_size"
    private const val K_LINE_SPACING = "r_line_spacing"
    private const val K_PARAGRAPH_SPACING = "r_paragraph_spacing"
    private const val K_H_MARGIN = "r_h_margin"
    private const val K_V_MARGIN = "r_v_margin"
    private const val K_INDENT = "r_indent"
    private const val K_LETTER_SPACING = "r_letter_spacing"
    private const val K_BOLD = "r_bold"
    private const val K_FONT_ID = "r_font_id"
    private const val K_FONT_WEIGHT = "r_font_weight"
    private const val K_THEME_ID = "r_theme_id"
    private const val K_CUSTOM_THEME_ID = "r_custom_theme_id"
    private const val K_FOLLOW_DARK = "r_follow_dark"
    private const val K_BG_IMAGE = "r_bg_image"
    private const val K_SYS_BRIGHTNESS = "r_sys_brightness"
    private const val K_BRIGHTNESS = "r_brightness"
    private const val K_WARM_FILTER = "r_warm_filter"
    private const val K_READING_DIR = "r_reading_direction"
    private const val K_FLIP_MODE = "r_flip_mode"
    private const val K_VOLUME_FLIP = "r_volume_flip"
    private const val K_TAP_REVERSED = "r_tap_reversed"
    private const val K_AUTO_PAGE_INTERVAL = "r_auto_page_interval"
    private const val K_ORIENTATION = "r_orientation"
    private const val K_IMMERSIVE = "r_immersive"
    private const val K_KEEP_SCREEN_ON = "r_keep_screen_on"
    private const val K_SHOW_TOP_PROGRESS = "r_show_top_progress"
    private const val K_SHOW_BOTTOM_PROGRESS = "r_show_bottom_progress"
    private const val K_IMG_PLACEMENT = "r_img_placement"
    private const val K_IMG_SCALE = "r_img_scale"
    private const val K_PRELOAD_AHEAD = "r_preload_ahead"
    private const val K_TTS_SPEED = "r_tts_speed"
    private const val K_TTS_PITCH = "r_tts_pitch"
    private const val K_TTS_ENGINE = "r_tts_engine"
    private const val K_TTS_VOICE = "r_tts_voice"
    private const val K_TTS_SLEEP = "r_tts_sleep"
    private const val K_EYE_REMIND = "r_eye_remind"
    private const val K_TOUCH_LOCKED = "r_touch_locked"
    private const val K_DEBUG_OVERLAY = "r_debug_overlay"
    private const val K_READING_SPEED = "r_reading_speed"
}
