package email.schaal.ocreader;


import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Preference Fragment
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
