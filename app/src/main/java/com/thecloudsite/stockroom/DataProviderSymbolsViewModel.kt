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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Get all the available symbols from the provider.
class DataProviderSymbolsViewModelCoingecko(application: Application) : AndroidViewModel(application) {

  private val dataProviderSymbolsRepository: DataProviderSymbolsRepository = DataProviderSymbolsRepository()

  val symbols: LiveData<List<DataProviderSymbolEntry>> = dataProviderSymbolsRepository.symbols

//  fun getData(api: () -> DataProviderSymbolsData?) {
//    viewModelScope.launch {
//      dataProviderSymbolsRepository.getData(api)
//    }
//  }

  fun getData() {
    viewModelScope.launch {
      dataProviderSymbolsRepository.getData { CoingeckoSymbolsApiFactory.coingeckoApi }
    }
  }
}
class DataProviderSymbolsViewModelCoinpaprika(application: Application) : AndroidViewModel(application) {

  private val dataProviderSymbolsRepository: DataProviderSymbolsRepository = DataProviderSymbolsRepository()

  val symbols: LiveData<List<DataProviderSymbolEntry>> = dataProviderSymbolsRepository.symbols

  fun getData() {
    viewModelScope.launch {
      dataProviderSymbolsRepository.getData { CoinpaprikaSymbolsApiFactory.coinpaprikaApi }
    }
  }
}
