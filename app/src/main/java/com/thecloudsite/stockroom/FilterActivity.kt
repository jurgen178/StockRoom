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
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.thecloudsite.stockroom.FilterDataTypeEnum.DoubleType
import com.thecloudsite.stockroom.FilterDataTypeEnum.TextType
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.R.string
import kotlinx.android.synthetic.main.activity_filter.addFilterButton
import kotlinx.android.synthetic.main.activity_filter.filterRecyclerView

class FilterActivity : AppCompatActivity() {

  private lateinit var filterDataViewModel: FilterDataViewModel

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_filter)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val filterClickListenerUpdate = { filterType: IFilterType, index: Int ->
      filterItemUpdateClicked(
          filterType, index
      )
    }
    val filterClickListenerDelete =
      { filterType: IFilterType, index: Int -> filterItemDeleteClicked(filterType, index) }
    val filterAdapter =
      FilterListAdapter(this, filterClickListenerUpdate, filterClickListenerDelete)

    filterDataViewModel = ViewModelProvider(this).get(FilterDataViewModel::class.java)

    filterDataViewModel.data.observe(this, Observer { data ->
      data?.let { filterData ->
        filterAdapter.setFilter(filterData)
      }
    })

    filterRecyclerView.layoutManager = LinearLayoutManager(this)
    filterRecyclerView.adapter = filterAdapter

    val testdata: List<IFilterType> =
      listOf(FilterTestType(), FilterTestType(), FilterTextType(), FilterDoubleType())
    filterDataViewModel.setData(testdata)


    val s = filterDataViewModel.getSerializedStr()


    addFilterButton.setOnClickListener {
      val builder = Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(layout.dialog_add_filter, null)
      val addUpdateFilterHeadlineView =
        dialogView.findViewById<TextView>(id.addUpdateFilterHeadline)
      addUpdateFilterHeadlineView.text = getString(string.add_filter)

      val textViewFilterTextType = dialogView.findViewById<TextView>(id.textViewFilterTextType)
      val textInputLayoutFilterTextType =
        dialogView.findViewById<TextInputLayout>(id.textInputLayoutFilterTextType)
      val textViewFilterDoubleType =
        dialogView.findViewById<TextView>(id.textViewFilterDoubleType)
      val textInputLayoutFilterDoubleType =
        dialogView.findViewById<TextInputLayout>(id.textInputLayoutFilterDoubleType)
      textViewFilterTextType.visibility = View.GONE
      textInputLayoutFilterTextType.visibility = View.GONE
      textViewFilterDoubleType.visibility = View.GONE
      textInputLayoutFilterDoubleType.visibility = View.GONE

      val spinnerData = getFilterDescriptionList()
      val textViewFilterSpinner = dialogView.findViewById<Spinner>(id.textViewFilterSpinner)
      textViewFilterSpinner.adapter =
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinnerData)

      textViewFilterSpinner.onItemSelectedListener = object : OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(
          parent: AdapterView<*>?,
          view: View?,
          position: Int,
          id: Long
        ) {
          val filter = FilterFactory.create(position)
          when (filter.type) {
            TextType -> {
              textViewFilterTextType.visibility = View.VISIBLE
              textInputLayoutFilterTextType.visibility = View.VISIBLE
              textViewFilterDoubleType.visibility = View.GONE
              textInputLayoutFilterDoubleType.visibility = View.GONE
            }
            DoubleType -> {
              textViewFilterTextType.visibility = View.GONE
              textInputLayoutFilterTextType.visibility = View.GONE
              textViewFilterDoubleType.visibility = View.VISIBLE
              textInputLayoutFilterDoubleType.visibility = View.VISIBLE
            }
            else -> {
              textViewFilterTextType.visibility = View.GONE
              textInputLayoutFilterTextType.visibility = View.GONE
              textViewFilterDoubleType.visibility = View.GONE
              textInputLayoutFilterDoubleType.visibility = View.GONE
            }
          }
        }
      }

      val filterDoubleValueView = dialogView.findViewById<TextView>(id.filterDoubleValue)
      val filterTextValueView = dialogView.findViewById<TextView>(id.filterTextValue)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              string.add
          ) { _, _ ->
            val filterIndex = textViewFilterSpinner.selectedItemPosition
            val filterType = FilterFactory.create(filterIndex)

            filterType.data = when (filterType.type) {
              TextType -> {
                // Add () to avoid cast exception.
                (filterTextValueView.text).toString()
                    .trim()
              }
              DoubleType -> {
                // Add () to avoid cast exception.
                (filterDoubleValueView.text).toString()
                    .trim()
              }
              else -> {
                ""
              }
            }

            filterDataViewModel.addData(filterType)

            Toast.makeText(
                this, getString(R.string.add_filter_msg, filterType.desc), Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(
              string.cancel
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

  private fun filterItemUpdateClicked(
    filterType: IFilterType,
    index: Int
  ) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_filter, null)

    val addUpdateFilterHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateFilterHeadline)
    addUpdateFilterHeadlineView.text = getString(R.string.update_filter)

    val spinnerData = getFilterDescriptionList()
    val textViewFilterSpinner = dialogView.findViewById<Spinner>(R.id.textViewFilterSpinner)
    textViewFilterSpinner.adapter =
      ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinnerData)
    textViewFilterSpinner.setSelection(spinnerData.indexOf(filterType.desc))

    textViewFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        val filter = FilterFactory.create(position)
        if (filter.desc == "") {

        }
      }
    }

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->

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
    filterType: IFilterType,
    index: Int
  ) {
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.delete_filter)
        .setMessage(
            getString(
                R.string.delete_filter_confirm, filterType.desc
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->

          filterDataViewModel.deleteData(index)

          Toast.makeText(
              this, getString(R.string.delete_filter_msg, filterType.desc), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }
}

