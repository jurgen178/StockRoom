/*
 * Copyright (C) 2020
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

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StockRoomListFragment : StockRoomBaseFragment() {

  companion object {
    fun newInstance() = StockRoomListFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val clickListenerGroup =
      { stockItem: StockItem, itemView: View -> clickListenerGroup(stockItem, itemView) }
    val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    val adapter = StockRoomListAdapter(requireContext(), clickListenerGroup, clickListenerSummary)

    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

    recyclerView.adapter = adapter

    // Set column number depending on orientation.
    val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      1
    } else {
      2
    }

    recyclerView.layoutManager = GridLayoutManager(context, spanCount)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let {
        adapter.setStockItems(it)
      }
    })
  }
}
