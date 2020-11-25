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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class FilterTypeJson
(
  var typeId: FilterTypeEnum,
  val data: String,
)

class Filters {
  var selectedFilter: String = "test"
  var filterActive: Boolean = false
  var map: MutableMap<String, List<IFilterType>> = mutableMapOf()

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

  fun getFilterNameList(): List<String> {
    return if (map.containsKey(selectedFilter)) {
      val list: MutableList<String> = mutableListOf()
      map.forEach { name, filterList ->
        list.add(name)
      }
      return list
    } else {
      emptyList()
    }
  }

  fun enable(enabled: Boolean) {
    filterActive = enabled
  }
}

class FilterDataRepository(val context: Context) {

//  private val _data = MutableLiveData<List<IFilterType>>()
//  val data: LiveData<List<IFilterType>>
//    get() = _data

  fun getSerializedStr(): String {

//    val filterTypeJsonList = SharedRepository.filterData.value?.let { filterList ->
//      filterList.map { filterType ->
//        FilterTypeJson(
//            typeId = filterType.typeId,
//            data = filterType.serializedData
//        )
//      }
//    }

    // Convert to a json string.
    val gson: Gson = GsonBuilder()
        //.setPrettyPrinting()
        .create()
    val jsonString = ""//gson.toJson(SharedRepository.filterData.value)

    return jsonString
  }

  fun setData(filterList: List<IFilterType>) {
    //SharedRepository.filterData.value?.filterList = filterList
  }

  fun setSerializedStr(filterData: String) {
//    val sType = object : TypeToken<List<FilterTypeJson>>() {}.type
//    val gson = Gson()
//    val filterTypeJsonList = gson.fromJson<List<FilterTypeJson>>(filterData, sType)
//
//    val list: MutableList<IFilterType> = mutableListOf()
//    filterTypeJsonList?.forEach { filterTypeJson ->
//      if (filterTypeJson.typeId != null && filterTypeJson.data != null) {
//        val filterType = FilterFactory.create(filterTypeJson.typeId, context)
//        filterType.data = filterTypeJson.data
//        list.add(filterType)
//      }
//    }
//    SharedRepository.filterData.value?.filterList = list
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

  fun enable(enabled: Boolean) {
    val filters = SharedRepository.filterMap.value
    if (filters != null) {
      filters.enable(enabled)
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

  fun getSelectedFilter(): String {
    val filters = SharedRepository.filterMap.value
    return filters?.selectedFilter ?: ""
  }

  fun setSelectedFilter(value: String) {
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

  fun setSerializedStr(filterData: String) {
    return filterDataRepository.setSerializedStr(filterData)
  }

  fun setData(filterList: List<IFilterType>) {
    filterDataRepository.setData(filterList)
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

  fun enable(enabled: Boolean) {
    filterDataRepository.enable(enabled)
  }

  val filterList: List<IFilterType>
    get() = filterDataRepository.getFilterList()

  val filterNameList: List<String>
    get() = filterDataRepository.getFilterNameList()

  var selectedFilter: String
    get() = filterDataRepository.getSelectedFilter()
    set(value) = filterDataRepository.setSelectedFilter(value)
}
