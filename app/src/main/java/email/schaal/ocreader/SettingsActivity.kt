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
package email.schaal.ocreader

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import email.schaal.ocreader.Preferences.ChangeAction
import email.schaal.ocreader.databinding.ActivitySettingsBinding
import email.schaal.ocreader.util.FaviconLoader

class SettingsActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var recreateActivity: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(EXTRA_RECREATE_ACTIVITY)) recreateActivity = intent.getBooleanExtra(EXTRA_RECREATE_ACTIVITY, false)
        val binding = DataBindingUtil.setContentView<ActivitySettingsBinding>(this, R.layout.activity_settings)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)

        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_settings, SettingsFragment())
                .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_RECREATE_ACTIVITY, recreateActivity)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        recreateActivity = savedInstanceState.getBoolean(EXTRA_RECREATE_ACTIVITY)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Preferences.getPreference(key)?.let { preference ->
            when (preference.changeAction) {
                ChangeAction.NOTHING -> {
                }
                ChangeAction.RECREATE -> {
                    recreateActivity = true
                    FaviconLoader.clearCache()
                    AppCompatDelegate.setDefaultNightMode(Preferences.getNightMode(sharedPreferences))
                    val intent = intent
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(EXTRA_RECREATE_ACTIVITY, recreateActivity)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                ChangeAction.UPDATE -> PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.key, true).apply()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            checkRecreateActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkRecreateActivity(): Boolean {
        if (recreateActivity) {
            val intent = Intent(this, ListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        return recreateActivity
    }

    override fun onBackPressed() { // Recreate the parent activity if necessary to apply theme change, go back normally otherwise
        if (!checkRecreateActivity()) {
            super.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_RECREATE_ACTIVITY = "recreateActivity"
    }
}