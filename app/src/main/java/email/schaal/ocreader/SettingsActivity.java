/*
 * Copyright © 2017. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;

import email.schaal.ocreader.databinding.ActivitySettingsBinding;
import email.schaal.ocreader.util.FaviconLoader;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean recreateActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra("recreateActivity"))
            recreateActivity = getIntent().getBooleanExtra("recreateActivity", false);

        ActivitySettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("recreateActivity", recreateActivity);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recreateActivity = savedInstanceState.getBoolean("recreateActivity");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Preferences.DARK_THEME.getKey())) {
            recreateActivity = true;
            FaviconLoader.clearCache();
            AppCompatDelegate.setDefaultNightMode(Preferences.getNightMode(sharedPreferences));
            Intent intent = getIntent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("recreateActivity", recreateActivity);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else if(key.equals(Preferences.ORDER.getKey()) || key.equals(Preferences.SHOW_ONLY_UNREAD.getKey()) || key.equals(Preferences.SORT_FIELD.getKey())) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), true).apply();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            checkRecreateActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkRecreateActivity() {
        if(recreateActivity) {
            final Intent intent = new Intent(this, ListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        return recreateActivity;
    }

    @Override
    public void onBackPressed() {
        // Recreate the parent activity if necessary to apply theme change, go back normally otherwise
        if(!checkRecreateActivity()) {
            super.onBackPressed();
        }
    }
}
