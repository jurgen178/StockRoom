package com.example.android.stockroom

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var stockRoomViewModel: StockRoomViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings, SettingsFragment())
        .commit()
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Settings activity started.")
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
    if (key == "postmarket") {
      SharedRepository.postMarket = sharedPreferences.getBoolean(key, true)
    }
  }

  class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var stockRoomViewModel: StockRoomViewModel

    override fun onCreatePreferences(
      savedInstanceState: Bundle?,
      rootKey: String?
    ) {
      setPreferencesFromResource(R.xml.root_preferences, rootKey)
      stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
      stockRoomViewModel.logDebug("Settings fragment started.")

/*
      val buttonExportList: Preference? = findPreference("export_list")
      if (buttonExportList != null) {
        buttonExportList.onPreferenceClickListener =
          OnPreferenceClickListener {
            onExportList()
            true
          }
      }
      */

      val buttonDeleteAll: Preference? = findPreference("delete_all")
      if (buttonDeleteAll != null) {
        buttonDeleteAll.onPreferenceClickListener =
          OnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_all_title)
                .setMessage(getString(R.string.delete_all_confirm))
                .setPositiveButton(R.string.delete) { _, _ ->
                  stockRoomViewModel.deleteAll()

                  // Leave settings activity.
                  activity?.onBackPressed()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()

            true
          }
      }

      val buttonUpdateGroups: Preference? = findPreference("update_groups")
      if (buttonUpdateGroups != null) {
        buttonUpdateGroups.onPreferenceClickListener =
          OnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_groups_title)
                .setMessage(getString(R.string.delete_all_confirm))
                .setPositiveButton(R.string.delete) { _, _ ->
                  // Leave settings activity.
                  activity?.onBackPressed()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()

            true
          }
      }

      val buttonFeedback: Preference? = findPreference("feedback")
      if (buttonFeedback != null) {
        buttonFeedback.onPreferenceClickListener =
          OnPreferenceClickListener {
            true
          }
      }
    }
  }
}