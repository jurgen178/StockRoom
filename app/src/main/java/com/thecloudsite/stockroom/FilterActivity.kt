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
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_filter.addFilterButton
import kotlinx.android.synthetic.main.activity_filter.filterRecyclerView

class FilterActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_filter)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val filterClickListenerUpdate =
      { filterType: IFilterType -> filterItemUpdateClicked(filterType) }
    val filterClickListenerDelete =
      { filterType: IFilterType -> filterItemDeleteClicked(filterType) }
    val filterAdapter =
      FilterListAdapter(this, filterClickListenerUpdate, filterClickListenerDelete)

    filterRecyclerView.layoutManager = LinearLayoutManager(this)
    filterRecyclerView.adapter = filterAdapter

    filterAdapter.setFilter(listOf(FilterTestType()))

    addFilterButton.setOnClickListener {
      val builder = AlertDialog.Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.dialog_add_filter, null)
      val addUpdateFilterHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateFilterHeadline)
      addUpdateFilterHeadlineView.text = getString(R.string.add_filter)

      val spinnerData = getFilterDescriptionList()
      val textViewFilterSpinner = dialogView.findViewById<Spinner>(R.id.textViewFilterSpinner)
      textViewFilterSpinner.adapter =
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinnerData)

      val filterValueView = dialogView.findViewById<TextView>(R.id.filterValue)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val filterValueText = (filterValueView.text).toString()
                .trim()

            filterAdapter.addFilter(FilterTestType())

            Toast.makeText(this, "pluralstr", Toast.LENGTH_LONG)
                .show()
          }
          .setNegativeButton(
              R.string.cancel
          ) { _, _ ->
          }
      builder
          .create()
          .show()
    }
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
//      if (requestCode == importListActivityRequestCode) {
//        resultData?.data?.also { uri ->
//
//          // Perform operations on the document using its URI.
//          stockRoomViewModel.importList(applicationContext, uri)
//          finish()
//        }
//      }
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

  private fun filterItemUpdateClicked(filterType: IFilterType) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_filter, null)

    val addUpdateFilterHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateFilterHeadline)
    addUpdateFilterHeadlineView.text = getString(R.string.update_filter)
    val filterValueView = dialogView.findViewById<TextView>(R.id.filterValue)
    filterValueView.text = "test123"

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          val filterValueText = (filterValueView.text).toString()
              .trim()

          Toast.makeText(
              this, "pluralstr", Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
          //getDialog().cancel()
        }
    builder
        .create()
        .show()
  }

  private fun filterItemDeleteClicked(
    filterType: IFilterType
  ) {
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.delete_filter)
        .setMessage(
            getString(
                R.string.delete_filter_confirm
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->
          //stockRoomViewModel.deleteAsset(asset)
          Toast.makeText(
              this, getString(R.string.delete_filter_msg), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }
}

