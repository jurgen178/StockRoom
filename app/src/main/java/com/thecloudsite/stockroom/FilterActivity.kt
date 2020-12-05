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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.thecloudsite.stockroom.FilterDataTypeEnum.DateType
import com.thecloudsite.stockroom.FilterDataTypeEnum.DoubleType
import com.thecloudsite.stockroom.FilterDataTypeEnum.GroupType
import com.thecloudsite.stockroom.FilterDataTypeEnum.IntType
import com.thecloudsite.stockroom.FilterDataTypeEnum.NoType
import com.thecloudsite.stockroom.FilterDataTypeEnum.TextType
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.database.Group
import kotlinx.android.synthetic.main.activity_filter.addFilterButton
import kotlinx.android.synthetic.main.activity_filter.filterEnableSwitch
import kotlinx.android.synthetic.main.activity_filter.filterRecyclerView
import kotlinx.android.synthetic.main.activity_filter.textViewFilterModeSpinner
import kotlinx.android.synthetic.main.activity_filter.textViewFilterModeText
import kotlinx.android.synthetic.main.activity_filter.textViewFilterSelection
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

class FilterActivity : AppCompatActivity() {

  private val loadFilterActivityRequestCode = 5
  private val saveFilterActivityRequestCode = 6
  private lateinit var filterDataViewModel: FilterDataViewModel
  //private lateinit var stockRoomViewModel: StockRoomViewModel
  //private lateinit var groups: MutableList<Group>

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

    filterRecyclerView.layoutManager = LinearLayoutManager(this)
    filterRecyclerView.adapter = filterAdapter

    filterEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
      filterDataViewModel.filterActive = isChecked
    }

    filterDataViewModel.data.observe(this, Observer { filter ->

      filterAdapter.setFilter(filter.filterList)

      textViewFilterSelection.text =
        getString(R.string.filter_set, filterDataViewModel.selectedFilter)

      filterEnableSwitch.isChecked = filter.filterActive

      val visibility = if (filter.filterActive) {
        View.VISIBLE
      } else {
        View.GONE
      }
      textViewFilterModeText.visibility = visibility
      textViewFilterModeSpinner.visibility = visibility
      textViewFilterSelection.visibility = visibility
      filterRecyclerView.visibility = visibility
      addFilterButton.visibility = visibility
    })

//    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
//
//    groups = stockRoomViewModel.getGroupsSync()
//        .toMutableList()

    // initialize the display values for the sub type enum
    initSubTypeList(this)

