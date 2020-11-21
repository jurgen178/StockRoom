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

class FilterDataRepository(val context: Context) {

//  private val _data = MutableLiveData<List<IFilterType>>()
//  val data: LiveData<List<IFilterType>>
//    get() = _data

  fun getSerializedStr(): String {

    val filterTypeJsonList = SharedRepository.filterLiveData.value?.let { filterList ->
      filterList.map { filterType ->
        FilterTypeJson(
            typeId = filterType.typeId,
            data = filterType.data
        )
      }
    }

    // Convert to a json string.
    val gson: Gson = GsonBuilder()
        //.setPrettyPrinting()
        .create()
    val jsonString = gson.toJson(filterTypeJsonList)

    return jsonString
  }

  fun setData(filterList: List<IFilterType>) {
    SharedRepository.filterLiveData.value = filterList
  }

  fun setSerializedStr(filterData: String) {
    val sType = object : TypeToken<List<FilterTypeJson>>() {}.type
    val gson = Gson()
    val filterTypeJsonList = gson.fromJson<List<FilterTypeJson>>(filterData, sType)

    val list: MutableList<IFilterType> = mutableListOf()
    filterTypeJsonList?.forEach { filterTypeJson ->
      if (filterTypeJson.typeId != null && filterTypeJson.data != null) {
        val filterType = FilterFactory.create(filterTypeJson.typeId, context)
        filterType.data = filterTypeJson.data
        list.add(filterType)
      }
    }
    SharedRepository.filterLiveData.value = list
  }

  fun addData(filterType: IFilterType) {
    val list: MutableList<IFilterType> = mutableListOf()
    SharedRepository.filterLiveData.value?.let { list.addAll(it) }
    list.add(filterType)
    SharedRepository.filterLiveData.value = list
  }

  // update Filter at position index
  fun updateData(
    filterType: IFilterType,
    index: Int
  ) {
    val list: MutableList<IFilterType> = mutableListOf()
    SharedRepository.filterLiveData.value?.let { list.addAll(it) }
    if (index >= 0 && index < list.size) {
      list[index] = filterType
      SharedRepository.filterLiveData.value = list
    }
  }

  fun deleteData(index: Int) {
    val list: MutableList<IFilterType> = mutableListOf()
    SharedRepository.filterLiveData.value?.let { list.addAll(it) }
    if (index >= 0 && index < list.size) {
      list.removeAt(index)
      SharedRepository.filterLiveData.value = list
    }
  }
}

class FilterDataViewModel(application: Application) : AndroidViewModel(application) {

  private val filterDataRepository: FilterDataRepository = FilterDataRepository(application)
  var data: LiveData<List<IFilterType>> = SharedRepository.filterData

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
}
