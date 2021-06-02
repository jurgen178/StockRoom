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

package com.thecloudsite.stockroom

import android.R.layout
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.core.view.isEmpty
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.calc.CalcActivity
import com.thecloudsite.stockroom.databinding.ActivitySettingsBinding
import com.thecloudsite.stockroom.databinding.DialogRenameAccountBinding
import com.thecloudsite.stockroom.databinding.DialogRenameSymbolBinding
import com.thecloudsite.stockroom.utils.setAppTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.util.Locale

const val settingChartOverlaySymbolsDefault = "^GSPC,^IXIC"

private lateinit var exportRequest: ActivityResultLauncher<String>

class SettingsActivity : AppCompatActivity(),
  SharedPreferences.OnSharedPreferenceChangeListener {

  private lateinit var binding: ActivitySettingsBinding
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var stockRoomViewModel: StockRoomViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivitySettingsBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

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

    exportRequest =
      registerForActivityResult(ActivityResultContracts.CreateDocument())
      { uri ->
        stockRoomViewModel.exportJSON(applicationContext, uri)
        finish()
      }
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
        //delegate.applyDayNight()
      }
      "chart_overlay_symbols" -> {
        // if empty reset entry to default
        val symbols = sharedPreferences.getString(key, settingChartOverlaySymbolsDefault)
        if (symbols.isNullOrEmpty()) {
          sharedPreferences.edit()
            .putString(key, settingChartOverlaySymbolsDefault)
            .apply()
        }
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
        "\u004d\u0069\u006e\u0065",
        "\u004f\u0062\u0073\u0065\u0072\u0076\u0065"
      )

      val messages: List<String> = listOf(
        // https://convertcodes.com/unicode-converter-encode-decode-utf/
        "\u0069\u0073\u0020\u006e\u006f\u0074\u0020\u0068\u0065\u0072\u0065\u002e",
        "\u004e\u006f\u0072\u0074\u0068\u0020\u0062\u0079\u0020\u004e\u006f\u0072\u0074\u0068\u0065\u0061\u0073\u0074",
        "\u006e\u0065\u0065\u0064\u0073\u0020\u0061\u0020\u0073\u0065\u0063\u006f\u006e\u0064\u0020\u006c\u006f\u006f\u006b\u002e",
        "\u0074\u0068\u0065\u0020\u0064\u0065\u0074\u0061\u0069\u006c\u0020\u0064\u0061\u0074\u0061\u002e",
        "\u0074\u0068\u0065\u0020\u0073\u0065\u0063\u0072\u0065\u0074\u0020\u006d\u0065\u0073\u0073\u0061\u0067\u0065\u0020\u0069\u006e\u0020\u0074\u0068\u0065\u0020\u0041\u0049\u002e"
      )

      var versionClickCounter: Int = 0
      version?.onPreferenceClickListener =
        OnPreferenceClickListener {
          versionClickCounter++
          if (versionClickCounter % 10 == 0 && versionClickCounter <= messages.size * 10) {
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

      val buttonRenameSymbol: Preference? = findPreference("rename_symbol")
      if (buttonRenameSymbol != null) {
        buttonRenameSymbol.onPreferenceClickListener =
          OnPreferenceClickListener {

            val builder = android.app.AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())
            val dialogBinding = DialogRenameSymbolBinding.inflate(inflater)

            stockRoomViewModel.allStockDBdata.observe(this, Observer { items ->
              if (items != null) {
                val spinnerData = items.map { stockItem ->
                  stockItem.symbol
                }
                  .sorted()

                dialogBinding.textViewSymbolSpinner.adapter =
                  ArrayAdapter(requireContext(), layout.simple_list_item_1, spinnerData)
              }
            })

            builder.setView(dialogBinding.root)
              .setTitle(R.string.rename_symbol_title)
              // Add action buttons
              .setPositiveButton(R.string.rename) { _, _ ->

                if (dialogBinding.textViewSymbolSpinner.isEmpty()) {
                  Toast.makeText(
                    requireContext(),
                    getString(R.string.no_symbols_available),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                val symbolOld = dialogBinding.textViewSymbolSpinner.selectedItem.toString()
                // Add () to avoid cast exception.
                val symbolNew = (dialogBinding.symbolNew.text).toString()
                  .trim()
                  .uppercase(Locale.ROOT)
                if (symbolNew.isEmpty() || symbolNew.contains(" ")) {
                  Toast.makeText(
                    requireContext(),
                    getString(R.string.symbol_name_not_valid),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                // Sync method to get the return value
                val renamed = stockRoomViewModel.renameSymbolSync(symbolOld, symbolNew)

                Toast.makeText(
                  requireContext(),

                  if (renamed) {
                    getString(R.string.symbol_renamed, symbolOld, symbolNew)
                  } else {
                    getString(R.string.symbol_not_renamed, symbolOld, symbolNew)
                  },

                  Toast.LENGTH_LONG
                )
                  .show()
              }
              .setNegativeButton(
                R.string.cancel
              ) { _, _ ->
              }
            builder
              .create()
              .show()

            true
          }
      }

      val buttonRenameAccount: Preference? = findPreference("rename_account")
      if (buttonRenameAccount != null) {
        buttonRenameAccount.onPreferenceClickListener =
          OnPreferenceClickListener {

            val builder = android.app.AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())
            val dialogBinding = DialogRenameAccountBinding.inflate(inflater)
            val standardAccount = getString(R.string.standard_account)

            // Default (empty) account cannot be renamed.
            val accounts = SharedAccountList.accounts.filter { account ->
              account.isNotEmpty()
            }
            dialogBinding.textViewAccountSpinner.adapter =
              ArrayAdapter(
                requireContext(),
                layout.simple_list_item_1,
                accounts
              )

            builder.setView(dialogBinding.root)
              .setTitle(getString(R.string.rename_account))
              // Add action buttons
              .setPositiveButton(R.string.rename) { _, _ ->
                if (dialogBinding.textViewAccountSpinner.isEmpty()) {
                  Toast.makeText(
                    requireContext(),
                    getString(R.string.no_accounts_available),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                // Add () to avoid cast exception.
                val accountText = (dialogBinding.accountNew.text).toString()
                  .trim()
                if (accountText.isEmpty() || accountText.compareTo(
                    standardAccount, true
                  ) == 0
                ) {
                  Toast.makeText(
                    requireContext(), getString(R.string.account_name_not_empty),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                val isUsed = SharedAccountList.accounts.find { account ->
                  account.equals(accountText, true)
                }
                if (isUsed != null) {
                  Toast.makeText(
                    requireContext(), getString(R.string.account_name_used, accountText),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                val accountOld = dialogBinding.textViewAccountSpinner.selectedItem.toString()
                stockRoomViewModel.updateAccount(accountOld, accountText)
              }
              .setNegativeButton(
                R.string.cancel
              ) { _, _ ->
              }
            builder
              .create()
              .show()

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

      val buttonCalc: Preference? = findPreference("calc")
      if (buttonCalc != null) {
        buttonCalc.onPreferenceClickListener =
          OnPreferenceClickListener {
            val intent = Intent(context, CalcActivity::class.java)
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
      val date = ZonedDateTime.now()
        .format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
        .replace(":", "_")
      val jsonFileName = context?.getString(R.string.json_default_filename, date) + ".json"
      exportRequest.launch(jsonFileName)
//      val intent = Intent()
//        .setType("application/json")
//        .setAction(Intent.ACTION_CREATE_DOCUMENT)
//        .addCategory(Intent.CATEGORY_OPENABLE)
//        .putExtra(Intent.EXTRA_TITLE, jsonFileName)
//      startActivityForResult(
//        Intent.createChooser(intent, getString(R.string.export_select_file)),
//        exportListActivityRequestCode
//      )
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