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
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.StockroomTableItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getDividendStr
import com.thecloudsite.stockroom.utils.getMarketValues
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT
import kotlin.text.StringBuilder

class StockRoomTableAdapter internal constructor(
  val context: Context,
  private val clickListenerSummary: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomTableAdapter.StockRoomTableViewHolder>(
    StockRoomDiffCallback()
) {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockItems: MutableList<StockItem> = mutableListOf()

  class StockRoomTableViewHolder(
    val binding: StockroomTableItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindSummary(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      binding.tableDataLayout.setOnClickListener { clickListener(stockItem) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomTableViewHolder {

    val binding = StockroomTableItemBinding.inflate(inflater, parent, false)
    return StockRoomTableViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: StockRoomTableViewHolder,
    position: Int
  ) {
    val current = getItem(position)

    if (current != null) {
      holder.bindSummary(current, clickListenerSummary)

      // Header item is symbol = ""
      if (current.stockDBdata.symbol.isEmpty()) {

        holder.binding.tableDataLayout.setBackgroundColor(Color.LTGRAY)

        setBackgroundColor(holder.binding.tableDataGroup, Color.TRANSPARENT)
        holder.binding.tableDataMarketPrice.setBackgroundColor(Color.LTGRAY)
        holder.binding.tableDataMarketChange.setBackgroundColor(Color.LTGRAY)

        holder.binding.tableDataSymbol.text =
          getHeaderStr(context.getString(R.string.table_column_symbol))
        holder.binding.tableDataName.text =
          getHeaderStr(context.getString(R.string.table_column_Name))
        holder.binding.tableDataMarketPrice.text =
          getHeaderStr(context.getString(R.string.table_column_MarketPrice))
        holder.binding.tableDataMarketChange.text =
          getHeaderStr(context.getString(R.string.table_column_MarketChange))
        holder.binding.tableDataQuantity.text =
          getHeaderStr(context.getString(R.string.table_column_Quantity))
        holder.binding.tableDataPurchaseprice.text =
          getHeaderStr(context.getString(R.string.table_column_Purchaseprice))
        holder.binding.tableDataAsset.text =
          getHeaderStr(context.getString(R.string.table_column_Asset))
        holder.binding.tableDataAssetChange.text =
          getHeaderStr(context.getString(R.string.table_column_AssetChange))
        holder.binding.tableDataDividend.text =
          getHeaderStr(context.getString(R.string.table_column_Dividend))
        holder.binding.tableDataAlertAbove.text =
          getHeaderStr(context.getString(R.string.table_column_AlertAbove))
        holder.binding.tableDataAlertBelow.text =
          getHeaderStr(context.getString(R.string.table_column_AlertBelow))
        holder.binding.tableDataEvents.text =
          getHeaderStr(context.getString(R.string.table_column_Events))
        holder.binding.tableDataNote.text =
          getHeaderStr(context.getString(R.string.table_column_Note))
      } else {

        holder.binding.tableDataLayout.setBackgroundColor(
            context.getColor(R.color.backgroundListColor)
        )

        var color = current.stockDBdata.groupColor
        if (color == 0) {
          color = context.getColor(R.color.backgroundListColor)
        }
        setBackgroundColor(holder.binding.tableDataGroup, color)

        holder.binding.tableDataSymbol.text = current.stockDBdata.symbol
        holder.binding.tableDataName.text = getName(current.onlineMarketData)

        val (quantity, asset) = getAssets(current.assets)

        if (quantity > 0.0 && asset > 0.0) {
          holder.binding.tableDataPurchaseprice.text =
            DecimalFormat(DecimalFormat2Digits).format(asset)
          holder.binding.tableDataQuantity.text =
            "${DecimalFormat(DecimalFormat0To4Digits).format(quantity)}@${
              DecimalFormat(DecimalFormat2To4Digits).format(
                  asset / quantity
              )
            }"

          val purchasePrice = quantity * current.onlineMarketData.marketPrice
          holder.binding.tableDataAsset.text = DecimalFormat(
              DecimalFormat2Digits
          ).format(purchasePrice)
        } else {
          // Don't own any quantity of this stock.
          holder.binding.tableDataPurchaseprice.text = ""
          holder.binding.tableDataQuantity.text = ""
          holder.binding.tableDataAsset.text = ""
        }

        val assetChange =
          getAssetChange(
              quantity,
              asset,
              current.onlineMarketData.marketPrice,
              current.onlineMarketData.postMarketData,
              Color.DKGRAY,
              context,
              false
          )
        holder.binding.tableDataAssetChange.text = assetChange.second

        if (current.onlineMarketData.marketPrice > 0.0) {
          val marketValues = getMarketValues(current.onlineMarketData)
          val marketChange = "${marketValues.second} ${marketValues.third}"

          if (current.onlineMarketData.postMarketData) {
            holder.binding.tableDataMarketPrice.text = SpannableStringBuilder()
                .italic { append(marketValues.first) }
            holder.binding.tableDataMarketChange.text = SpannableStringBuilder()
                .italic { append(marketChange) }
          } else {
            holder.binding.tableDataMarketPrice.text = marketValues.first
            holder.binding.tableDataMarketChange.text = marketChange
          }
        } else {
          holder.binding.tableDataMarketPrice.text = ""
          holder.binding.tableDataMarketChange.text = ""
        }

        val marketColor = getChangeColor(
            current.onlineMarketData.marketChange,
            current.onlineMarketData.postMarketData,
            context.getColor(R.color.backgroundListColor),
            context
        )
        holder.binding.tableDataMarketPrice.setBackgroundColor(marketColor)
        holder.binding.tableDataMarketChange.setBackgroundColor(marketColor)

        holder.binding.tableDataDividend.text = getDividendStr(current)

        var alertAboveText = ""
        if (current.stockDBdata.alertAbove > 0.0) {
          alertAboveText = DecimalFormat(
              DecimalFormat2To4Digits
          ).format(current.stockDBdata.alertAbove)
          if (current.stockDBdata.alertAboveNote.isNotEmpty()) {
            alertAboveText += "\n${current.stockDBdata.alertAboveNote}"
          }
        }
        holder.binding.tableDataAlertAbove.text = alertAboveText

        var alertBelowText = ""
        if (current.stockDBdata.alertBelow > 0.0) {
          alertBelowText = DecimalFormat(
              DecimalFormat2To4Digits
          ).format(current.stockDBdata.alertBelow)
          if (current.stockDBdata.alertBelowNote.isNotEmpty()) {
            alertBelowText += "\n${current.stockDBdata.alertBelowNote}"
          }
        }

        holder.binding.tableDataAlertBelow.text = alertBelowText

        holder.binding.tableDataEvents.text = if (current.events.isNotEmpty()) {
          val events: StringBuilder = StringBuilder()
          val count = current.events.size
          val eventStr =
            context.resources.getQuantityString(R.plurals.events_in_list, count, count)

          events.append(eventStr)
          current.events.forEach {
            val localDateTime = LocalDateTime.ofEpochSecond(it.datetime, 0, ZoneOffset.UTC)
            val datetime = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
            events.append(
                "\n${
                  context.getString(
                      R.string.event_datetime_format, it.title, datetime
                  )
                }"
            )
          }

          events.toString()
        } else {
          ""
        }

        holder.binding.tableDataNote.text = current.stockDBdata.note
      }
    }
  }

  private fun getHeaderStr(text: String): SpannableStringBuilder =
    SpannableStringBuilder()
        .color(Color.WHITE) {
          bold { append(text) }
        }

  internal fun setStockItems(stockItems: List<StockItem>) {
    this.stockItems = mutableListOf(
        StockItem(
            onlineMarketData = OnlineMarketData(symbol = ""),
            stockDBdata = StockDBdata(symbol = ""),
            assets = emptyList(),
            events = emptyList(),
            dividends = emptyList()
        )
    )

    this.stockItems.addAll(stockItems)

    submitList(this.stockItems)
    notifyDataSetChanged()
  }
}
