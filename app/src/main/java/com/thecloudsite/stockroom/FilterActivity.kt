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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.thecloudsite.stockroom.FilterDataTypeEnum.DateType
import com.thecloudsite.stockroom.FilterDataTypeEnum.DoubleType
import com.thecloudsite.stockroom.FilterDataTypeEnum.IntType
import com.thecloudsite.stockroom.FilterDataTypeEnum.NoType
import com.thecloudsite.stockroom.FilterDataTypeEnum.TextType
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.R.string
import kotlinx.android.synthetic.main.activity_filter.addFilterButton
import kotlinx.android.synthetic.main.activity_filter.filterEnableSwitch
import kotlinx.android.synthetic.main.activity_filter.filterRecyclerView
import java.time.LocalDateTime
import java.time.ZoneOffset

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

    filterEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
      SharedRepository.filterActive.value = isChecked
    }

    SharedRepository.filterActiveLiveData.observe(this, Observer { isChecked ->
      filterEnableSwitch.isChecked = isChecked

      if (isChecked) {
        filterRecyclerView.visibility = View.VISIBLE
        addFilterButton.visibility = View.VISIBLE
      } else {
        filterRecyclerView.visibility = View.GONE
        addFilterButton.visibility = View.GONE
      }

      val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
      sharedPreferences
          .edit()
          .putBoolean("filterEnabled", isChecked)
          .apply()
    })

//    val testdata: List<IFilterType> =
//      listOf(FilterTestType(), FilterTestType(), FilterTextType(), FilterDoubleType())
//    filterDataViewModel.setData(testdata)

    addFilterButton.setOnClickListener {
      addUpdateFilter(FilterNullType(), -1)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onPause() {
    super.onPause()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)

    with(sharedPreferences.edit()) {
      putString("filterSetting", filterDataViewModel.getSerializedStr())
      commit()
    }
  }

  private fun filterItemUpdateClicked(
    filterType: IFilterType,
    index: Int
  ) {
    addUpdateFilter(filterType, index)
  }

  private fun filterItemDeleteClicked(
    filterType: IFilterType,
    index: Int
  ) {
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.delete_filter)
        .setMessage(
            getString(
                R.string.delete_filter_confirm, filterType.displayName
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->

          filterDataViewModel.deleteData(index)

          Toast.makeText(
              this, getString(R.string.delete_filter_msg, filterType.displayName),
              Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  private fun addUpdateFilter(
    filterType: IFilterType,
    index: Int
  ) {
    val builder = Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(layout.dialog_add_filter, null)
    val addUpdateFilterHeadlineView =
      dialogView.findViewById<TextView>(id.addUpdateFilterHeadline)

    val textViewFilterDesc = dialogView.findViewById<TextView>(id.textViewFilterDesc)
    val textViewFilterTextType = dialogView.findViewById<TextView>(id.textViewFilterTextType)
    val textInputLayoutFilterTextType =
      dialogView.findViewById<TextInputLayout>(id.textInputLayoutFilterTextType)

    val textViewFilterDoubleType =
      dialogView.findViewById<TextView>(id.textViewFilterDoubleType)
    val textInputLayoutFilterDoubleType =
      dialogView.findViewById<TextInputLayout>(id.textInputLayoutFilterDoubleType)

    val textViewFilterIntType =
      dialogView.findViewById<TextView>(id.textViewFilterIntType)
    val textInputLayoutFilterIntType =
      dialogView.findViewById<TextInputLayout>(id.textInputLayoutFilterIntType)

    textViewFilterTextType.visibility = View.GONE
    textInputLayoutFilterTextType.visibility = View.GONE
    textViewFilterDoubleType.visibility = View.GONE
    textInputLayoutFilterDoubleType.visibility = View.GONE
    textViewFilterIntType.visibility = View.GONE
    textInputLayoutFilterIntType.visibility = View.GONE

    val spinnerData = getFilterNameList(applicationContext)
    val textViewFilterSpinner = dialogView.findViewById<Spinner>(id.textViewFilterSpinner)
    textViewFilterSpinner.adapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, spinnerData)

    val filterDoubleValueView = dialogView.findViewById<TextView>(id.filterDoubleValue)
    val filterIntValueView = dialogView.findViewById<TextView>(id.filterIntValue)
    val filterTextValueView = dialogView.findViewById<TextView>(id.filterTextValue)
    val datePickerFilterDateView =
      dialogView.findViewById<DatePicker>(R.id.datePickerFilter)

    // Update or Add?
    if (filterType.typeId != FilterTypeEnum.FilterNullType) {
      // Update
      textViewFilterSpinner.setSelection(spinnerData.indexOf(filterType.displayName))
      addUpdateFilterHeadlineView.text = getString(string.update_filter)

      when (filterType.dataType) {
        TextType -> {
          filterTextValueView.text = filterType.data
        }
        DoubleType -> {
          filterDoubleValueView.text = filterType.data
        }
        IntType -> {
          filterIntValueView.text = filterType.data
        }
        DateType -> {
          val date = try {
            filterType.serializedData.toLong()
          } catch (e: Exception) {
            0L
          }
          val localDateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC)
          // month is starting from zero
          datePickerFilterDateView.updateDate(
              localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
          )
        }
        NoType -> {
        }
      }
    } else {
      // Add
      addUpdateFilterHeadlineView.text = getString(string.add_filter)
    }

    textViewFilterDesc.text = filterType.desc

    textViewFilterSpinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        val filter = FilterFactory.create(position, applicationContext)
        textViewFilterDesc.text = filter.desc

        when (filter.dataType) {
          TextType -> {
            textViewFilterTextType.visibility = View.VISIBLE
            textInputLayoutFilterTextType.visibility = View.VISIBLE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
          }
          DoubleType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.VISIBLE
            textInputLayoutFilterDoubleType.visibility = View.VISIBLE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
          }
          IntType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.VISIBLE
            textInputLayoutFilterIntType.visibility = View.VISIBLE
            datePickerFilterDateView.visibility = View.GONE
          }
          DateType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.VISIBLE
          }
          NoType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
          }
        }
      }
    }

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            if (filterType.typeId != FilterTypeEnum.FilterNullType) string.update else string.add
        ) { _, _ ->
          val filterIndex = textViewFilterSpinner.selectedItemPosition
          val newFilterType = FilterFactory.create(filterIndex, applicationContext)

          newFilterType.data = when (newFilterType.dataType) {
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
            IntType -> {
              // Add () to avoid cast exception.
              (filterIntValueView.text).toString()
                  .trim()
            }
            DateType -> {
              val localDateTime: LocalDateTime = LocalDateTime.of(
                  datePickerFilterDateView.year, datePickerFilterDateView.month + 1,
                  datePickerFilterDateView.dayOfMonth, 0, 0
              )
              val date = localDateTime.toEpochSecond(ZoneOffset.UTC)
              date.toString()
            }
            NoType -> {
              ""
            }
          }

          // Update or Add?
          if (filterType.typeId != FilterTypeEnum.FilterNullType) {
            // Update
            filterDataViewModel.updateData(newFilterType, index)

            Toast.makeText(
                this, getString(R.string.update_filter_msg, newFilterType.displayName),
                Toast.LENGTH_LONG
            )
                .show()
          } else {
            // Add
            filterDataViewModel.addData(newFilterType)

            Toast.makeText(
                this, getString(R.string.add_filter_msg, newFilterType.displayName),
                Toast.LENGTH_LONG
            )
                .show()
          }
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

