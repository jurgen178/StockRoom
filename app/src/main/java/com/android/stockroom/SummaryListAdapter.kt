/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.stockroom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.summarylist_item.view.summaryListItemLayout
import java.text.DecimalFormat

class SummaryListAdapter internal constructor(
  val context: Context,
  private val clickListenerListItem: (StockItem) -> Unit
) : RecyclerView.Adapter<SummaryListAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockItems: MutableList<StockItem> = mutableListOf()

  inner class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      itemView.summaryListItemLayout.setOnClickListener { clickListener(stockItem) }
    }

    val summaryListItemSymbol: TextView = itemView.findViewById(R.id.summaryListItemSymbol)
    val summaryListItemMarketPrice: TextView =
      itemView.findViewById(R.id.summaryListItemMarketPrice)
    val summaryListItemMarketChange: TextView =
      itemView.findViewById(R.id.summaryListItemMarketChange)
    val summaryListItemCapital: TextView =
      itemView.findViewById(R.id.summaryListItemCapital)
    val summaryListItemAssetChange: TextView =
      itemView.findViewById(R.id.summaryListItemAssetChange)
    val summaryListItemGroup: TextView = itemView.findViewById(R.id.summaryListItemGroup)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.summarylist_item, parent, false)
    return OnlineDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current = stockItems[position]

    holder.bind(current, clickListenerListItem)

    holder.summaryListItemSymbol.text = current.stockDBdata.symbol

    var color = current.stockDBdata.groupColor
    if (color == 0) {
      color = context.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(holder.summaryListItemGroup, color)

    val shares = current.assets.sumByDouble {
      it.shares.toDouble()
    }
        .toFloat()

    var asset: Float = 0f
    if (shares > 0f) {
      asset = current.assets.sumByDouble {
        it.shares.toDouble() * it.price
      }
          .toFloat()
    }

    if (current.onlineMarketData.marketPrice > 0f) {
      holder.summaryListItemMarketPrice.text =
        if (current.onlineMarketData.marketPrice > 5f) {
          DecimalFormat("0.00").format(current.onlineMarketData.marketPrice)
        } else {
          DecimalFormat("0.00##").format(current.onlineMarketData.marketPrice)
        }

      holder.summaryListItemMarketChange.text = "${
      DecimalFormat("0.00##").format(current.onlineMarketData.marketChange)} (${DecimalFormat(
          "0.00"
      ).format(
          current.onlineMarketData.marketChangePercent
      )}%)"

      var capital: Float = 0f

      if (shares > 0f) {
        capital = current.assets.sumByDouble {
          it.shares.toDouble() * current.onlineMarketData.marketPrice
        }
            .toFloat()

        holder.summaryListItemCapital.text = DecimalFormat("0.00").format(capital)
      } else {
        // Don't own any shares of this stock.
        holder.summaryListItemCapital.text = ""
      }

      holder.summaryListItemAssetChange.text =
        getAssetChange(current.assets, current.onlineMarketData.marketPrice, context)
    } else {
      // offline
      holder.summaryListItemMarketPrice.text = ""
      holder.summaryListItemMarketChange.text = ""
      holder.summaryListItemAssetChange.text = ""

      if (asset > 0f) {
        holder.summaryListItemCapital.text = DecimalFormat("0.00").format(asset)
      } else {
        holder.summaryListItemCapital.text = ""
      }
    }
  }

  fun updateData(stockItemSet: StockItemSet) {
    if (stockItemSet.dataValid) {
      stockItems = stockItemSet.stockItems.toMutableList()
      notifyDataSetChanged()
    }
  }

  override fun getItemCount() = stockItems.size
}
