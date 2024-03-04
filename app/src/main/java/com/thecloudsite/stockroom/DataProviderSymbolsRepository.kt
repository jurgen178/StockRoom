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
import com.thecloudsite.stockroom.utils.idToName
import java.util.*

open class DataProviderSymbolsBaseRepository() : BaseRepository() {

    private val _symbols = MutableLiveData<List<DataProviderSymbolEntry>>()
    val symbols: LiveData<List<DataProviderSymbolEntry>>
        get() = _symbols

    suspend fun getData() {
        _symbols.value = getDataProviderSymbols()
    }

    open suspend fun getDataProviderSymbols(): List<DataProviderSymbolEntry> {
        return emptyList()
    }
}

class DataProviderCoingeckoSymbolsRepository() : DataProviderSymbolsBaseRepository() {

    override suspend fun getDataProviderSymbols(): List<DataProviderSymbolEntry> {

        val api = CoingeckoSymbolsApiFactory.dataProviderApi ?: return emptyList()

        val dataProviderSymbols: List<DataProviderCoingeckoSymbolEntry>? = try {
            apiCall(
                    call = {
                        updateCounter()
                        api.getDataProviderSymbolsDataAsync()
                                .await()
                    },
                    errorMessage = "Error getting list data."
            )
        } catch (e: Exception) {
            Log.d(
                    "DataProviderCoingeckoSymbolsRepository.getDataProviderSymbols() failed",
                    "Exception=$e"
            )
            null
        }

        return (dataProviderSymbols ?: emptyList())
                .sortedBy { symbol -> symbol.name.lowercase(Locale.ROOT) }
                .map { symbol -> DataProviderSymbolEntry(id = symbol.id, name = symbol.name) }
    }
}

class DataProviderCoinpaprikaSymbolsRepository() : DataProviderSymbolsBaseRepository() {

    override suspend fun getDataProviderSymbols(): List<DataProviderSymbolEntry> {

        val api = CoinpaprikaSymbolsApiFactory.dataProviderApi ?: return emptyList()

        val dataProviderSymbols: List<DataProviderCoinpaprikaSymbolEntry>? = try {
            apiCall(
                    call = {
                        updateCounter()
                        api.getDataProviderSymbolsDataAsync()
                                .await()
                    },
                    errorMessage = "Error getting list data."
            )
        } catch (e: Exception) {
            Log.d(
                    "DataProviderCoinpaprikaSymbolsRepository.getDataProviderSymbols() failed",
                    "Exception=$e"
            )
            null
        }

        return (dataProviderSymbols ?: emptyList())
                .filter { symbol -> symbol.is_active == true }
                .sortedBy { symbol -> symbol.name.lowercase(Locale.ROOT) }
                .map { symbol -> DataProviderSymbolEntry(id = symbol.id, name = symbol.name) }
    }
}

class DataProviderGeminiSymbolsRepository() : DataProviderSymbolsBaseRepository() {

    override suspend fun getDataProviderSymbols(): List<DataProviderSymbolEntry> {

        val api = GeminiSymbolsApiFactory.dataProviderApi ?: return emptyList()

        val dataProviderSymbols: List<String>? = try {
            apiCall(
                    call = {
                        updateCounter()
                        api.getDataProviderSymbolsDataAsync()
                                .await()
                    },
                    errorMessage = "Error getting list data."
            )
        } catch (e: Exception) {
            Log.d(
                    "DataProviderGeminiSymbolsRepository.getDataProviderSymbols() failed",
                    "Exception=$e"
            )
            null
        }

        return (dataProviderSymbols ?: emptyList())
                .sortedBy { symbol -> symbol.lowercase(Locale.ROOT) }
                .map { symbol -> DataProviderSymbolEntry(id = symbol, name = idToName(symbol)) }
    }
}

class DataProviderOkxSymbolsRepository() : DataProviderSymbolsBaseRepository() {

    override suspend fun getDataProviderSymbols(): List<DataProviderSymbolEntry> {

        val api = OkxSymbolsApiFactory.dataProviderApi ?: return emptyList()

        val dataProviderSymbolsResponse: DataProviderSymbolsDataOkxResponse? = try {
            apiCall(
                    call = {
                        updateCounter()
                        api.getDataProviderSymbolsDataAsync()
                                .await()
                    },
                    errorMessage = "Error getting list data."
            )
        } catch (e: Exception) {
            Log.d(
                    "DataProviderOkxSymbolsRepository.getDataProviderSymbols() failed",
                    "Exception=$e"
            )
            null
        }

        val symbolEntries : MutableList<DataProviderSymbolEntry> = mutableListOf()

        dataProviderSymbolsResponse?.data?.filter { entry -> entry.instId.endsWith("-usdc", true) }?.forEach { result ->
            if (result.last > 0.0) {
                val id = result.instId.replace("-usdc", "", true)
                symbolEntries.add(
                        DataProviderSymbolEntry(
                                id = id,
                                name = id,
                        )
                )
            }
        }

        return symbolEntries
                .sortedBy { entry -> entry.id.lowercase(Locale.ROOT) }
    }
}

