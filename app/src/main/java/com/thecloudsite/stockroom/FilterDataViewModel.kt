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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FilterDataRepository {

  private val _data = MutableLiveData<List<IFilterType>>()
  val data: LiveData<List<IFilterType>>
    get() = _data

  fun setData(filterList: List<IFilterType>) {
    _data.value = filterList
  }

  fun addData(filterType: IFilterType) {
    val list: MutableList<IFilterType> = mutableListOf()
    _data.value?.let { list.addAll(it) }
    list.add(filterType)
    _data.value = list
  }

  fun deleteData(index: Int) {
    val list: MutableList<IFilterType> = mutableListOf()
    _data.value?.let { list.addAll(it) }
    if (index >= 0 && index < list.size) {
      list.removeAt(index)
      _data.value = list
    }
  }
}

class FilterDataViewModel(application: Application) : AndroidViewModel(application) {

  private val filterDataRepository: FilterDataRepository = FilterDataRepository()
  var data: LiveData<List<IFilterType>>

  fun setData(filterList: List<IFilterType>) {
    filterDataRepository.setData(filterList)
  }

  fun addData(filterType: IFilterType) {
    filterDataRepository.addData(filterType)
  }

  fun deleteData(index: Int) {
    filterDataRepository.deleteData(index)
  }

  init {
    data = filterDataRepository.data
  }
}
