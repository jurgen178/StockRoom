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

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Locale

data class FilterTypeJson
(
  val name: String,
  var typeId: FilterTypeEnum,
  val data: String,
)

class Filters(
  var map: MutableMap<String, List<IFilterType>> = mutableMapOf(),
  val context: Context? = null
) {

  private val defaultFilterName =
    context?.getString(R.string.filter_default_name) ?: "Standard Filter"

  var selectedFilter: String = defaultFilterName
    set(value) {
      if (field != value) {
        field = value
        val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)
        sharedPreferences
            .edit()
            .putString("selectedFilter", value)
            .apply()
      }
    }

  var filterActive: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)
        sharedPreferences
            .edit()
            .putBoolean("filterActive", value)
            .apply()
      }
    }

  fun add(filterType: IFilterType) {
    val list: MutableList<IFilterType> = mutableListOf()
    if (map.containsKey(selectedFilter)) {
      map[selectedFilter]?.let { list.addAll(it) }
    }
    list.add(filterType)
    map[selectedFilter] = list
  }

  fun update(
    filterType: IFilterType,
    index: Int
  ) {
    if (map.containsKey(selectedFilter)) {
      val list: MutableList<IFilterType> = mutableListOf()
      map[selectedFilter]?.let { list.addAll(it) }
      if (index >= 0 && index < list.size) {
        list[index] = filterType
        map[selectedFilter] = list
      }
    }
  }

  fun delete(index: Int) {
    if (map.containsKey(selectedFilter)) {
      val list: MutableList<IFilterType> = mutableListOf()
      map[selectedFilter]?.let { list.addAll(it) }
      if (index >= 0 && index < list.size) {
        list.removeAt(index)
        map[selectedFilter] = list
      }
    }
  }

  fun getFilterList(): List<IFilterType> {
    return if (filterActive && map.containsKey(selectedFilter)) {
      map[selectedFilter]!!
    } else {
      emptyList()
    }
  }

  // sorted alphabetically case insensitive
  fun getFilterNameList(): List<String> {
    val list: MutableList<String> = mutableListOf()
    map.forEach { (name, _) ->
      list.add(name)
    }
    return list.sortedBy { filterName ->
      filterName.toLowerCase(Locale.ROOT)
    }
  }
}

class FilterDataRepository(val context: Context) {

  fun getSerializedStr(): String {

    verify()

    var jsonString = ""
    try {
      val filters = SharedRepository.filterMap.value
      if (filters != null) {
        val filterTypeJsonList: MutableList<FilterTypeJson> = mutableListOf()
        filters.map.forEach { (name, filterList) ->
          filterList.forEach { filter ->
            filterTypeJsonList.add(
                FilterTypeJson(
                    name = name,
                    typeId = filter.typeId,
                    data = filter.serializedData
                )
            )
          }
        }

        // Convert to a json string.
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        jsonString = gson.toJson(filterTypeJsonList)
      }
    } catch (e: Exception) {
      Toast.makeText(
          context, context.getString(R.string.load_filter_error, e.message), Toast.LENGTH_LONG
      )
          .show()
    }

    return jsonString
  }

  fun setSerializedStr(
    filterData: String,
    selectedFilter: String?,
    filterActive: Boolean?
  ) {
    try {

      val sType = object : TypeToken<List<FilterTypeJson>>() {}.type
      val gson = Gson()
      val filterList = gson.fromJson<List<FilterTypeJson>>(filterData, sType)

      val map: MutableMap<String, List<IFilterType>> = mutableMapOf()

      filterList?.forEach { filterTypeJson ->
        val list: MutableList<IFilterType> = mutableListOf()
        if (map.containsKey(filterTypeJson.name)) {
          map[filterTypeJson.name]?.let { list.addAll(it) }
        }
        val filterType = FilterFactory.create(filterTypeJson.typeId, context)
        filterType.data = filterTypeJson.data
        list.add(filterType)
        map[filterTypeJson.name] = list
      }

      val filters = Filters(map, context)
      filters.selectedFilter =
        selectedFilter ?: SharedRepository.filterMap.value?.selectedFilter.toString()
      filters.filterActive = filterActive ?: SharedRepository.filterMap.value?.filterActive == true
      SharedRepository.filterMap.value = filters

      verify()
    } catch (e: Exception) {
      Toast.makeText(
          context, context.getString(R.string.save_filter_error, e.message), Toast.LENGTH_LONG
      )
          .show()
    }
  }

