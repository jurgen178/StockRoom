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

package com.thecloudsite.stockroom

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
      it.shares
    }

    var asset: Double = 0.0
    if (shares > 0.0) {
      asset = current.assets.sumByDouble {
        it.shares * it.price
      }
    }

    if (current.onlineMarketData.marketPrice > 0.0) {
      holder.summaryListItemMarketPrice.text =
        if (current.onlineMarketData.marketPrice > 5.0) {
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

      var capital: Double = 0.0

      if (shares > 0.0) {
        capital = current.assets.sumByDouble {
          it.shares * current.onlineMarketData.marketPrice
        }

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

      if (asset > 0.0) {
        holder.summaryListItemCapital.text = DecimalFormat("0.00").format(asset)
      } else {
        holder.summaryListItemCapital.text = ""
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
