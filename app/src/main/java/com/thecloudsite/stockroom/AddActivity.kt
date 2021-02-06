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
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import com.thecloudsite.stockroom.databinding.ActivityAddBinding
import com.thecloudsite.stockroom.list.ListActivity
import java.util.Locale

/*
 * Activity for entering a new symbol.
 */

const val importListActivityRequestCode = 2

class AddActivity : AppCompatActivity() {

  private lateinit var binding: ActivityAddBinding
  private lateinit var addView: EditText
  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    const val EXTRA_REPLY = "com.thecloudsite.stockroom.ADDSYMBOL"
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityAddBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    addView = binding.editAdd
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Crashlytics test
    //throw RuntimeException("Test Crash") // Force a crash

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

/*
    // Setup observer to enable valid data for the export function.
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let {
      }
    })
*/

    binding.buttonAdd.setOnClickListener {
      val replyIntent = Intent()
      if (TextUtils.isEmpty(addView.text)) {
        setResult(Activity.RESULT_CANCELED, replyIntent)
      } else {
        val symbol = addView.text.toString()
            .trim()

        // https://convertcodes.com/unicode-converter-encode-decode-utf/
        if (symbol.toLowerCase(Locale.ROOT) == "\u0064\u0065\u0062\u0075\u0067") {
          val intent = Intent(this@AddActivity, ListActivity::class.java)
          startActivity(intent)
          setResult(Activity.RESULT_CANCELED, replyIntent)
        } else {
          replyIntent.putExtra(EXTRA_REPLY, symbol)
          setResult(Activity.RESULT_OK, replyIntent)
        }
      }
      finish()
    }

    binding.importButton.setOnClickListener {
      // match importList()
      val mimeTypes = arrayOf(

          // .json
          "application/json",
          "text/x-json",

          // .csv
          "text/csv",
          "text/comma-separated-values",
          "application/octet-stream",

          // .txt
          "text/plain"
      )

      val intent = Intent()
      intent.type = "*/*"
      intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
      intent.action = Intent.ACTION_OPEN_DOCUMENT

      startActivityForResult(
          Intent.createChooser(intent, getString(R.string.import_select_file)),
          importListActivityRequestCode
      )
    }

    /*
    exportButton.setOnClickListener {
      val intent = Intent()
          .setType("application/json")
          .setAction(Intent.ACTION_CREATE_DOCUMENT)
          .addCategory(Intent.CATEGORY_OPENABLE)
      startActivityForResult(
          Intent.createChooser(intent, "Select a file"), exportListActivityRequestCode
      )
    }
*/
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
      if (requestCode == importListActivityRequestCode) {
        resultData?.data?.also { uri ->

          // Perform operations on the document using its URI.
          stockRoomViewModel.importList(applicationContext, uri)
          finish()
        }
      }
      /*
      else
        if (requestCode == exportListActivityRequestCode) {
          if (data != null && data.data is Uri) {
            val exportListUri = data.data!!
            stockRoomViewModel.exportList(applicationContext, exportListUri)
            finish()
          }
        }
      */
    }
  }
}

