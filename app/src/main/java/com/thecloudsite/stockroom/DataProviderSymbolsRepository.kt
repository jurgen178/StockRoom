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

class CryptoSymbolsRepository() : BaseRepository() {

  private val _symbols = MutableLiveData<List<CryptoSymbolEntry>>()
  val symbols: LiveData<List<CryptoSymbolEntry>>
    get() = _symbols

  suspend fun getData(api: () -> CryptoSymbolsData?) {
    _symbols.value = getCryptoSymbols(api)
  }

  private suspend fun getCryptoSymbols(api: () -> CryptoSymbolsData?): List<CryptoSymbolEntry> {

    if (api() == null) {
      return emptyList()
    }

    val cryptoSymbols: List<CryptoSymbolEntry>? = try {
      safeApiCall(
        call = {
          updateCounter()
          api()!!.getCryptoSymbolsDataAsync()
            .await()
        },
        errorMessage = "Error getting list data."
      )
    } catch (e: Exception) {
      Log.d("StockChartDataRepository.getYahooChartDataAsync() failed", "Exception=$e")
      null
    }

    return cryptoSymbols ?: emptyList()
  }
}
