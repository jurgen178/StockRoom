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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend

data class AssetDividendLiveData(
  var assets: List<Asset> = emptyList(),
  var dividends: List<Dividend> = emptyList(),
)

class StockRoomAllTransactionsFragment : StockRoomBaseTransactionsFragment() {

  private val assetDividendChange = AssetDividendLiveData()
  private val assetDividendChangeLiveData = MediatorLiveData<AssetDividendLiveData>()

  companion object {
    fun newInstance() = StockRoomAllTransactionsFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // Use MediatorLiveView to combine the assets and dividend data changes.
    val assetsLiveData: LiveData<List<Asset>> = stockRoomViewModel.allAssetTable
    assetDividendChangeLiveData.addSource(assetsLiveData) { value ->
      if (value != null) {
        assetDividendChange.assets = value
        assetDividendChangeLiveData.postValue(assetDividendChange)
      }
    }

    val dividendsLiveData: LiveData<List<Dividend>> = stockRoomViewModel.allDividendTable
    assetDividendChangeLiveData.addSource(dividendsLiveData) { value ->
      if (value != null) {
        assetDividendChange.dividends = value
        assetDividendChangeLiveData.postValue(assetDividendChange)
      }
    }

    // Observe asset or dividend changes.
    assetDividendChangeLiveData.observe(requireActivity(), Observer { item ->
      if (item != null) {

        resetTransactionDataList()

        // Asset bought
        addAssetsBought(item.assets)

        // Asset sold
        addAssetsSold(item.assets)

        // Dividend received
        addDividendsReceived(item.dividends)

        // Add stats and update the adapter.
        updateData()
      }
    })
  }
}
