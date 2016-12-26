package email.schaal.ocreader;


import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Preference Fragment
 */
public class SettingsFragment extends PreferenceFragment {
    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
