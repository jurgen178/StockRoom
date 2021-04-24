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
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.StockDBdata

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

    stockRoomViewModel.allAssetTable.observe(viewLifecycleOwner, Observer { assets ->
      if (assets != null) {

        val stockMap = HashMap<String, MutableList<Asset>>()

        // Get all assets for each symbol.
        assets.forEach { asset ->
          if (!stockMap.containsKey(asset.symbol)) {
            stockMap[asset.symbol] = mutableListOf()
          }

          stockMap[asset.symbol]?.add(asset)
        }

        // Compose a stockitem list with symbol and asset list.
        val stockItemsList: List<StockItem> = stockMap.map { mapEntry ->
          StockItem(
            stockDBdata = StockDBdata(symbol = mapEntry.key),
            assets = mapEntry.value,
            events = listOf(),
            dividends = listOf(),
            onlineMarketData = OnlineMarketData(symbol = mapEntry.key)
          )
        }

        updateList(stockItemsList)
      }
    })
  }
}
