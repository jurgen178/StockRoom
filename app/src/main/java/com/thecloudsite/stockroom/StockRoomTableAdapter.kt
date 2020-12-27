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
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.StockRoomListAdapter.StockRoomViewHolder
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.StockroomTableItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getDividendStr
import com.thecloudsite.stockroom.utils.getMarketValues
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT
import kotlin.math.absoluteValue

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
      // Header item is symbol = ""
      if (current.stockDBdata.symbol.isEmpty()) {
        holder.binding.tableDataSymbol.text = getHeaderStr(context.getString(R.string.table_symbol_column))
        holder.binding.tableStockdbdataPortfolio.text = getHeaderStr("bestand")
        holder.binding.tableStockdbdataData.text = getHeaderStr("data")
        holder.binding.tableStockdbdataGroupColor.text = getHeaderStr("groupColor")
        holder.binding.tableStockdbdataNote.text = getHeaderStr("note")
        holder.binding.tableStockdbdataDividendNote.text = getHeaderStr("dividendNote")
        holder.binding.tableStockdbdataAnnualDividendRate.text = getHeaderStr("annualDividendRate")
        holder.binding.tableStockdbdataAlertAbove.text = getHeaderStr("alertAbove")
        holder.binding.tableStockdbdataAlertAboveNote.text = getHeaderStr("alertAboveNote")
        holder.binding.tableStockdbdataAlertBelow.text = getHeaderStr("alertBelow")
        holder.binding.tableStockdbdataAlertBelowNote.text = getHeaderStr("alertBelowNote")
      } else {
        holder.bindSummary(current, clickListenerSummary)

        holder.binding.tableDataSymbol.text = current.stockDBdata.symbol
        holder.binding.tableStockdbdataAlertBelow.text = current.stockDBdata.alertBelow.toString()
      }
    }
  }

  private fun getHeaderStr(text: String): SpannableStringBuilder =
    SpannableStringBuilder()
        .color(Color.BLUE) {
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
