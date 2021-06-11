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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DataProviderSymbolsRepository() : BaseRepository() {

  private val _symbols = MutableLiveData<List<DataProviderSymbolEntry>>()
  val symbols: LiveData<List<DataProviderSymbolEntry>>
    get() = _symbols

  suspend fun getData(api: () -> DataProviderSymbolsData?) {
    _symbols.value = getDataProviderSymbols(api)
  }

  private suspend fun getDataProviderSymbols(api: () -> DataProviderSymbolsData?): List<DataProviderSymbolEntry> {

    if (api() == null) {
      return emptyList()
    }

    val dataProviderSymbols: List<DataProviderSymbolEntry>? = try {
      safeApiCall(
        call = {
          updateCounter()
          api()!!.getDataProviderSymbolsDataAsync()
            .await()
        },
        errorMessage = "Error getting list data."
      )
    } catch (e: Exception) {
      Log.d("DataProviderSymbolsRepository.getDataProviderSymbols() failed", "Exception=$e")
      null
    }

    return dataProviderSymbols ?: emptyList()
  }
}
