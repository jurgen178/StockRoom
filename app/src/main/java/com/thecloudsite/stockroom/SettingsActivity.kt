/*
 * Copyright (C) 2020
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

package com.thecloudsite.stockroom

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.main_tab_layout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

const val exportListActivityRequestCode = 3
//const val authActivityRequestCode = 4

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

          val exportJsonUri = data.data!!
          stockRoomViewModel.exportJSON(applicationContext, exportJsonUri)
          finish()
        }
      }
    }
    /*
    else
      if (requestCode == authActivityRequestCode) {
        val response = IdpResponse.fromResultIntent(data)

        if (resultCode == Activity.RESULT_OK) {
          // Successfully signed in
          val user = FirebaseAuth.getInstance().currentUser

          exportListToCloud()
          // ...
        } else {
          // Sign in failed. If response is null the user canceled the
          // sign-in flow using the back button. Otherwise check
          // response.getError().getErrorCode() and handle the error.
          // ...
        }
      }*/
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
            .setMessage(getString(R.string.displayed_views_dialog_message))
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.settings_menu, menu)
    return true
  }

  fun onSettings1(item: MenuItem) {
    AlertDialog.Builder(this)
        // https://convertcodes.com/unicode-converter-encode-decode-utf/
        .setTitle(
            "\u0059\u006f\u0075\u0020\u0073\u0068\u006f\u0075\u006c\u0064\u0020\u006d\u0061\u006b\u0065"
        )
        .setMessage(
            "\u0061\u0020\u0064\u0065\u0062\u0075\u0067\u0020\u0069\u006e\u0076\u0065\u0073\u0074\u006d\u0065\u006e\u0074\u002e"
        )
        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        .show()
//    val intent = Intent(this@SettingsActivity, ListActivity::class.java)
//    startActivity(intent)
  }

  fun onSettings2(item: MenuItem) {
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

      // Set version info.
      val versionCode = BuildConfig.VERSION_CODE
      //val versionName = BuildConfig.VERSION_NAME
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
          ) { bold { append(" $versionCode $versionBuild") } }
//          ) { bold { append(" \t\t$versionCode") } }
//          .append("\nVersion name")
//          .color(
//              context?.getColor(R.color.settingsblue)!!
//          ) { bold { append(" \t$versionName $versionBuild") } }
          .italic {
            scale(0.8f) {
              color(0xffffbb33.toInt()) {
                // https://convertcodes.com/unicode-converter-encode-decode-utf/
                append(
                    "\n\u006a\u0075\u0072\u0067\u0065\u006e\u0031\u0037\u0038\u002c\u0020\u0061\u0069\u0061\u006d\u0061\u006e\u002c\u0020\u006b\u0065\u006e\u0064\u0079\u002c\u0020\u0062\u006c\u0075\u006c\u0062\u0020\u0061\u006e\u0064\u0020\u0074\u0075\u006c\u0062\u0070\u0069\u0072"
                )
              }
            }
          }
      version?.summary = versionStr

      val titles: List<String> = listOf(
          // https://convertcodes.com/unicode-converter-encode-decode-utf/
          "\u0057\u0068\u0061\u0074\u0020\u0079\u006f\u0075\u0020\u0061\u0072\u0065\u0020\u006c\u006f\u006f\u006b\u0069\u006e\u0067\u0020\u0066\u006f\u0072",
          "\u004c\u006f\u006f\u006b",
          "\u0054\u0068\u0065\u0020\u0063\u0068\u0061\u0072\u0074",
          "\u004d\u0069\u006e\u0065"
      )

      val messages: List<String> = listOf(
          // https://convertcodes.com/unicode-converter-encode-decode-utf/
          "\u0069\u0073\u0020\u006e\u006f\u0074\u0020\u0068\u0065\u0072\u0065\u002e",
          "\u004e\u006f\u0072\u0074\u0068\u0020\u0062\u0079\u0020\u004e\u006f\u0072\u0074\u0068\u0065\u0061\u0073\u0074",
          "\u006e\u0065\u0065\u0064\u0073\u0020\u0061\u0020\u0073\u0065\u0063\u006f\u006e\u0064\u0020\u006c\u006f\u006f\u006b\u002e",
          "\u0074\u0068\u0065\u0020\u0064\u0065\u0074\u0061\u0069\u006c\u0020\u0064\u0061\u0074\u0061\u002e"
      )

      var versionClickCounter: Int = 0
      version?.onPreferenceClickListener =
        OnPreferenceClickListener {
          versionClickCounter++
          if (versionClickCounter % 10 == 0 && versionClickCounter <= 40) {
            val index = versionClickCounter / 10 - 1
            AlertDialog.Builder(requireContext())
                .setTitle(titles[index])
                .setMessage(messages[index])
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
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

      val buttonResetPortfolios: Preference? = findPreference("reset_portfolios")
      if (buttonResetPortfolios != null) {
        buttonResetPortfolios.onPreferenceClickListener =
          OnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_portfolios_title)
                .setMessage(getString(R.string.reset_portfolios_confirm))
                .setPositiveButton(R.string.reset) { _, _ ->
                  stockRoomViewModel.resetPortfolios()

                  // Leave settings activity.
                  activity?.onBackPressed()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()

            true
          }
      }

/*
      val buttonExportListToCloud: Preference? = findPreference("export_cloud")
      if (buttonExportListToCloud != null) {
        buttonExportListToCloud.onPreferenceClickListener =
          OnPreferenceClickListener {
            onExportListToCloud()
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
          .replace(":", "_")
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

    /*
    FirebaseUser user = firebaseAuth.getCurrentUser();
    if (user != null) {
      FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).setValue(true);
    }


    private fun onExportListToCloud() {
      val auth: FirebaseAuth = Firebase.auth

      // Check if user is signed in (non-null) and update UI accordingly.
      val currentUser = auth.currentUser

      if (currentUser != null) {
        val myRef2 = Firebase.database.getReference("message")
        myRef2.keepSynced(true)
        myRef2.setValue("Hello, World!1234")
            .addOnSuccessListener {
              // Write was successful!
              val a= it
              // ...
            }
            .addOnFailureListener {
              // Write failed
              val a= it
              // ...
            }
        Firebase.database.reference.setValue("abcd")

        // Read from the database
        myRef2.addListenerForSingleValueEvent(object : ValueEventListener {
          override fun onDataChange(dataSnapshot: DataSnapshot) {
            // This method is called once with the initial value and again
            // whenever data at this location is updated.
            val value = dataSnapshot.getValue<String>()
            val a = value
            //Log.d(TAG, "Value is: $value")
          }

          override fun onCancelled(error: DatabaseError) {
            // Failed to read value
            //Log.w(TAG, "Failed to read value.", error.toException())
          }
        })
        exportListToCloud()
      } else {

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder()
                .build(),
            AuthUI.IdpConfig.GoogleBuilder()
                .build()
            /*,
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.TwitterBuilder().build()
             */
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            authActivityRequestCode
        )
      }
    }
    */

  }
}