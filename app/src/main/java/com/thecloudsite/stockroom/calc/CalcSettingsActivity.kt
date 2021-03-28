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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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

const val importCalcActivityRequestCode = 7
const val exportCalcActivityRequestCode = 8

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

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    resultData: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, resultData)

    if (resultCode == Activity.RESULT_OK) {
      val resultCodeShort = requestCode.toShort()
        .toInt()
      if (resultCodeShort == importCalcActivityRequestCode) {
        resultData?.data?.also { uri ->

          // Perform operations on the document using its URI.
          loadCode(this, uri)
          finish()
        }
      } else
        if (resultCodeShort == exportCalcActivityRequestCode) {
          resultData?.data?.also { uri ->

            // Perform operations on the document using its URI.
            saveCode(this, uri)
            finish()
          }
        }
    }
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
      val mimeTypes = arrayOf(

        // .json
        "application/json",
        "text/x-json",

        )

      val intent = Intent()
      intent.type = "*/*"
      intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
      intent.action = Intent.ACTION_OPEN_DOCUMENT

      startActivityForResult(
        Intent.createChooser(intent, getString(R.string.import_select_file)),
        importCalcActivityRequestCode
      )
    }

    private fun onExportCode() {
      // Set default filename.
      val jsonFileName = "AI-Code"
      val intent = Intent()
        .setType("application/json")
        .setAction(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .putExtra(Intent.EXTRA_TITLE, jsonFileName)
      startActivityForResult(
        Intent.createChooser(intent, getString(R.string.export_select_file)),
        exportCalcActivityRequestCode
      )
    }
  }
}