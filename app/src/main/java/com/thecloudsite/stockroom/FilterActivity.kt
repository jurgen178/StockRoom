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

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.FilterDataTypeEnum.DateType
import com.thecloudsite.stockroom.FilterDataTypeEnum.DoubleType
import com.thecloudsite.stockroom.FilterDataTypeEnum.IntType
import com.thecloudsite.stockroom.FilterDataTypeEnum.NoType
import com.thecloudsite.stockroom.FilterDataTypeEnum.SelectionType
import com.thecloudsite.stockroom.FilterDataTypeEnum.TextType
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.calc.CreateJsonDocument
import com.thecloudsite.stockroom.calc.GetJsonContent
import com.thecloudsite.stockroom.databinding.ActivityFilterBinding
import com.thecloudsite.stockroom.databinding.DialogAddFilterBinding
import com.thecloudsite.stockroom.databinding.DialogAddFilternameBinding
import com.thecloudsite.stockroom.utils.saveTextToFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

class FilterActivity : AppCompatActivity() {

  private lateinit var binding: ActivityFilterBinding
  private lateinit var loadFilterRequest: ActivityResultLauncher<String>
  private lateinit var saveFilterRequest: ActivityResultLauncher<String>
  private lateinit var filterDataViewModel: FilterDataViewModel
  // private lateinit var stockRoomViewModel: StockRoomViewModel

  // used by listener for FilterFactory.create because applicationContext cannot be used
  lateinit var thisCopy: FilterActivity

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    thisCopy = this

    binding = ActivityFilterBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

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

    binding.filterRecyclerView.layoutManager = LinearLayoutManager(this)
    binding.filterRecyclerView.adapter = filterAdapter

    binding.filterEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
      filterDataViewModel.filterActive = isChecked
    }

    filterDataViewModel.filterLiveData.observe(this, Observer { filter ->

      filterAdapter.setFilter(filter.filterList)

      binding.textViewFilterSelection.text =
        getString(R.string.filter_set, filterDataViewModel.selectedFilter)

      binding.filterEnableSwitch.isChecked = filter.filterActive

      val visibility = if (filter.filterActive) {
        View.VISIBLE
      } else {
        View.GONE
      }

      binding.textViewFilterSelection.visibility = visibility
      binding.filterRecyclerView.visibility = visibility
      binding.addFilterButton.visibility = visibility

      // Show filter mode only for more than one filter
      val filterModeVisibility = if (filter.filterList.size > 1) {
        visibility
      } else {
        View.GONE
      }

      binding.textViewFilterModeText.visibility = filterModeVisibility
      binding.textViewFilterModeSpinner.visibility = filterModeVisibility

      binding.textViewFilterModeSpinner.setSelection(
        when (filter.filterMode) {
          FilterModeTypeEnum.AndType -> 0
          FilterModeTypeEnum.OrType -> 1
        }
      )
    })

    // stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    // initialize the display values for the sub type enum
    initSubTypeList(this)

