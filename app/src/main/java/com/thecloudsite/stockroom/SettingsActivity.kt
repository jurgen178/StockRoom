package com.thecloudsite.stockroom

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TypefaceSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

const val exportListActivityRequestCode = 3

class SettingsActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var stockRoomViewModel: StockRoomViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

/*
    settingsViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            SettingsFragment.newInstance()
          }
          else -> {
            DebugFragment.newInstance()
          }
        }
      }

      override fun getItemCount(): Int {
        return 2
      }
    }
*/

    supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings, SettingsFragment())
        .commit()
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Settings activity started.")

    // Setup observer to enable valid data for the export function.
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let {
      }
    })
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)

    val resultCodeShort = requestCode.toShort()
        .toInt()
    if (resultCode == Activity.RESULT_OK) {
      if (resultCodeShort == exportListActivityRequestCode) {
        if (data != null && data.data is Uri) {

          val exportListUri = data.data!!
          stockRoomViewModel.exportList(applicationContext, exportListUri)
          finish()
        }
      }
    }
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
    if (key == "notifications") {
      SharedRepository.notifications = sharedPreferences.getBoolean(key, true)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.settings_menu, menu)
    return true
  }

  fun onSettings(item: MenuItem) {
    val intent = Intent(this@SettingsActivity, ListActivity::class.java)
    startActivity(intent)
  }

  class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var stockRoomViewModel: StockRoomViewModel

    companion object {
      fun newInstance() = SettingsFragment()
    }

    override fun onCreatePreferences(
      savedInstanceState: Bundle?,
      rootKey: String?
    ) {
      setPreferencesFromResource(R.xml.root_preferences, rootKey)
      stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
      stockRoomViewModel.logDebug("Settings fragment started.")

      // Set version info.
      val versionCode = BuildConfig.VERSION_CODE
      val versionName = BuildConfig.VERSION_NAME
      val versionBuild = if (BuildConfig.DEBUG) {
        "(Debug)"
      } else {
        ""
      }
      val version: Preference? = findPreference("version")
      val versionStr = SpannableStringBuilder()
          .append("Version code")
          .color(
              context?.getColor(R.color.settingsblue)!!
          ) { bold { append(" \t\t$versionCode\n") } }
          .append("Version name")
          .color(
              context?.getColor(R.color.settingsblue)!!
          ) { bold { append(" \t$versionName $versionBuild") } }
      version?.summary = versionStr

      var versionClickCounter: Int = 1
      version?.onPreferenceClickListener=
        OnPreferenceClickListener {
          versionClickCounter++
          if(versionClickCounter % 10 == 0 && versionClickCounter % 20 != 0)
          {
            AlertDialog.Builder(requireContext())
                .setTitle("What you are looking for,")
                .setMessage("is not here.")
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
          }
          if(versionClickCounter % 20 == 0)
          {
            AlertDialog.Builder(requireContext())
                .setTitle("Look at")
                .setMessage("North by Northeast")
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
          }
          true
        }

      val buttonExportList: Preference? = findPreference("export_list")
      if (buttonExportList != null) {
        buttonExportList.onPreferenceClickListener =
          OnPreferenceClickListener {
            onExportList()
            true
          }
      }

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
            val intent = Intent(context, UpdateGroupActivity::class.java)
            startActivity(intent)
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

    private fun onExportList() {
      // Set default filename.
      val date = LocalDateTime.now()
          .format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
      val jsonFileName = context?.getString(R.string.json_default_filename, date)
      val intent = Intent()
          .setType("application/json")
          .setAction(Intent.ACTION_CREATE_DOCUMENT)
          .addCategory(Intent.CATEGORY_OPENABLE)
          .putExtra(Intent.EXTRA_TITLE, jsonFileName)
      startActivityForResult(
          Intent.createChooser(intent, getString(R.string.export_select_file)),
          exportListActivityRequestCode
      )
    }
  }
}