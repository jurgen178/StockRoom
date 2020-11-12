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

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.thecloudsite.stockroom.treemap.AndroidMapItem
import com.thecloudsite.stockroom.treemap.TreeModel
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import kotlinx.android.synthetic.main.fragment_treemap.view.treemap_view
import okhttp3.internal.toHexString
import java.text.DecimalFormat

class StockRoomTreemapFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = StockRoomTreemapFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_treemap, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    //val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    //val adapter = StockRoomTreemapListAdapter(requireContext(), clickListenerSummary)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItemSet ->
        updateTreemap(view, stockItemSet.stockItems)
      }
    })

    // Rotating device keeps sending alerts.
    // State changes of the lifecycle trigger the notification.
    stockRoomViewModel.resetAlerts()
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        stockRoomViewModel.runOnlineTaskNow("Request to get online data manually.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getColorStr(color: Int): String {
    val hexStr = "0x${color.toHexString()}"
    return hexStr.replace("0xff", "#")
  }

  private fun updateTreemap(
    view: View,
    stockItems: List<StockItem>
  ) {
    val treemapView = view.treemap_view

    // Gets displayed if no items are added to the root item.
    val noAssetsStr = context?.getString(R.string.no_assets)
    val rootItem = AndroidMapItem(1.0, noAssetsStr, "", "", 0)
    val treeModel = TreeModel(rootItem)

    stockItems.forEach { stockItem ->
      val (totalQuantity, totalPrice) = getAssets(stockItem.assets)

      if (totalQuantity > 0.0) {
        val assets = totalQuantity * stockItem.onlineMarketData.marketPrice
        val color = if (stockItem.stockDBdata.groupColor != 0) {
          stockItem.stockDBdata.groupColor
        } else {
          context?.getColor(R.color.backgroundListColor)
        }

        val assetChange = getAssetChange(
            totalQuantity,
            totalPrice,
            stockItem.onlineMarketData.marketPrice,
            stockItem.onlineMarketData.postMarketData,
            Color.DKGRAY,
            requireContext()
        ).first

        treeModel.addChild(
            TreeModel(
                AndroidMapItem(
                    assets,
                    stockItem.stockDBdata.symbol,
                    DecimalFormat("0.00").format(assets),
                    assetChange,
                    color
                )
            )
        )
      }
    }

    treemapView.setTreeModel(treeModel)
    treemapView.setOnClickCallback { symbol ->
      val intent = Intent(context, StockDataActivity::class.java)
      intent.putExtra("symbol", symbol)
      startActivity(intent)
    }

    treemapView.invalidate()
  }
}
