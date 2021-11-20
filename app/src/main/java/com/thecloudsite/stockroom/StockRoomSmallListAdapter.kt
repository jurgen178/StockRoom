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
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.databinding.StockroomSmalllistItemBinding
import com.thecloudsite.stockroom.utils.*

class StockRoomSmallListAdapter internal constructor(
    val context: Context,
    private val clickListenerSymbolLambda: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomSmallListAdapter.StockRoomViewHolder>(
    StockRoomDiffCallback()
) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var defaultTextColor: Int? = null

    class StockRoomViewHolder(
        val binding: StockroomSmalllistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem) -> Unit
        ) {
            binding.smalllistItemLayout.setOnClickListener { clickListenerLambda(stockItem) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StockRoomViewHolder {

        val binding = StockroomSmalllistItemBinding.inflate(inflater, parent, false)
        return StockRoomViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StockRoomViewHolder,
        position: Int
    ) {
        val current = getItem(position)
        if (current != null) {
            if (defaultTextColor == null) {
                defaultTextColor = holder.binding.smalllistTextViewMarketPrice.currentTextColor
            }

            holder.bindOnClickListener(current, clickListenerSymbolLambda)

            holder.binding.smalllistItemLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))
            holder.binding.smalllistTextViewSymbol.text = current.onlineMarketData.symbol

            if (current.onlineMarketData.marketPrice > 0.0) {
                val marketValues = getMarketValues(current.onlineMarketData)
                val marketPriceStr =
                    "${marketValues.first} ${marketValues.second} ${marketValues.third}"

                if (current.onlineMarketData.postMarketData) {
                    holder.binding.smalllistTextViewMarketPrice.text = SpannableStringBuilder()
                        .italic { append(marketPriceStr) }
                } else {
                    holder.binding.smalllistTextViewMarketPrice.text = marketPriceStr
                }
            } else {
                holder.binding.smalllistTextViewMarketPrice.text = ""
            }

            val assetChange =
                getAssetChange(
                    current.assets,
                    current.onlineMarketData.marketPrice,
                    current.onlineMarketData.postMarketData,
                    Color.DKGRAY,
                    context,
                    false
                )

            val changeText = assetChange.colorChangeStr
            val dividendStr = getDividendStr(current, context)
            if (assetChange.changeStr.isNotEmpty() && dividendStr.isNotEmpty()) {
                changeText.append("\n")
            }
            holder.binding.smalllistTextViewChange.text =
                changeText.append(dividendStr)

            // set the background color to the market change
            holder.binding.smalllistTextViewMarketPrice.setBackgroundColor(
                getChangeColor(
                    current.onlineMarketData.marketChange,
                    current.onlineMarketData.postMarketData,
                    context.getColor(color.backgroundListColor),
                    context
                )
            )

            if (useWhiteOnRedGreen && current.onlineMarketData.marketChange != 0.0) {
                holder.binding.smalllistTextViewMarketPrice.setTextColor(Color.WHITE)
            } else {
                holder.binding.smalllistTextViewMarketPrice.setTextColor(defaultTextColor!!)
            }

//      // Set the background color to the market change.
//      holder.itemViewMarketPriceLayout.setBackgroundColor(
//          getChangeColor(assetChange.third, context)
//      )

            var color = current.stockDBdata.groupColor
            if (color == 0) {
                color = context.getColor(R.color.backgroundListColor)
            }
            setGroupBackground(
                context,
                current.stockDBdata.marker,
                color,
                holder.binding.smalllistItemviewGroup,
                holder.binding.smalllistItemviewGroupSep,
                holder.binding.smalllistItemviewGroupMarker
            )
        }
    }

    internal fun setStockItems(stockItems: List<StockItem>) {
        submitList(stockItems)
        notifyDataSetChanged()
    }
}
