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

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.databinding.StockroomTileItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getMarketValues
import java.text.DecimalFormat

class StockRoomTile1Adapter internal constructor(
  val context: Context,
  private val clickListenerListItemLambda: (StockItem) -> Unit
) : RecyclerView.Adapter<StockRoomTile1Adapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockItems: List<StockItem> = listOf()

  class OnlineDataViewHolder(
    val binding: StockroomTileItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
      stockItem: StockItem,
      clickListenerLambda: (StockItem) -> Unit
    ) {
      binding.stockRoomTileItemLayout.setOnClickListener { clickListenerLambda(stockItem) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {

    val binding = StockroomTileItemBinding.inflate(inflater, parent, false)
    return OnlineDataViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current = stockItems[position]

    holder.bind(current, clickListenerListItemLambda)
    holder.binding.stockRoomTileItemSymbol.text = current.stockDBdata.symbol

    var color = current.stockDBdata.groupColor
    if (color == 0) {
      color = context.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(holder.binding.stockRoomTileItemGroup, color)

    val (quantity, asset, commission) = getAssets(current.assets)

    if (current.onlineMarketData.marketPrice > 0.0) {
      val marketValues = getMarketValues(current.onlineMarketData)
      val marketChange = "${marketValues.second} ${marketValues.third}"

      if (current.onlineMarketData.postMarketData) {
        holder.binding.stockRoomTileItemMarketPrice.text = SpannableStringBuilder()
            .italic { append(marketValues.first) }
        holder.binding.stockRoomTileItemMarketChange.text = SpannableStringBuilder()
            .italic { append(marketChange) }
      } else {
        holder.binding.stockRoomTileItemMarketPrice.text = marketValues.first
        holder.binding.stockRoomTileItemMarketChange.text = marketChange
      }

      if (quantity > 0.0) {
        val capital = quantity * current.onlineMarketData.marketPrice
//        capital = current.assets.sumOf {
//          it.quantity * current.onlineMarketData.marketPrice
//        }

        val capitalStr = SpannableStringBuilder().bold {
          append(DecimalFormat(DecimalFormat2Digits).format(capital))
        }
        capitalStr.scale(currencyScale) { append(getCurrency(current.onlineMarketData)) }

        holder.binding.stockRoomTileItemCapital.text = capitalStr
      } else {
        // Don't own any quantity of this stock.
        holder.binding.stockRoomTileItemCapital.text = ""
      }

      holder.binding.stockRoomTileItemAssetChange.text =
        getAssetChange(
            quantity,
            asset,
            current.onlineMarketData.marketPrice,
            current.onlineMarketData.postMarketData,
            Color.DKGRAY,
            context
        ).second
    } else {
      // offline
      holder.binding.stockRoomTileItemMarketPrice.text = ""
      holder.binding.stockRoomTileItemMarketChange.text = ""
      holder.binding.stockRoomTileItemAssetChange.text = ""

      // Show asset instead of capital.
      if (asset > 0.0) {
        holder.binding.stockRoomTileItemCapital.text = SpannableStringBuilder().bold {
          append(DecimalFormat(DecimalFormat2Digits).format(asset))
        }
      } else {
        holder.binding.stockRoomTileItemCapital.text = ""
      }
    }
  }

  fun updateData(stockItems: List<StockItem>) {
    this.stockItems = stockItems
    notifyDataSetChanged()
  }

  override fun getItemCount() = stockItems.size
}
