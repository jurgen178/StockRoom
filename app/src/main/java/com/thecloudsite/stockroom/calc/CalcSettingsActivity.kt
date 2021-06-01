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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.xml
import com.thecloudsite.stockroom.databinding.ActivityCalcSettingsBinding
import com.thecloudsite.stockroom.utils.saveTextToFile
import java.io.BufferedReader
import java.io.InputStreamReader

private lateinit var loadRequest: ActivityResultLauncher<String>
private lateinit var saveRequest: ActivityResultLauncher<String>

// https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument
class GetJsonContent : ActivityResultContracts.GetContent() {
  override fun createIntent(context: Context, input: String): Intent {
    val mimeTypes = arrayOf(

      // .json
      "application/json",
      "text/x-json",

      )

// Intent.createChooser(intent, getString(R.string.import_select_file)),

    return super.createIntent(context, input)
      .setType("*/*")
      .setAction(Intent.ACTION_OPEN_DOCUMENT)
      .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
  }
}

class CreateJsonDocument : ActivityResultContracts.CreateDocument() {
  override fun createIntent(context: Context, input: String): Intent {
    return super.createIntent(context, input)
      .setType("application/json")
      .setAction(Intent.ACTION_CREATE_DOCUMENT)
      .addCategory(Intent.CATEGORY_OPENABLE)
      .putExtra(Intent.EXTRA_TITLE, input)
  }
}

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

    loadRequest =
      registerForActivityResult(GetJsonContent())
      { uri ->
        loadCode(this, uri)
        finish()
      }

    saveRequest =
      registerForActivityResult(CreateJsonDocument())
      { uri ->
        saveCode(this, uri)
        finish()
      }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  private fun loadCode(
    context: Context,
    uri: Uri
  ) {
    try {
      context.contentResolver.openInputStream(uri)
        ?.use { inputStream ->
          BufferedReader(InputStreamReader(inputStream)).use { reader ->
            val text: String = reader.readText()

            // https://developer.android.com/training/secure-file-sharing/retrieve-info

            when (val type = context.contentResolver.getType(uri)) {
              "application/json", "text/x-json" -> {

                val sharedPreferences =
                  PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

                sharedPreferences
                  .edit()
                  .putString("calcCodeMap", text)
                  .apply()

                val msg = application.getString(
                  R.string.load_code_msg
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG)
                  .show()
              }
              else -> {
                val msg = application.getString(
                  R.string.import_mimetype_error, type
                )
                throw IllegalArgumentException(msg)
              }
            }
          }
        }
    } catch (e: Exception) {
      Toast.makeText(
        context, application.getString(R.string.import_error, e.message),
        Toast.LENGTH_LONG
      )
        .show()
    }
  }

  private fun saveCode(
    context: Context,
    exportJsonUri: Uri
  ) {
    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    val jsonString = sharedPreferences.getString("calcCodeMap", "").toString()
    val msg = application.getString(
      R.string.save_code_msg
    )

    saveTextToFile(jsonString, msg, context, exportJsonUri)
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

      val buttonExportCode: Preference? = findPreference("export_code")
      if (buttonExportCode != null) {
        buttonExportCode.onPreferenceClickListener =
          Preference.OnPreferenceClickListener {
            onExportCode()
            true
          }
      }

      val buttonImportCode: Preference? = findPreference("import_code")
      if (buttonImportCode != null) {
        buttonImportCode.onPreferenceClickListener =
          Preference.OnPreferenceClickListener {
            onImportCode()
            true
          }
      }
    }

    private fun onImportCode() {

      loadRequest.launch("")
    }

    private fun onExportCode() {
      // Set default filename.
      val jsonFileName = "AI-Code.json"

      saveRequest.launch(jsonFileName)
    }
  }
}