//    val testdata: List<IFilterType> =
//      listOf(FilterTestType(), FilterTestType(), FilterTextType(), FilterDoubleType())
//    filterDataViewModel.setData(testdata)

    // Setup filter selection menu
    textViewFilterSelection.setOnClickListener { viewFilter ->
      val popupMenu = PopupMenu(this, viewFilter)

      var menuIndex: Int = Menu.FIRST

      filterDataViewModel.filterNameList
          .forEach { filtername ->
            popupMenu.menu.add(0, menuIndex++, Menu.NONE, filtername)
          }

      // Last-3 item is to add a new filter
      val addFilterSet = SpannableStringBuilder()
          .color(getColor(R.color.colorAccent)) {
            bold { append(getString(R.string.add_filter_set)) }
          }
      popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addFilterSet)

      // Last-2 item is to delete all filter
      val deleteFilterSetItem = SpannableStringBuilder()
          .color(getColor(R.color.colorAccent)) {
            bold { append(getString(R.string.delete_filter_set)) }
          }
      popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, deleteFilterSetItem)

      // Last-1 item is to load filter
      val loadFilterSetItem = SpannableStringBuilder()
          .color(getColor(R.color.colorAccent)) {
            bold { append(getString(R.string.menu_load_filter_set)) }
          }
      popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, loadFilterSetItem)

      // Last item is to save the filter
      val saveFilterSetItem = SpannableStringBuilder()
          .color(getColor(R.color.colorAccent)) {
            bold { append(getString(R.string.menu_save_filter_set)) }
          }
      popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, saveFilterSetItem)

      popupMenu.show()

      popupMenu.setOnMenuItemClickListener { menuitem ->

        val addSelected = menuIndex - 4 == menuitem.itemId
        val deleteSelected = menuIndex - 3 == menuitem.itemId
        val loadSelected = menuIndex - 2 == menuitem.itemId
        val saveSelected = menuIndex - 1 == menuitem.itemId

        when {
          addSelected -> {
            // Add filter
            val builder = android.app.AlertDialog.Builder(this)
            // Get the layout inflater
            val inflater = LayoutInflater.from(this)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dialogView = inflater.inflate(R.layout.dialog_add_filtername, null)

            val filterHeaderView =
              dialogView.findViewById<TextView>(R.id.filterHeader)
            val addFilterNameView =
              dialogView.findViewById<TextView>(R.id.addFilterName)

            filterHeaderView.text = getString(R.string.add_filter_set)

            builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton(R.string.add) { _, _ ->
                  // Add () to avoid cast exception.
                  val filterName = (addFilterNameView.text).toString()
                      .trim()
                  if (filterName.isEmpty()) {
                    Toast.makeText(
                        this, getString(R.string.filter_name_not_empty),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return@setPositiveButton
                  }

                  addFilterNameView.text = filterName

                  filterDataViewModel.selectedFilter = filterName
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                }
            builder
                .create()
                .show()
          }
          deleteSelected -> {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_filter_title)
                .setMessage(getString(R.string.delete_all_filter_confirm))
                .setPositiveButton(R.string.delete) { _, _ ->
                  filterDataViewModel.deleteAllData()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
          }
          loadSelected -> {
            // match importList()
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
                loadFilterActivityRequestCode
            )
          }
          saveSelected -> {
            // Set default filename.
            val date = LocalDateTime.now()
                .format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
                .replace(":", "_")
            val jsonFileName = this.getString(R.string.json_default_filter_filename, date)
            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE, jsonFileName)
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.export_select_file)),
                saveFilterActivityRequestCode
            )
          }
          else -> {
            val filterName = menuitem.title.trim()
                .toString()
            textViewFilterSelection.text = getString(R.string.filter_set, filterName)

            filterDataViewModel.selectedFilter = filterName
          }
        }
        true
      }
    }

    addFilterButton.setOnClickListener {
      addUpdateFilter(FilterFactory.create(FilterTypeEnum.FilterNullType, this), -1)
    }

    textViewFilterModeSpinner.setSelection(
        when (filterDataViewModel.filterMode) {
          FilterModeTypeEnum.AndType -> 0
          FilterModeTypeEnum.OrType -> 1
        }
    )

    textViewFilterModeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        when (position) {
          0 -> filterDataViewModel.filterMode = FilterModeTypeEnum.AndType
          1 -> filterDataViewModel.filterMode = FilterModeTypeEnum.OrType
        }
      }
    }
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
      if (requestCode == loadFilterActivityRequestCode) {
        resultData?.data?.also { uri ->

          // Perform operations on the document using its URI.
          loadFilter(applicationContext, uri)
          finish()
        }
      } else
        if (resultCodeShort == saveFilterActivityRequestCode) {
          resultData?.data?.also { uri ->

            // Perform operations on the document using its URI.
            saveFilter(applicationContext, uri)
            finish()
          }
        }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onPause() {
    super.onPause()

    storeFilters()
  }

  private fun storeFilters() {
    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)

    with(sharedPreferences.edit()) {
      putString("filterSetting", filterDataViewModel.getSerializedStr())
      commit()
    }
  }

  private fun loadFilter(
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
                  filterDataViewModel.setSerializedStr(text)
                  storeFilters()

                  val msg = application.getString(
                      R.string.load_filter_msg, filterDataViewModel.filterNameList.size
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

  private fun saveFilter(
    context: Context,
    exportJsonUri: Uri
  ) {
    val jsonString = filterDataViewModel.getSerializedStr()

    // Write the json string.
    try {
      context.contentResolver.openOutputStream(exportJsonUri)
          ?.use { output ->
            output as FileOutputStream
            output.channel.truncate(0)
            output.write(jsonString.toByteArray())
          }

      val msg = application.getString(
          R.string.save_filter_msg, filterDataViewModel.filterNameList.size
      )

      Toast.makeText(context, msg, Toast.LENGTH_LONG)
          .show()

    } catch (e: Exception) {
      Toast.makeText(
          context, application.getString(R.string.export_error, e.message),
          Toast.LENGTH_LONG
      )
          .show()
      Log.d("Export JSON error", "Exception: $e")
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
    filter: IFilterType,
    index: Int
  ) {
    var filterType: IFilterType = filter

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

    val textViewSubTypeSpinner = dialogView.findViewById<Spinner>(R.id.textViewSubTypeSpinner)
    val datePickerFilterDateView =
      dialogView.findViewById<DatePicker>(R.id.datePickerFilter)

    val spinnerData = getFilterTypeList(applicationContext)
    val textViewFilterSpinner = dialogView.findViewById<Spinner>(id.textViewFilterSpinner)
    textViewFilterSpinner.adapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, spinnerData)

    val subTypeData: MutableList<String> = mutableListOf()
    subTypeData.addAll(filterType.subTypeList.map { type ->
      type.value
    })
    val subTypeSpinnerAdapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, subTypeData)
    textViewSubTypeSpinner.adapter = subTypeSpinnerAdapter
    val subTypeIndex = filterType.subTypeList.indexOf(filterType.subType)
    textViewSubTypeSpinner.setSelection(subTypeIndex)

    val textViewFilterGroupType =
      dialogView.findViewById<TextView>(id.textViewFilterGroupType)

    val groupSpinnerFilter =
      dialogView.findViewById<Spinner>(R.id.groupSpinnerFilter)
    val groupData: List<String> = SharedFilterGroupList.groups.map { group ->
      group.name
    }
    val groupSpinnerAdapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, groupData)
    groupSpinnerFilter.adapter = groupSpinnerAdapter
    groupSpinnerFilter.setSelection(subTypeIndex)

    val filterDoubleValueView = dialogView.findViewById<TextView>(id.filterDoubleValue)
    val filterIntValueView = dialogView.findViewById<TextView>(id.filterIntValue)
    val filterTextValueView = dialogView.findViewById<TextView>(id.filterTextValue)

    // Update or Add?
    if (index >= 0) {
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
        GroupType -> {
          val date = try {
            filterType.serializedData.toLong()
          } catch (e: Exception) {
            0L
          }
          groupSpinnerFilter
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
        filterType = FilterFactory.create(position, applicationContext)
        textViewFilterDesc.text = filterType.desc
        textViewFilterDesc.visibility = if (filterType.desc.isEmpty()) View.GONE else View.VISIBLE

        when (filterType.dataType) {
          TextType -> {
            textViewFilterTextType.visibility = View.VISIBLE
            textInputLayoutFilterTextType.visibility = View.VISIBLE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
            textViewFilterGroupType.visibility = View.GONE
            groupSpinnerFilter.visibility = View.GONE
          }
          DoubleType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.VISIBLE
            textInputLayoutFilterDoubleType.visibility = View.VISIBLE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
            textViewFilterGroupType.visibility = View.GONE
            groupSpinnerFilter.visibility = View.GONE
          }
          IntType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.VISIBLE
            textInputLayoutFilterIntType.visibility = View.VISIBLE
            datePickerFilterDateView.visibility = View.GONE
            textViewFilterGroupType.visibility = View.GONE
            groupSpinnerFilter.visibility = View.GONE
          }
          DateType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.VISIBLE
            textViewFilterGroupType.visibility = View.GONE
            groupSpinnerFilter.visibility = View.GONE
          }
          GroupType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
            textViewFilterGroupType.visibility = View.VISIBLE
            groupSpinnerFilter.visibility = View.VISIBLE
          }
          NoType -> {
            textViewFilterTextType.visibility = View.GONE
            textInputLayoutFilterTextType.visibility = View.GONE
            textViewFilterDoubleType.visibility = View.GONE
            textInputLayoutFilterDoubleType.visibility = View.GONE
            textViewFilterIntType.visibility = View.GONE
            textInputLayoutFilterIntType.visibility = View.GONE
            datePickerFilterDateView.visibility = View.GONE
            textViewFilterGroupType.visibility = View.GONE
            groupSpinnerFilter.visibility = View.GONE
          }
        }

        subTypeData.clear()
        subTypeData.addAll(filterType.subTypeList.map { type ->
          type.value
        })
        textViewSubTypeSpinner.visibility = if (subTypeData.isEmpty()) View.GONE else View.VISIBLE
        subTypeSpinnerAdapter.notifyDataSetChanged()
      }
    }

    textViewSubTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        if (position >= 0 && position < filterType.subTypeList.size) {
          val selectedSubType = filterType.subTypeList[position]

          // Display/Hide text entry for certain types.
          when (selectedSubType) {
            FilterSubTypeEnum.ContainsTextType,
            FilterSubTypeEnum.NotContainsTextType,
            FilterSubTypeEnum.StartsWithTextType,
            FilterSubTypeEnum.EndsWithTextType,
            FilterSubTypeEnum.IsTextType,
            FilterSubTypeEnum.IsNotTextType
            -> {
              textViewFilterTextType.visibility = View.VISIBLE
              textInputLayoutFilterTextType.visibility = View.VISIBLE
            }
            FilterSubTypeEnum.IsEmptyTextType,
            FilterSubTypeEnum.IsNotEmptyTextType -> {
              textViewFilterTextType.visibility = View.GONE
              textInputLayoutFilterTextType.visibility = View.GONE
            }
            else -> {
            }
          }
        }
      }
    }

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            if (index >= 0) string.update else string.add
        ) { _, _ ->
          val filterIndex = textViewFilterSpinner.selectedItemPosition
          val newFilterType = FilterFactory.create(filterIndex, applicationContext)

          val subType =
            if (textViewSubTypeSpinner.selectedItemPosition >= 0
                && textViewSubTypeSpinner.selectedItemPosition < newFilterType.subTypeList.size
            ) {
              newFilterType.subTypeList[textViewSubTypeSpinner.selectedItemPosition]
            } else {
              FilterSubTypeEnum.NoType
            }
          newFilterType.subType = subType

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
            GroupType -> {
              val group = groupSpinnerFilter.selectedItemPosition
              val color = if (group >= 0 && group < SharedFilterGroupList.groups.size) {
                SharedFilterGroupList.groups[group].color
              } else {
                -1
              }
              color.toString()
            }
            NoType -> {
              ""
            }
          }

          // Update or Add?
          if (index >= 0) {
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

