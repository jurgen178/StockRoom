/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.stockroom

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_add.button_add
import kotlinx.android.synthetic.main.activity_add.importButton

/**
 * Activity for entering a new symbol.
 */

const val importListActivityRequestCode = 2

class AddActivity : AppCompatActivity() {

  private lateinit var addView: EditText
  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    const val EXTRA_REPLY = "com.android.stockroom.ADDSYMBOL"
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add)
    addView = findViewById(R.id.edit_add)
    //supportActionBar?.setDisplayHomeAsUpEnabled(true)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Add activity started.")

/*
    // Setup observer to enable valid data for the export function.
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let {
      }
    })
*/

    button_add.setOnClickListener {
      val replyIntent = Intent()
      if (TextUtils.isEmpty(addView.text)) {
        setResult(Activity.RESULT_CANCELED, replyIntent)
      } else {
        val symbol = addView.text.toString()
        replyIntent.putExtra(EXTRA_REPLY, symbol)
        setResult(Activity.RESULT_OK, replyIntent)
      }
      finish()
    }

    importButton.setOnClickListener {
      val intent = Intent()
          .setType("*/*")
          .setAction(Intent.ACTION_OPEN_DOCUMENT)
      startActivityForResult(
          Intent.createChooser(intent, "Select a file"), importListActivityRequestCode
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
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == importListActivityRequestCode) {
        if (data != null && data.data is Uri) {
          val importListUri = data.data!!
          stockRoomViewModel.importList(applicationContext, importListUri)
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

