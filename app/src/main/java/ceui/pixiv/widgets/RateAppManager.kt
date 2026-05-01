package ceui.pixiv.widgets

import ceui.lisa.activities.Shaft
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages the timing and display logic for the Play Store rating dialog.
 *
 * Strategy:
 * - Track cumulative bookmark + follow operations
 * - When total reaches [ENGAGEMENT_THRESHOLD], auto-show the dialog once
 * - After auto-shown once: never auto-show again
 * - Manual trigger from About page is unaffected
 */
object RateAppManager {

    private const val KEY_ENGAGEMENT_COUNT = "rate_engagement_count"
    private const val KEY_AUTO_SHOWN = "rate_auto_shown"

    private const val ENGAGEMENT_THRESHOLD = 50

    private val prefs: SharedPreferences get() = Shaft.getDefaultPrefs()

    /**
     * Call this after a successful bookmark-add or follow-add operation.
     */
    fun onUserEngaged() {
        val count = prefs.getInt(KEY_ENGAGEMENT_COUNT, 0)
        prefs.edit { putInt(KEY_ENGAGEMENT_COUNT, count + 1) }
    }

    /**
     * Returns true if we should auto-show the rating dialog.
     */
    fun shouldShowRateDialog(): Boolean {
        if (prefs.getBoolean(KEY_AUTO_SHOWN, false)) return false

        val count = prefs.getInt(KEY_ENGAGEMENT_COUNT, 0)
        return count >= ENGAGEMENT_THRESHOLD
    }

    /**
     * Call this right after auto-showing the dialog, so it never auto-shows again.
     */
    fun onAutoShown() {
        prefs.edit { putBoolean(KEY_AUTO_SHOWN, true) }
    }
}