  val filterActive: Boolean
    get() = SharedRepository.filterMap.value?.filterActive == true

  private fun verify() {
    var selectedFilter = SharedRepository.filterMap.value?.selectedFilter ?: "Filter"

    val map: MutableMap<String, List<IFilterType>>? = SharedRepository.filterMap.value?.map

    if (map != null && !map.containsKey(selectedFilter)) {
      if (map.isNotEmpty()) {
        selectedFilter = map.toList()
            .first().first
        SharedRepository.filterMap.value?.selectedFilter = selectedFilter
      }
    }
  }

  fun addData(filterType: IFilterType) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.add(filterType)
      SharedRepository.filterMap.value = filters
    }
  }

  // update Filter at position index
  fun updateData(
    filterType: IFilterType,
    index: Int
  ) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.update(filterType, index)
      SharedRepository.filterMap.value = filters
    }
  }

  fun deleteData(index: Int) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.delete(index)
      SharedRepository.filterMap.value = filters
    }
  }

  fun deleteAllData() {
    val filters = Filters(mutableMapOf(), context)
    filters.filterActive = SharedRepository.filterMap.value?.filterActive == true

    SharedRepository.filterMap.value = filters
  }

  fun enable(enabled: Boolean) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.filterActive = enabled
      SharedRepository.filterMap.value = filters
    }
  }

  fun getFilterList(): List<IFilterType> {
    val filters = SharedRepository.filterMap.value
    return filters?.getFilterList() ?: emptyList()
  }

  fun getFilterNameList(): List<String> {
    val filters = SharedRepository.filterMap.value
    return filters?.getFilterNameList() ?: emptyList()
  }

  fun getSelectedFilterName(): String {
    val filters = SharedRepository.filterMap.value

//    if (filters?.map?.containsKey(selectedFilter) != true) {
//      if (filters?.map?.isNotEmpty() == true) {
//        selectedFilter = filters.map.toList()
//            .first().first
//        SharedRepository.filterMap.value?.selectedFilter = selectedFilter
//      }
//    }

    return filters?.selectedFilter ?: "Filter"
  }

  fun setSelectedFilterName(value: String) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.selectedFilter = value
      SharedRepository.filterMap.value = filters
    }
  }
}

class FilterDataViewModel(application: Application) : AndroidViewModel(application) {

  private val filterDataRepository: FilterDataRepository = FilterDataRepository(application)
  var data: LiveData<Filters> = SharedRepository.filterMapLiveData

  fun getSerializedStr(): String {
    return filterDataRepository.getSerializedStr()
  }

  fun setSerializedStr(
    filterData: String,
    selectedFilter: String? = null,
    filterActive: Boolean = false
  ) {
    return filterDataRepository.setSerializedStr(filterData, selectedFilter, filterActive)
  }

  fun addData(filterType: IFilterType) {
    filterDataRepository.addData(filterType)
  }

  fun updateData(
    filterType: IFilterType,
    index: Int
  ) {
    filterDataRepository.updateData(filterType, index)
  }

  fun deleteData(index: Int) {
    filterDataRepository.deleteData(index)
  }

  fun deleteAllData() {
    filterDataRepository.deleteAllData()
  }

  val filterList: List<IFilterType>
    get() = filterDataRepository.getFilterList()

  val filterNameList: List<String>
    get() = filterDataRepository.getFilterNameList()

  var selectedFilter: String
    get() = filterDataRepository.getSelectedFilterName()
    set(value) = filterDataRepository.setSelectedFilterName(value)

  var filterActive: Boolean
    get() = filterDataRepository.filterActive
    set(value) = filterDataRepository.enable(value)
}
