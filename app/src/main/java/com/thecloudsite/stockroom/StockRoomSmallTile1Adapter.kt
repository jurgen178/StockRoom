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
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.databinding.StockroomSmalltile1ItemBinding
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getMarketValues

class StockRoomSmallTile1Adapter internal constructor(
  val context: Context,
  private val clickListenerSymbolLambda: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomSmallTile1Adapter.StockRoomViewHolder>(
  StockRoomDiffCallback()
) {

  private val inflater: LayoutInflater = LayoutInflater.from(context)

  class StockRoomViewHolder(
    val binding: StockroomSmalltile1ItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindOnClickListener(
      stockItem: StockItem,
      clickListenerLambda: (StockItem) -> Unit
    ) {
      binding.smalltileItemLayout.setOnClickListener { clickListenerLambda(stockItem) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomViewHolder {

    val binding = StockroomSmalltile1ItemBinding.inflate(inflater, parent, false)
    return StockRoomViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: StockRoomViewHolder,
    position: Int
  ) {
    val current = getItem(position)
    if (current != null) {
      holder.bindOnClickListener(current, clickListenerSymbolLambda)

      holder.binding.smalltileItemLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))
      holder.binding.smalltileTextViewSymbol.text = current.onlineMarketData.symbol

      if (current.onlineMarketData.marketPrice > 0.0) {
        val marketValues = getMarketValues(current.onlineMarketData)
        val marketPriceStr = "${marketValues.first} ${marketValues.second} ${marketValues.third}"

        if (current.onlineMarketData.postMarketData) {
          holder.binding.smalltileTextViewMarketPrice.text = SpannableStringBuilder()
            .italic { append(marketPriceStr) }
        } else {
          holder.binding.smalltileTextViewMarketPrice.text = marketPriceStr
        }
      } else {
        holder.binding.smalltileTextViewMarketPrice.text = ""
      }

      // set the background color to the market change
      val backgroundColor = getChangeColor(
        current.onlineMarketData.marketChange,
        current.onlineMarketData.postMarketData,
        context.getColor(color.backgroundListColor),
        context
      )

      holder.binding.smalltileTextViewSymbolLayout.setBackgroundColor(backgroundColor)
      // holder.binding.smalltileTextViewMarketPriceLayout.setBackgroundColor(backgroundColor)

      var color = current.stockDBdata.groupColor
      if (color == 0) {
        color = context.getColor(R.color.backgroundListColor)
      }
      setBackgroundColor(holder.binding.smalltileItemviewGroup, color)
    }
  }

  internal fun setStockItems(stockItems: List<StockItem>) {
    submitList(stockItems)
    notifyDataSetChanged()
  }
}
