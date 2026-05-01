package ceui.pixiv.login

import android.content.SharedPreferences
import androidx.core.content.edit
import ceui.lisa.activities.Shaft

class PrefsVerifierStore(
    private val prefs: SharedPreferences = Shaft.getDefaultPrefs(),
) : VerifierStore {

    override fun save(verifier: String) {
        prefs.edit { putString(KEY, verifier) }
    }

    override fun load(): String? = prefs.getString(KEY, null)

    override fun clear() {
        prefs.edit { remove(KEY) }
    }

    companion object {
        private const val KEY = "pixiv_oauth_pkce_verifier"
    }
}
