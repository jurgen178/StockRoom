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
import androidx.lifecycle.Observer

class StockRoomPortfolioTransactionsFragment : StockRoomBaseTransactionsFragment() {

  companion object {
    fun newInstance() = StockRoomPortfolioTransactionsFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items.let { stockItems ->

        transactionDataList.clear()
        assetBought = 0
        assetSold = 0
        dividendReceived = 0

        stockItems.forEach { stockItem ->

          // Asset bought
          addAssetsBought(stockItem.assets)

          // Asset sold
          addAssetsSold(stockItem.assets)

          // Dividend received
          addDividendsReceived(stockItem.dividends)
        }

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
