package email.schaal.ocreader

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import email.schaal.ocreader.database.Queries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Preference Fragment
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>("reset_database")?.apply {
            setOnPreferenceClickListener {
                Queries.resetDatabase()
                Glide.get(context).apply {
                    runBlocking(Dispatchers.IO) {
                        clearDiskCache()
                    }
                    clearMemory()
                }
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.key, true)
                        .apply()
                it.title = getString(R.string.database_was_reset)
                it.isEnabled = false
                true
            }
        }
    }
}