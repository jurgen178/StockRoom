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

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/

class GainLossAllTimelineFragment : GainLossBaseTimelineFragment() {

  companion object {
    fun newInstance() = GainLossAllTimelineFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

//    // all DB entries unfiltered
//    // allStockItemsDB compose the StockItem at runtime
//    stockRoomViewModel.allStockItemsDB.observe(viewLifecycleOwner, Observer { stockItemsDB ->
//      if (stockItemsDB != null) {
//        updateList(stockItemsDB)
//      }
//    })

    stockRoomViewModel.allAssets.observe(viewLifecycleOwner, Observer { assets ->
      if (assets != null) {

        val stockItemList = assets.map { item ->
          StockItem(
            stockDBdata = item.stockDBdata,
            assets = item.assets,
            events = listOf(),
            dividends = listOf(),
            onlineMarketData = OnlineMarketData(symbol = item.stockDBdata.symbol)
          )
        }

        updateList(stockItemList)
      }
    })
  }
}
