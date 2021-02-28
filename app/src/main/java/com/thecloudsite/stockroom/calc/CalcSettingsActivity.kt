/*
 * Copyright (C) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.stockroom.calc

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.xml
import com.thecloudsite.stockroom.SharedRepository
import com.thecloudsite.stockroom.databinding.ActivityCalcSettingsBinding
import com.thecloudsite.stockroom.utils.setAppTheme

class CalcSettingsActivity : AppCompatActivity(),
  SharedPreferences.OnSharedPreferenceChangeListener {

  private lateinit var binding: ActivityCalcSettingsBinding
  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityCalcSettingsBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportFragmentManager
      .beginTransaction()
      .replace(id.calc_settings, CalcSettingsFragment())
      .commit()
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onResume() {
    super.onResume()
    sharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override fun onPause() {
    super.onPause()
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override fun onSharedPreferenceChanged(
    sharedPreferences: SharedPreferences,
    key: String
  ) {
    when (key) {
      "postmarket" -> {
        SharedRepository.postMarket = sharedPreferences.getBoolean(key, true)
      }
      "notifications" -> {
        SharedRepository.notifications = sharedPreferences.getBoolean(key, true)
      }
      "displayed_views" -> {
        AlertDialog.Builder(this)
          .setTitle(getString(R.string.displayed_views_dialog_title))
          .setMessage(getString(R.string.app_needs_restart_message))
          .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
          .show()
      }
      "app_theme" -> {
        setAppTheme(this)
        delegate.applyDayNight()
      }
    }
  }

  class CalcSettingsFragment : PreferenceFragmentCompat() {

    companion object {
      fun newInstance() = CalcSettingsFragment()
    }

    override fun onCreatePreferences(
      savedInstanceState: Bundle?,
      rootKey: String?
    ) {
      setPreferencesFromResource(xml.calc_preferences, rootKey)
    }
  }
}