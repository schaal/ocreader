package email.schaal.ocreader

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

/**
 * Preference Fragment
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}