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
import com.thecloudsite.stockroom.database.Dividend

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/

class DividendTimelineFragment : DividendBaseTimelineFragment() {

  companion object {
    fun newInstance() = DividendTimelineFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // allStockItems is the filtered list of the current portfolio
    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { stockItems ->
      if (stockItems != null) {
        val dividendList: MutableList<Dividend> = mutableListOf()

        stockItems.forEach { stockItem ->
          dividendList.addAll(stockItem.dividends)
        }

        updateDividends(dividendList)
      }
    })
  }
}
