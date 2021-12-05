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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.backgroundColor
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.StockroomTableItemBinding
import com.thecloudsite.stockroom.utils.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import kotlin.math.absoluteValue

enum class TableSortMode {
    Unsorted,
    BySymbolUp,
    BySymbolDown,
    ByNameUp,
    ByNameDown,
    ByMarketPriceUp,
    ByMarketPriceDown,
    ByMarketChangeUp,
    ByMarketChangeDown,
    ByMarketCurrencyUp,
    ByMarketCurrencyDown,
    ByQuantityUp,
    ByQuantityDown,
    ByPurchasepriceUp,
    ByPurchasepriceDown,
    ByAssetUp,
    ByAssetDown,
    ByAssetChangeUp,
    ByAssetChangeDown,
    ByAssetFeeUp,
    ByAssetFeeDown,
    ByAssetTotalFeeUp,
    ByAssetTotalFeeDown,
    ByDividendUp,
    ByDividendDown,
    ByAlertBelowUp,
    ByAlertBelowDown,
    ByAlertAboveUp,
    ByAlertAboveDown,
    ByAssetsUp,
    ByAssetsDown,
    ByEventsUp,
    ByEventsDown,
    ByNoteUp,
    ByNoteDown,
}

class StockRoomTableAdapter internal constructor(
    val context: Context,
    private val clickListenerSymbolLambda: (StockItem) -> Unit
) : RecyclerView.Adapter<StockRoomTableAdapter.StockRoomTableViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var stockItems: MutableList<StockItem> = mutableListOf()
    private var stockItemsCopy: List<StockItem> = mutableListOf()
    private var defaultTextColor: Int? = null
    private var tableSortmode = TableSortMode.Unsorted

    class StockRoomTableViewHolder(
        val binding: StockroomTableItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem) -> Unit
        ) {
            binding.tableDataLayout.setOnClickListener { clickListenerLambda(stockItem) }
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
        val current = this.stockItems[position]

        if (defaultTextColor == null) {
            defaultTextColor = holder.binding.tableDataMarketPrice.currentTextColor
        }

        holder.bindOnClickListener(current, clickListenerSymbolLambda)

        // Header item is symbol = ""
        val isHeader = current.stockDBdata.symbol.isEmpty()

        val alignmentNumbers = if (isHeader) {
            Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.END or Gravity.TOP
        }
        holder.binding.tableDataMarketPrice.gravity = alignmentNumbers
        holder.binding.tableDataMarketChange.gravity = alignmentNumbers
        holder.binding.tableDataMarketCurrency.gravity = alignmentNumbers
        holder.binding.tableDataQuantity.gravity = alignmentNumbers
        holder.binding.tableDataPurchaseprice.gravity = alignmentNumbers
        holder.binding.tableDataAsset.gravity = alignmentNumbers
        holder.binding.tableDataAssetChange.gravity = alignmentNumbers
        holder.binding.tableDataAssetFee.gravity = alignmentNumbers
        holder.binding.tableDataAssetTotalFee.gravity = alignmentNumbers
        holder.binding.tableDataDividend.gravity = alignmentNumbers
        holder.binding.tableDataAlertBelow.gravity = alignmentNumbers
        holder.binding.tableDataAlertAbove.gravity = alignmentNumbers

        val alignmentText = if (isHeader) {
            Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.START
        }
        holder.binding.tableDataSymbol.gravity = alignmentText
        holder.binding.tableDataName.gravity = alignmentText
        holder.binding.tableDataAssets.gravity = alignmentText
        holder.binding.tableDataMarketCurrency.gravity = alignmentText
        holder.binding.tableDataEvents.gravity = alignmentText
        holder.binding.tableDataNote.gravity = alignmentText

        if (isHeader) {

            holder.binding.tableDataLayout.setBackgroundColor(context.getColor(R.color.tableHeaderBackground))

            setBackgroundColor(holder.binding.tableDataGroup, Color.TRANSPARENT)
            // TextView
            holder.binding.tableDataGroupSep.setBackgroundColor(Color.TRANSPARENT)
            // TextView with gradient background set.
            setBackgroundColor(holder.binding.tableDataGroupMarker, Color.TRANSPARENT)

            holder.binding.tableDataSymbol.setOnClickListener {
                update(TableSortMode.BySymbolUp, TableSortMode.BySymbolDown)
            }
            holder.binding.tableDataSymbol.text =
                getHeaderStr(context.getString(R.string.table_column_symbol))

            holder.binding.tableDataName.setOnClickListener {
                update(TableSortMode.ByNameUp, TableSortMode.ByNameDown)
            }
            holder.binding.tableDataName.text =
                getHeaderStr(context.getString(R.string.table_column_Name))

            holder.binding.tableDataMarketPrice.setOnClickListener {
                update(TableSortMode.ByMarketPriceUp, TableSortMode.ByMarketPriceDown)
            }
            holder.binding.tableDataMarketPrice.text =
                getHeaderStr(context.getString(R.string.table_column_MarketPrice))

            holder.binding.tableDataMarketChange.setOnClickListener {
                update(TableSortMode.ByMarketChangeUp, TableSortMode.ByMarketChangeDown)
            }
            holder.binding.tableDataMarketChange.text =
                getHeaderStr(context.getString(R.string.table_column_MarketChange))

            holder.binding.tableDataMarketCurrency.setOnClickListener {
                update(TableSortMode.ByMarketCurrencyUp, TableSortMode.ByMarketCurrencyDown)
            }
            holder.binding.tableDataMarketCurrency.text =
                getHeaderStr(context.getString(R.string.table_column_MarketCurrency))

            holder.binding.tableDataQuantity.setOnClickListener {
                update(TableSortMode.ByQuantityUp, TableSortMode.ByQuantityDown)
            }
            holder.binding.tableDataQuantity.text =
                getHeaderStr(context.getString(R.string.table_column_Quantity))

            holder.binding.tableDataPurchaseprice.setOnClickListener {
                update(TableSortMode.ByPurchasepriceUp, TableSortMode.ByPurchasepriceDown)
            }
            holder.binding.tableDataPurchaseprice.text =
                getHeaderStr(context.getString(R.string.table_column_Purchaseprice))

            holder.binding.tableDataAsset.setOnClickListener {
                update(TableSortMode.ByAssetUp, TableSortMode.ByAssetDown)
            }
            holder.binding.tableDataAsset.text =
                getHeaderStr(context.getString(R.string.table_column_Asset))

            holder.binding.tableDataAssetChange.setOnClickListener {
                update(TableSortMode.ByAssetChangeUp, TableSortMode.ByAssetChangeDown)
            }
            holder.binding.tableDataAssetChange.text =
                getHeaderStr(context.getString(R.string.table_column_AssetChange))

            holder.binding.tableDataAssetFee.setOnClickListener {
                update(TableSortMode.ByAssetFeeUp, TableSortMode.ByAssetFeeDown)
            }
            holder.binding.tableDataAssetFee.text =
                getHeaderStr(context.getString(R.string.table_column_AssetFee))

            holder.binding.tableDataAssetTotalFee.setOnClickListener {
                update(
                    TableSortMode.ByAssetTotalFeeUp,
                    TableSortMode.ByAssetTotalFeeDown
                )
            }
            holder.binding.tableDataAssetTotalFee.text =
                getHeaderStr(context.getString(R.string.table_column_AssetTotalFees))

            holder.binding.tableDataDividend.setOnClickListener {
                update(TableSortMode.ByDividendUp, TableSortMode.ByDividendDown)
            }
            holder.binding.tableDataDividend.text =
                getHeaderStr(context.getString(R.string.table_column_Dividend))

            holder.binding.tableDataAlertBelow.setOnClickListener {
                update(TableSortMode.ByAlertBelowUp, TableSortMode.ByAlertBelowDown)
            }
            holder.binding.tableDataAlertBelow.text =
                getHeaderStr(context.getString(R.string.table_column_AlertBelow))

            holder.binding.tableDataAlertAbove.setOnClickListener {
                update(TableSortMode.ByAlertAboveUp, TableSortMode.ByAlertAboveDown)
            }
            holder.binding.tableDataAlertAbove.text =
                getHeaderStr(context.getString(R.string.table_column_AlertAbove))

            holder.binding.tableDataAssets.setOnClickListener {
                update(TableSortMode.ByAssetsUp, TableSortMode.ByAssetsDown)
            }
            holder.binding.tableDataAssets.text =
                getHeaderStr(context.getString(R.string.table_column_Assets))

            holder.binding.tableDataEvents.setOnClickListener {
                update(TableSortMode.ByEventsUp, TableSortMode.ByEventsDown)
            }
            holder.binding.tableDataEvents.text =
                getHeaderStr(context.getString(R.string.table_column_Events))

            holder.binding.tableDataNote.setOnClickListener {
                update(TableSortMode.ByNoteUp, TableSortMode.ByNoteDown)
            }
            holder.binding.tableDataNote.text =
                getHeaderStr(context.getString(R.string.table_column_Note))

            when (tableSortmode) {
                TableSortMode.BySymbolUp -> updateTextviewUp(holder.binding.tableDataSymbol)
                TableSortMode.BySymbolDown -> updateTextviewDown(holder.binding.tableDataSymbol)

                TableSortMode.ByNameUp -> updateTextviewUp(holder.binding.tableDataName)
                TableSortMode.ByNameDown -> updateTextviewDown(holder.binding.tableDataName)

                TableSortMode.ByMarketPriceUp -> updateTextviewUp(holder.binding.tableDataMarketPrice)
                TableSortMode.ByMarketPriceDown -> updateTextviewDown(holder.binding.tableDataMarketPrice)

                TableSortMode.ByMarketChangeUp -> updateTextviewUp(holder.binding.tableDataMarketChange)
                TableSortMode.ByMarketChangeDown -> updateTextviewDown(holder.binding.tableDataMarketChange)

                TableSortMode.ByMarketCurrencyUp -> updateTextviewUp(holder.binding.tableDataMarketCurrency)
                TableSortMode.ByMarketCurrencyDown -> updateTextviewDown(holder.binding.tableDataMarketCurrency)

                TableSortMode.ByQuantityUp -> updateTextviewUp(holder.binding.tableDataQuantity)
                TableSortMode.ByQuantityDown -> updateTextviewDown(holder.binding.tableDataQuantity)

                TableSortMode.ByPurchasepriceUp -> updateTextviewUp(holder.binding.tableDataPurchaseprice)
                TableSortMode.ByPurchasepriceDown -> updateTextviewDown(holder.binding.tableDataPurchaseprice)

                TableSortMode.ByAssetUp -> updateTextviewUp(holder.binding.tableDataAsset)
                TableSortMode.ByAssetDown -> updateTextviewDown(holder.binding.tableDataAsset)

                TableSortMode.ByAssetChangeUp -> updateTextviewUp(holder.binding.tableDataAssetChange)
                TableSortMode.ByAssetChangeDown -> updateTextviewDown(holder.binding.tableDataAssetChange)

                TableSortMode.ByAssetFeeUp -> updateTextviewUp(holder.binding.tableDataAssetFee)
                TableSortMode.ByAssetFeeDown -> updateTextviewDown(holder.binding.tableDataAssetFee)

                TableSortMode.ByAssetTotalFeeUp -> updateTextviewUp(holder.binding.tableDataAssetTotalFee)
                TableSortMode.ByAssetTotalFeeDown -> updateTextviewDown(holder.binding.tableDataAssetTotalFee)

                TableSortMode.ByDividendUp -> updateTextviewUp(holder.binding.tableDataDividend)
                TableSortMode.ByDividendDown -> updateTextviewDown(holder.binding.tableDataDividend)

                TableSortMode.ByAlertBelowUp -> updateTextviewUp(holder.binding.tableDataAlertBelow)
                TableSortMode.ByAlertBelowDown -> updateTextviewDown(holder.binding.tableDataAlertBelow)

                TableSortMode.ByAlertAboveUp -> updateTextviewUp(holder.binding.tableDataAlertAbove)
                TableSortMode.ByAlertAboveDown -> updateTextviewDown(holder.binding.tableDataAlertAbove)

                TableSortMode.ByAssetsUp -> updateTextviewUp(holder.binding.tableDataAssets)
                TableSortMode.ByAssetsDown -> updateTextviewDown(holder.binding.tableDataAssets)

                TableSortMode.ByEventsUp -> updateTextviewUp(holder.binding.tableDataEvents)
                TableSortMode.ByEventsDown -> updateTextviewDown(holder.binding.tableDataEvents)

                TableSortMode.ByNoteUp -> updateTextviewUp(holder.binding.tableDataNote)
                TableSortMode.ByNoteDown -> updateTextviewDown(holder.binding.tableDataNote)

                // case: unsorted
                else -> {
                }
            }
        } else {

            val backgroundColor = context.getColor(R.color.backgroundListColor)
            holder.binding.tableDataLayout.setBackgroundColor(backgroundColor)

            var color = current.stockDBdata.groupColor
            if (color == 0) {
                color = backgroundColor
            }
            setGroupBackground(
                context,
                current.stockDBdata.marker,
                color,
                holder.binding.tableDataGroup,
                holder.binding.tableDataGroupSep,
                holder.binding.tableDataGroupMarker
            )

            var displayName = current.stockDBdata.symbol
            if (current.stockDBdata.name.isNotEmpty()) {
                displayName += "\n(${current.stockDBdata.name})"
            }
            holder.binding.tableDataSymbol.text = displayName
            holder.binding.tableDataName.text = getName(current.onlineMarketData)

            val (quantity, asset, fee) = getAssets(current.assets)

            if (quantity > 0.0) {
                holder.binding.tableDataPurchaseprice.text =
                    DecimalFormat(DecimalFormat2Digits).format(asset + fee)

                val tableDataQuantity = SpannableStringBuilder()
                tableDataQuantity.append(
                    "${DecimalFormat(DecimalFormatQuantityDigits).format(quantity)}@${
                        to2To8Digits(asset / quantity)
                    }"
                )
                if (fee > 0.0) {
                    tableDataQuantity.scale(feeScale) {
                        append(
                            "+${DecimalFormat(DecimalFormat2To4Digits).format(fee)}"
                        )
                    }
                }
                holder.binding.tableDataQuantity.text = tableDataQuantity

                holder.binding.tableDataAsset.text =
                    if (quantity > 0.0 && current.onlineMarketData.marketPrice > 0.0) {
                        DecimalFormat(
                            DecimalFormat2Digits
                        ).format(quantity * current.onlineMarketData.marketPrice)
                    } else {
                        ""
                    }
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
            holder.binding.tableDataAssetChange.text = assetChange.displayColorStr
            holder.binding.tableDataAssetFee.text =
                if (fee > 0.0) {
                    DecimalFormat(
                        DecimalFormat2To4Digits
                    ).format(fee)
                } else {
                    ""
                }

            val totalFee = getTotalFee(current.assets)
            holder.binding.tableDataAssetTotalFee.text =
                if (totalFee > 0.0) {
                    DecimalFormat(
                        DecimalFormat2To4Digits
                    ).format(totalFee)
                } else {
                    ""
                }
            holder.binding.tableDataMarketCurrency.text = getCurrency(current.onlineMarketData)

            if (current.onlineMarketData.marketPrice > 0.0) {

                val marketColor = getChangeColor(
                    current.onlineMarketData.marketChange,
                    current.onlineMarketData.postMarketData,
                    defaultTextColor!!,
                    context
                )

                val marketValues = getMarketValues(current.onlineMarketData)
                val marketChange = "${marketValues.second} ${marketValues.third}"

                if (current.onlineMarketData.postMarketData) {
                    holder.binding.tableDataMarketPrice.text = SpannableStringBuilder()
                        .color(marketColor)
                        { italic { append(marketValues.first) } }
                    holder.binding.tableDataMarketChange.text = SpannableStringBuilder()
                        .color(marketColor)
                        { italic { append(marketChange) } }
                } else {
                    holder.binding.tableDataMarketPrice.text = SpannableStringBuilder()
                        .color(marketColor)
                        { append(marketValues.first) }
                    holder.binding.tableDataMarketChange.text = SpannableStringBuilder()
                        .color(marketColor)
                        { append(marketChange) }
                }
            } else {
                holder.binding.tableDataMarketPrice.text = ""
                holder.binding.tableDataMarketChange.text = ""
            }

//        val marketColor = getChangeColor(
//            current.onlineMarketData.marketChange,
//            current.onlineMarketData.postMarketData,
//            context.getColor(R.color.backgroundListColor),
//            context
//        )
//        holder.binding.tableDataMarketPrice.setBackgroundColor(marketColor)
//        holder.binding.tableDataMarketChange.setBackgroundColor(marketColor)

            val dividendStr = SpannableStringBuilder().append(getDividendStr(current))

            val dividendRate = if (current.stockDBdata.annualDividendRate >= 0.0) {
                current.stockDBdata.annualDividendRate
            } else {
                current.onlineMarketData.annualDividendRate
            }

            if (dividendRate > 0.0 && quantity > 0.0) {
                val totalDividend = quantity * dividendRate

                val dividendCycleList = context.resources.getStringArray(array.dividend_cycles)
                val textScale = 0.75f

                dividendStr
                    .scale(textScale)
                    {
                        // monthly
                        append("\n${dividendCycleList[DividendCycleStrIndex.Monthly.value]} ")
                            .bold {
                                append(
                                    DecimalFormat(DecimalFormat2Digits).format(totalDividend / 12.0)
                                )
                            }
                            // quarterly
                            .append("\n${dividendCycleList[DividendCycleStrIndex.Quarterly.value]} ")
                            .bold {
                                append(
                                    DecimalFormat(DecimalFormat2Digits).format(totalDividend / 4.0)
                                )
                            }
                            // annual
                            .append("\n${dividendCycleList[DividendCycleStrIndex.Annual.value]} ")
                            .bold {
                                append(DecimalFormat(DecimalFormat2Digits).format(totalDividend))
                            }
                    }
            }
            holder.binding.tableDataDividend.text = dividendStr

            var alertBelowText = ""
            if (current.stockDBdata.alertBelow > 0.0) {
                alertBelowText = DecimalFormat(
                    DecimalFormat2To4Digits
                ).format(current.stockDBdata.alertBelow)
//          if (current.stockDBdata.alertBelowNote.isNotEmpty()) {
//            alertBelowText += "\n${current.stockDBdata.alertBelowNote}"
//          }
            }

            holder.binding.tableDataAlertBelow.text = alertBelowText

            var alertAboveText = ""
            if (current.stockDBdata.alertAbove > 0.0) {
                alertAboveText = DecimalFormat(
                    DecimalFormat2To4Digits
                ).format(current.stockDBdata.alertAbove)
//          if (current.stockDBdata.alertAboveNote.isNotEmpty()) {
//            alertAboveText += "\n${current.stockDBdata.alertAboveNote}"
//          }
            }
            holder.binding.tableDataAlertAbove.text = alertAboveText

            val textScale = 0.75f
            val colorNegativeAsset = context.getColor(R.color.negativeAsset)
            val colorObsoleteAsset = context.getColor(R.color.obsoleteAsset)

            val assetStr = SpannableStringBuilder()

            if (current.assets.isNotEmpty()) {
                // Sort assets in the list by date.
                val sortedList = current.assets.sortedBy { assetItem ->
                    assetItem.date
                }

                val (totalQuantity, totalPrice, _) = getAssets(
                    sortedList,
                    obsoleteAssetType
                )

                // List each asset
                sortedList.forEach { assetItem ->

                    val datetime: ZonedDateTime =
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(assetItem.date),
                            ZoneOffset.systemDefault()
                        )

                    val assetEntry = SpannableStringBuilder()
                        .scale(textScale) {
                            append(DecimalFormat(DecimalFormatQuantityDigits).format(assetItem.quantity))
                                .append("@${to2To8Digits(assetItem.price)}")
                                .append(
                                    if (assetItem.price > 0.0) {
                                        "=${
                                            DecimalFormat(DecimalFormat2Digits).format(
                                                assetItem.quantity.absoluteValue * assetItem.price
                                            )
                                        }"
                                    } else {
                                        ""
                                    }
                                )
                                .append("   ")
                                .append(
                                    datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                                )
                        }

                    if (assetItem.account.isNotEmpty()) {
                        assetEntry.scale(textScale) {
                            append(
                                "   ${
                                    context.getString(
                                        R.string.account_overview_headline,
                                        assetItem.account
                                    )
                                }"
                            )
                        }
                    }

                    if (assetItem.note.isNotEmpty()) {
                        assetEntry.scale(textScale) { append("   '${assetItem.note}'") }
                    }

                    assetEntry.scale(textScale) { append("\n") }

                    when {
                        // Sold (negative) values are in italic and colored gray.
                        assetItem.quantity < 0.0 -> {
                            assetStr.color(colorNegativeAsset) { italic { append(assetEntry) } }
                        }
                        // Obsolete entries are colored gray.
                        assetItem.type and obsoleteAssetType != 0 -> {
                            assetStr.color(colorObsoleteAsset) { append(assetEntry) }
                        }
                        else -> {
                            assetStr.append(assetEntry)
                        }
                    }
                }

                // Add summary
                val (capitalGain, capitalLoss, gainLossMap) = getAssetsCapitalGain(current.assets)
                val capitalGainLossText = getCapitalGainLossText(context, capitalGain, capitalLoss)
                assetStr.scale(textScale) {
                    append("\n${context.getString(R.string.summary_capital_gain)} ")
                        .append(capitalGainLossText)
                }

                // Add summary text
                if (totalQuantity > 0.0) {
                    var totalAssetStr =
                        "\n${
                            DecimalFormat(DecimalFormatQuantityDigits).format(totalQuantity)
                        }@${
                            to2To8Digits(totalPrice / totalQuantity)
                        }"
                    totalAssetStr += " = ${DecimalFormat(DecimalFormat2Digits).format(totalPrice)}"

                    assetStr.scale(textScale) {
                        append("\n${context.getString(R.string.asset_summary_text)}")
                            .color(Color.BLACK) {
                                backgroundColor(Color.YELLOW)
                                {
                                    append(totalAssetStr)
                                }
                            }
                    }
                }
            }

            holder.binding.tableDataAssets.text = assetStr

            // Add Events
            holder.binding.tableDataEvents.text = if (current.events.isNotEmpty()) {
                val events: SpannableStringBuilder = SpannableStringBuilder()
                val count = current.events.size
                val eventStr =
                    context.resources.getQuantityString(R.plurals.events_in_list, count, count)

                events.scale(textScale) { append(eventStr) }
                current.events.forEach {
                    val localDateTime = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(it.datetime),
                        ZoneOffset.systemDefault()
                    )
                    val datetime =
                        localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
                    events.scale(textScale) {
                        append(
                            "\n${
                                context.getString(
                                    R.string.event_datetime_format, it.title, datetime
                                )
                            }"
                        )
                    }
                }
                events
            } else {
                SpannableStringBuilder()
            }

            holder.binding.tableDataNote.text = current.stockDBdata.note
        }
    }

    override fun getItemCount() = this.stockItems.size

    private fun getHeaderStr(text: String): SpannableStringBuilder =
        SpannableStringBuilder()
            .color(Color.WHITE) {
                bold { append(text) }
            }

    private fun updateTextviewUp(textView: TextView) {
        textView.text = textView.text.toString() + " ▲"
    }

    private fun updateTextviewDown(textView: TextView) {
        textView.text = textView.text.toString() + " ▼"
    }

    internal fun setStockItems(stockItems: List<StockItem>) {
        this.stockItemsCopy = stockItems
        update(this.tableSortmode, this.tableSortmode)
    }

    internal fun update(tableSortmodeUp: TableSortMode, tableSortmodeDown: TableSortMode) {
        this.tableSortmode = if (this.tableSortmode == tableSortmodeUp) {
            tableSortmodeDown
        } else {
            tableSortmodeUp
        }

        this.stockItems = mutableListOf(
            StockItem(
                onlineMarketData = OnlineMarketData(symbol = ""),
                stockDBdata = StockDBdata(symbol = ""),
                assets = emptyList(),
                events = emptyList(),
                dividends = emptyList()
            )
        )

        this.stockItems.addAll(when (tableSortmode) {
            TableSortMode.BySymbolUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.stockDBdata.symbol
            }
            TableSortMode.BySymbolDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.stockDBdata.symbol
            }

            TableSortMode.ByNameUp -> this.stockItemsCopy.sortedBy { stockItem ->
                getName(stockItem.onlineMarketData)
            }
            TableSortMode.ByNameDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                getName(stockItem.onlineMarketData)
            }

            TableSortMode.ByMarketPriceUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.onlineMarketData.marketPrice
            }
            TableSortMode.ByMarketPriceDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.onlineMarketData.marketPrice
            }

            TableSortMode.ByMarketChangeUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.onlineMarketData.marketChange
            }
            TableSortMode.ByMarketChangeDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.onlineMarketData.marketChange
            }

            TableSortMode.ByMarketCurrencyUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.onlineMarketData.currency
            }
            TableSortMode.ByMarketCurrencyDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.onlineMarketData.currency
            }

            TableSortMode.ByQuantityUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity
            }
            TableSortMode.ByQuantityDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity
            }

            TableSortMode.ByPurchasepriceUp,
            TableSortMode.ByAssetsUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                asset + fee
            }
            TableSortMode.ByPurchasepriceDown,
            TableSortMode.ByAssetsDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                asset + fee
            }

            TableSortMode.ByAssetUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * stockItem.onlineMarketData.marketPrice
            }
            TableSortMode.ByAssetDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * stockItem.onlineMarketData.marketPrice
            }

            TableSortMode.ByAssetChangeUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * stockItem.onlineMarketData.marketPrice - asset
            }
            TableSortMode.ByAssetChangeDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * stockItem.onlineMarketData.marketPrice - asset
            }

            TableSortMode.ByAssetFeeUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                fee
            }
            TableSortMode.ByAssetFeeDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                fee
            }

            TableSortMode.ByAssetTotalFeeUp -> this.stockItemsCopy.sortedBy { stockItem ->
                getTotalFee(stockItem.assets)
            }
            TableSortMode.ByAssetTotalFeeDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                getTotalFee(stockItem.assets)
            }

            TableSortMode.ByDividendUp -> this.stockItemsCopy.sortedBy { stockItem ->
                val dividendRate = if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
                    stockItem.stockDBdata.annualDividendRate
                } else {
                    stockItem.onlineMarketData.annualDividendRate
                }
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * dividendRate
            }
            TableSortMode.ByDividendDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                val dividendRate = if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
                    stockItem.stockDBdata.annualDividendRate
                } else {
                    stockItem.onlineMarketData.annualDividendRate
                }
                val (quantity, asset, fee) = getAssets(stockItem.assets)
                quantity * dividendRate
            }

            TableSortMode.ByAlertBelowUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.stockDBdata.alertBelow
            }
            TableSortMode.ByAlertBelowDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.stockDBdata.alertBelow
            }

            TableSortMode.ByAlertAboveUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.stockDBdata.alertAbove
            }
            TableSortMode.ByAlertAboveDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.stockDBdata.alertAbove
            }

            TableSortMode.ByEventsUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.events.size
            }
            TableSortMode.ByEventsDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.events.size
            }

            TableSortMode.ByNoteUp -> this.stockItemsCopy.sortedBy { stockItem ->
                stockItem.stockDBdata.note
            }
            TableSortMode.ByNoteDown -> this.stockItemsCopy.sortedByDescending { stockItem ->
                stockItem.stockDBdata.note
            }

            else -> this.stockItemsCopy
        }
        )

        notifyDataSetChanged()
    }
}