//    val testdata: List<IFilterType> =
//      listOf(FilterTestType(), FilterTestType(), FilterTextType(), FilterDoubleType())
//    filterDataViewModel.setData(testdata)

    // Setup filter selection menu
    binding.textViewFilterSelection.setOnClickListener { viewFilter ->
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

            val dialogBinding = DialogAddFilternameBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            //val dialogView = inflater.inflate(R.layout.dialog_add_filtername, null)

//            val filterHeaderView =
//              dialogView.findViewById<TextView>(R.id.filterHeader)
//            val addFilterNameView =
//              dialogView.findViewById<TextView>(R.id.addFilterName)

            builder.setView(dialogBinding.root)
              .setTitle(R.string.add_filter_set)
              // Add action buttons
              .setPositiveButton(R.string.add) { _, _ ->
                // Add () to avoid cast exception.
                val filterName = (dialogBinding.addFilterName.text).toString()
                  .trim()
                if (filterName.isEmpty()) {
                  Toast.makeText(
                    this, getString(R.string.filter_name_not_empty),
                    Toast.LENGTH_LONG
                  )
                    .show()
                  return@setPositiveButton
                }

                dialogBinding.addFilterName.setText(filterName)
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
            loadFilterRequest.launch("")
          }
          saveSelected -> {
            // Set default filename.
            val date = ZonedDateTime.now()
              .format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
              .replace(":", "_")
            val jsonFileName = this.getString(R.string.json_default_filter_filename, date)

            saveFilterRequest.launch(jsonFileName)
          }
          else -> {
            val filterName = menuitem.title.trim()
              .toString()
            binding.textViewFilterSelection.text = getString(R.string.filter_set, filterName)

            filterDataViewModel.selectedFilter = filterName
          }
        }
        true
      }
    }

    binding.addFilterButton.setOnClickListener {
      addUpdateFilter(FilterFactory.create(FilterTypeEnum.FilterNullType, this), -1)
    }

    binding.textViewFilterModeSpinner.setSelection(
      when (filterDataViewModel.filterMode) {
        FilterModeTypeEnum.AndType -> 0
        FilterModeTypeEnum.OrType -> 1
      }
    )

    binding.textViewFilterModeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
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

    loadFilterRequest =
      registerForActivityResult(GetJsonContent())
      { uri ->
        // Perform operations on the document using its URI.
        loadFilter(this, uri)
        finish()
      }

    saveFilterRequest =
      registerForActivityResult(CreateJsonDocument())
      { uri ->
        // Perform operations on the document using its URI.
        saveFilter(this, uri)
        finish()
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

    val msg = application.getString(
      R.string.save_filter_msg, filterDataViewModel.filterNameList.size
    )
    saveTextToFile(jsonString, msg, context, exportJsonUri)
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
    val dialogBinding = DialogAddFilterBinding.inflate(inflater)
    //val dialogView = inflater.inflate(layout.dialog_add_filter, null)
    val spinnerData = getFilterTypeList(this)
    dialogBinding.textViewFilterSpinner.adapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, spinnerData)

    val subTypeData: MutableList<String> = mutableListOf()
    subTypeData.addAll(filterType.subTypeList.map { type ->
      type.value
    })
    val subTypeSpinnerAdapter =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, subTypeData)
    dialogBinding.textViewSubTypeSpinner.adapter = subTypeSpinnerAdapter

    // Update or Add?
    if (index >= 0) {
      // Update
      dialogBinding.textViewFilterSpinner.setSelection(spinnerData.indexOf(filterType.displayName))

      when (filterType.dataType) {
        TextType -> {
          dialogBinding.filterTextValue.setText(filterType.data)
        }
        DoubleType -> {
          dialogBinding.filterDoubleValue.setText(filterType.data)
        }
        IntType -> {
          dialogBinding.filterIntValue.setText(filterType.data)
        }
        DateType -> {
          val date = try {
            filterType.serializedData.toLong()
          } catch (e: Exception) {
            0L
          }
          val localDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneOffset.systemDefault())
          // month is starting from zero
          dialogBinding.datePickerFilter.updateDate(
            localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
          )
        }
//        GroupType -> {
//          val groupColor = try {
//            filterType.data.toInt()
//          } catch (e: Exception) {
//            0
//          }
//          val selectedGroup = SharedFilterGroupList.groups.find { group ->
//            group.color == groupColor
//          }
//          dialogBinding.groupSpinnerFilter.setSelection(
//              if (selectedGroup != null) {
//                val selectedIndex = SharedFilterGroupList.groups.indexOf(selectedGroup)
//                selectedIndex
//              } else {
//                // not-assigned color is added to the group list as last entry
//                SharedFilterGroupList.groups.size
//              }
//          )
//        }
        SelectionType -> {
          val selection = try {
            filterType.data.toInt()
          } catch (e: Exception) {
            0
          }
          dialogBinding.selectionSpinnerFilter.setSelection(selection)
        }
        NoType -> {
        }
      }
    }

    dialogBinding.textViewFilterDesc.text = filterType.desc

    fun setUI(
      filterType: IFilterType
    ) {
      data class ViewSet
        (
        var view: View,
        var visibility: Int = View.GONE,
      )

      val viewMap: MutableMap<String, ViewSet> = mutableMapOf()
      viewMap["textViewFilterTextTypeVisibility"] = ViewSet(dialogBinding.textViewFilterTextType)
      viewMap["textInputLayoutFilterTextTypeVisibility"] =
        ViewSet(dialogBinding.textInputLayoutFilterTextType)
      viewMap["textViewFilterDoubleTypeVisibility"] =
        ViewSet(dialogBinding.textViewFilterDoubleType)
      viewMap["textInputLayoutFilterDoubleTypeVisibility"] =
        ViewSet(dialogBinding.textInputLayoutFilterDoubleType)
      viewMap["textViewFilterIntTypeVisibility"] = ViewSet(dialogBinding.textViewFilterIntType)
      viewMap["textInputLayoutFilterIntTypeVisibility"] =
        ViewSet(dialogBinding.textInputLayoutFilterIntType)
      viewMap["datePickerFilterDateViewVisibility"] = ViewSet(dialogBinding.datePickerFilter)
      viewMap["textViewFilterSelectionTypeVisibility"] =
        ViewSet(dialogBinding.textViewFilterSelectionType)
      viewMap["selectionSpinnerFilterVisibility"] = ViewSet(dialogBinding.selectionSpinnerFilter)

      fun allGone() {
        viewMap.forEach { (view, _) ->
          viewMap[view]?.visibility = View.GONE
        }
      }

      when (filterType.dataType) {
        TextType -> {
          viewMap["textViewFilterTextTypeVisibility"]?.visibility = View.VISIBLE
          viewMap["textInputLayoutFilterTextTypeVisibility"]?.visibility = View.VISIBLE
        }
        DoubleType -> {
          viewMap["textViewFilterDoubleTypeVisibility"]?.visibility = View.VISIBLE
          viewMap["textInputLayoutFilterDoubleTypeVisibility"]?.visibility = View.VISIBLE
        }
        IntType -> {
          viewMap["textViewFilterIntTypeVisibility"]?.visibility = View.VISIBLE
          viewMap["textInputLayoutFilterIntTypeVisibility"]?.visibility = View.VISIBLE
        }
        DateType -> {
          viewMap["datePickerFilterDateViewVisibility"]?.visibility = View.VISIBLE
        }
        SelectionType -> {
          viewMap["textViewFilterSelectionTypeVisibility"]?.visibility = View.VISIBLE
          viewMap["selectionSpinnerFilterVisibility"]?.visibility = View.VISIBLE
        }
        NoType -> {
          allGone()
        }
      }

      // Display/Hide text entry for certain types.
      when (filterType.subType) {
        FilterSubTypeEnum.IsPresentType,
        FilterSubTypeEnum.IsNotPresentType,
        FilterSubTypeEnum.IsUsedType,
        FilterSubTypeEnum.IsNotUsedType,
        FilterSubTypeEnum.IsEmptyTextType,
        FilterSubTypeEnum.IsNotEmptyTextType,
        FilterSubTypeEnum.IsMarketLargeCapType,
        FilterSubTypeEnum.IsMarketMidCapType,
        FilterSubTypeEnum.IsMarketSmallCapType,
        FilterSubTypeEnum.IsMarketMicroCapType,
        FilterSubTypeEnum.IsMarketNanoCapType,
        -> {
          allGone()
        }
        else -> {
        }
      }

      viewMap.forEach { (view, _) ->
        viewMap[view]?.view?.visibility = viewMap[view]?.visibility!!
      }
    }

    dialogBinding.textViewFilterSpinner.onItemSelectedListener =
      object : OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(
          parent: AdapterView<*>?,
          view: View?,
          position: Int,
          id: Long
        ) {
          // Preserve sub type if possible when filter is changed.
          val subType = filterType.subType

          // cannot use applicationContext because the context does not update
          // when dark mode (resource color.black is used by the filter) is switched
          filterType = FilterFactory.create(position, thisCopy)
          if (filterType.subTypeList.indexOf(subType) != -1) {
            filterType.subType = subType
          }
          dialogBinding.textViewFilterDesc.text = filterType.desc
          dialogBinding.textViewFilterDesc.visibility =
            if (filterType.desc.isEmpty()) View.GONE else View.VISIBLE

          subTypeData.clear()
          subTypeData.addAll(filterType.subTypeList.map { type ->
            type.value
          })

          setUI(filterType)

          if (view != null) {
            val selection = dialogBinding.selectionSpinnerFilter.selectedItemPosition
            dialogBinding.selectionSpinnerFilter.adapter =
              ArrayAdapter(
                view.context, android.R.layout.simple_list_item_1, filterType.selectionList
              )

            if (selection >= 0 && selection < filterType.selectionList.size) {
              dialogBinding.selectionSpinnerFilter.setSelection(selection)
            }
          }

          dialogBinding.textViewSubTypeSpinner.visibility =
            if (subTypeData.isEmpty()) View.GONE else View.VISIBLE
          subTypeSpinnerAdapter.notifyDataSetChanged()

          // Reset selection if filter type changed and the filter sub type is not available.
          val subTypeIndex = filterType.subTypeList.indexOf(filterType.subType)
          dialogBinding.textViewSubTypeSpinner.setSelection(
            if (subTypeIndex != -1) {
              subTypeIndex
            } else {
              0
            }
          )
        }
      }

    dialogBinding.textViewSubTypeSpinner.onItemSelectedListener =
      object : OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(
          parent: AdapterView<*>?,
          view: View?,
          position: Int,
          id: Long
        ) {
          if (position >= 0 && position < filterType.subTypeList.size) {
            filterType.subType = filterType.subTypeList[position]

            setUI(filterType)
          }
        }
      }

    builder.setView(dialogBinding.root)
      .setTitle(
        if (index >= 0) {
          R.string.update_filter
        } else {
          R.string.add_filter
        }
      )
      // Add action buttons
      .setPositiveButton(
        if (index >= 0) string.update else string.add
      )
      { _, _ ->
        val filterIndex = dialogBinding.textViewFilterSpinner.selectedItemPosition
        val newFilterType = FilterFactory.create(filterIndex, this)

        val subType =
          if (dialogBinding.textViewSubTypeSpinner.selectedItemPosition >= 0
            && dialogBinding.textViewSubTypeSpinner.selectedItemPosition < newFilterType.subTypeList.size
          ) {
            newFilterType.subTypeList[dialogBinding.textViewSubTypeSpinner.selectedItemPosition]
          } else {
            FilterSubTypeEnum.NoType
          }
        newFilterType.subType = subType

        newFilterType.data = when (newFilterType.dataType) {
          TextType -> {
            // Add () to avoid cast exception.
            (dialogBinding.filterTextValue.text).toString()
              .trim()
          }
          DoubleType -> {
            // Add () to avoid cast exception.
            (dialogBinding.filterDoubleValue.text).toString()
              .trim()
          }
          IntType -> {
            // Add () to avoid cast exception.
            (dialogBinding.filterIntValue.text).toString()
              .trim()
          }
          DateType -> {
            val localDateTime: ZonedDateTime = ZonedDateTime.of(
              dialogBinding.datePickerFilter.year,
              dialogBinding.datePickerFilter.month + 1,
              dialogBinding.datePickerFilter.dayOfMonth,
              0,
              0,
              0,
              0,
              ZoneOffset.systemDefault()
            )
            val date = localDateTime.toEpochSecond() // in GMT
            date.toString()
          }
          SelectionType -> {
            val selection = dialogBinding.selectionSpinnerFilter.selectedItemPosition
            selection.toString()
          }
          NoType -> {
            ""
          }
        }

        // Check regex
        if (subType == FilterSubTypeEnum.MatchRegexTextType
          || subType == FilterSubTypeEnum.NotMatchRegexTextType
        ) {
          try {
            newFilterType.data.toRegex(regexOption)
              .containsMatchIn("test")
          } catch (e: Exception) {
            Toast.makeText(
              this, getString(R.string.filter_regex_error_msg, newFilterType.data),
              Toast.LENGTH_LONG
            )
              .show()

            return@setPositiveButton
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
      )
      { _, _ ->
      }
    builder
      .create()
      .show()
  }
}

