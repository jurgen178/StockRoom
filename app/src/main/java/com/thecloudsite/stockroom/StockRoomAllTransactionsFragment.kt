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

class StockRoomAllTransactionsFragment : StockRoomBaseTransactionsFragment() {

  private val accountChange = AccountLiveData()
  private val accountChangeLiveData = MediatorLiveData<AccountLiveData>()

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
    accountChangeLiveData.addSource(assetsLiveData) { value ->
      if (value != null) {
        accountChange.assets = value
        accountChangeLiveData.postValue(accountChange)
      }
    }

    val dividendsLiveData: LiveData<List<Dividend>> = stockRoomViewModel.allDividendTable
    accountChangeLiveData.addSource(dividendsLiveData) { value ->
      if (value != null) {
        accountChange.dividends = value
        accountChangeLiveData.postValue(accountChange)
      }
    }

    // Observe asset or dividend changes.
    accountChangeLiveData.observe(requireActivity(), Observer { item ->
      if (item != null) {

        transactionDataList.clear()
        assetBought = 0
        assetSold = 0
        dividendReceived = 0

        // Asset bought
        addAssetsBought(item.assets)

        // Asset sold
        addAssetsSold(item.assets)

        // Dividend received
        addDividendsReceived(item.dividends)

        transactionDataList.add(
          TransactionData(
            viewType = transaction_stats_type,
            date = 0, // sorted by date, get displayed first
            symbol = "",
            type = TransactionType.StatsType,
            assetBought = assetBought,
            assetSold = assetSold,
            dividendReceived = dividendReceived,
          )
        )

        adapter.updateData(transactionDataList.sortedBy { transactionData ->
          transactionData.date
        })
      }
    })
  }
}
