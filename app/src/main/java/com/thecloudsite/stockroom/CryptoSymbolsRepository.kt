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

class CryptoSymbolsRepository(private val api: () -> CrypoSymbolsData?) : BaseRepository() {

  private val _symbols = MutableLiveData<List<CrypoSymbolEntry>>()
  val symbols: LiveData<List<CrypoSymbolEntry>>
    get() = _symbols

  suspend fun getData() {
    _symbols.value = getCryptoSymbols()
  }

  private suspend fun getCryptoSymbols(): List<CrypoSymbolEntry> {

    val api: CrypoSymbolsData = api() ?: return emptyList()

    val cryptoSymbols: List<CrypoSymbolEntry>? = try {
      safeApiCall(
        call = {
          updateCounter()
          api.getCrypoSymbolsDataAsync()
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
