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

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.italic
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getMarketValues
import kotlinx.android.synthetic.main.stockroomtile_item.view.stockRoomTileItemLayout
import java.text.DecimalFormat

class StockRoomTileAdapter internal constructor(
  val context: Context,
  private val clickListenerListItem: (StockItem) -> Unit
) : RecyclerView.Adapter<StockRoomTileAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockItems: MutableList<StockItem> = mutableListOf()

  class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      itemView.stockRoomTileItemLayout.setOnClickListener { clickListener(stockItem) }
    }

    val itemSymbol: TextView = itemView.findViewById(R.id.stockRoomTileItemSymbol)
    val itemMarketPrice: TextView =
      itemView.findViewById(R.id.stockRoomTileItemMarketPrice)
    val itemMarketChange: TextView =
      itemView.findViewById(R.id.stockRoomTileItemMarketChange)
    val itemCapital: TextView =
      itemView.findViewById(R.id.stockRoomTileItemCapital)
    val itemAssetChange: TextView =
      itemView.findViewById(R.id.stockRoomTileItemAssetChange)
    val itemGroup: TextView = itemView.findViewById(R.id.stockRoomTileItemGroup)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.stockroomtile_item, parent, false)
    return OnlineDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current = stockItems[position]

    holder.bind(current, clickListenerListItem)

    holder.itemSymbol.text = current.stockDBdata.symbol

    var color = current.stockDBdata.groupColor
    if (color == 0) {
      color = context.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(holder.itemGroup, color)

    val (quantity, asset) = getAssets(current.assets)

//    val quantity = current.assets.sumByDouble {
//      it.quantity
//    }
//
//    var asset: Double = 0.0
//    if (quantity > 0.0) {
//      asset = current.assets.sumByDouble {
//        it.quantity * it.price
//      }
//    }

    if (current.onlineMarketData.marketPrice > 0.0) {
      val marketValues = getMarketValues(current.onlineMarketData)

      val marketChange = "${marketValues.second} ${marketValues.third}"

      if (current.onlineMarketData.postMarketData) {
        holder.itemMarketPrice.text = SpannableStringBuilder()
            .italic { append(marketValues.first) }
        holder.itemMarketChange.text = SpannableStringBuilder()
            .italic { append(marketChange) }
      } else {
        holder.itemMarketPrice.text = marketValues.first
        holder.itemMarketChange.text = marketChange
      }

      var capital: Double = 0.0

      if (quantity > 0.0) {
        capital = quantity * current.onlineMarketData.marketPrice
//        capital = current.assets.sumByDouble {
//          it.quantity * current.onlineMarketData.marketPrice
//        }

        holder.itemCapital.text = DecimalFormat("0.00").format(capital)
      } else {
        // Don't own any quantity of this stock.
        holder.itemCapital.text = ""
      }

      holder.itemAssetChange.text =
        getAssetChange(
            current.assets,
            current.onlineMarketData.marketPrice,
            current.onlineMarketData.postMarketData,
            Color.DKGRAY,
            context
        ).second
    } else {
      // offline
      holder.itemMarketPrice.text = ""
      holder.itemMarketChange.text = ""
      holder.itemAssetChange.text = ""

      if (asset > 0.0) {
        holder.itemCapital.text = DecimalFormat("0.00").format(asset)
      } else {
        holder.itemCapital.text = ""
      }
    }
  }

  fun updateData(stockItemSet: StockItemSet) {
    // Using allDataReady the list is updated only if all data sources are ready
    // which can take a few seconds because of the slow online data.
    // Without this check, the list is filled instantly, but might be reshuffled
    // for sorting when the online data is ready.

    //if (stockItemSet.allDataReady) {
    stockItems = stockItemSet.stockItems.toMutableList()
    notifyDataSetChanged()
    //}
  }

  override fun getItemCount() = stockItems.size
}
