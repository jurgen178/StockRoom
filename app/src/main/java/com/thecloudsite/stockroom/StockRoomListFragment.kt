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
import androidx.recyclerview.widget.GridLayoutManager
import kotlin.math.roundToInt

class StockRoomListFragment : StockRoomBaseFragment() {

    companion object {
        fun newInstance() = StockRoomListFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val clickListenerGroupLambda =
            { stockItem: StockItem, itemView: View -> clickListenerGroup(stockItem, itemView) }
        val clickListenerMarkerLambda =
            { stockItem: StockItem, itemView: View ->
                clickListenerMarker(
                    stockItem,
                    itemView
                )
            }
        val clickListenerSymbolLambda = { stockItem: StockItem -> clickListenerSymbol(stockItem) }
        val adapter = StockRoomListAdapter(
            requireContext(),
            clickListenerGroupLambda,
            clickListenerMarkerLambda,
            clickListenerSymbolLambda
        )

        val recyclerView = binding.recyclerview
        recyclerView.adapter = adapter

        // Set column number depending on screen width.
        val scale = 494
        val spanCount =
            (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

        recyclerView.layoutManager = GridLayoutManager(
            context,
            Integer.min(Integer.max(spanCount, 1), 10)
        )

        stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
            items?.let {
                adapter.setStockItems(it)
            }
        })
    }
}
