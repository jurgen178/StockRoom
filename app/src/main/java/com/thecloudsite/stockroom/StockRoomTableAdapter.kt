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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.backgroundColor
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.StockroomTableItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.DividendCycleStrIndex
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getDividendStr
import com.thecloudsite.stockroom.utils.getMarketValues
import com.thecloudsite.stockroom.utils.obsoleteAssetType
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import kotlin.math.absoluteValue

class StockRoomTableAdapter internal constructor(
  val context: Context,
  private val clickListenerSymbolLambda: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomTableAdapter.StockRoomTableViewHolder>(
  StockRoomDiffCallback()
) {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockItems: MutableList<StockItem> = mutableListOf()
  private var defaultTextColor: Int? = null

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
    val current = getItem(position)

    if (defaultTextColor == null) {
      defaultTextColor = holder.binding.tableDataMarketPrice.currentTextColor
    }

    if (current != null) {

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

        holder.binding.tableDataSymbol.text =
          getHeaderStr(context.getString(R.string.table_column_symbol))
        holder.binding.tableDataName.text =
          getHeaderStr(context.getString(R.string.table_column_Name))
        holder.binding.tableDataMarketPrice.text =
          getHeaderStr(context.getString(R.string.table_column_MarketPrice))
        holder.binding.tableDataMarketChange.text =
          getHeaderStr(context.getString(R.string.table_column_MarketChange))
        holder.binding.tableDataMarketCurrency.text =
          getHeaderStr(context.getString(R.string.table_column_MarketCurrency))
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
        holder.binding.tableDataAlertBelow.text =
          getHeaderStr(context.getString(R.string.table_column_AlertBelow))
        holder.binding.tableDataAlertAbove.text =
          getHeaderStr(context.getString(R.string.table_column_AlertAbove))
        holder.binding.tableDataAssets.text =
          getHeaderStr(context.getString(R.string.table_column_Assets))
        holder.binding.tableDataEvents.text =
          getHeaderStr(context.getString(R.string.table_column_Events))
        holder.binding.tableDataNote.text =
          getHeaderStr(context.getString(R.string.table_column_Note))
      } else {

        val backgroundColor = context.getColor(R.color.backgroundListColor)
        holder.binding.tableDataLayout.setBackgroundColor(backgroundColor)

        var color = current.stockDBdata.groupColor
        if (color == 0) {
          color = backgroundColor
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
        holder.binding.tableDataAssetChange.text = assetChange.second
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

          val (totalQuantity, totalPrice) = getAssets(sortedList, obsoleteAssetType)

          // List each asset
          sortedList.forEach { assetItem ->

            val datetime: LocalDateTime =
              LocalDateTime.ofEpochSecond(assetItem.date, 0, ZoneOffset.UTC)

            val assetEntry = SpannableStringBuilder()
              .scale(textScale) {
                append(
                  DecimalFormat(DecimalFormat0To4Digits).format(assetItem.quantity)
                )
                  .append(
                    if (assetItem.price > 0.0) {
                      "@${DecimalFormat(DecimalFormat2To4Digits).format(assetItem.price)}"
                    } else {
                      ""
                    }
                  )
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
          if (totalQuantity > 0.0 && totalPrice > 0.0) {
            assetStr.scale(textScale) {
              append("\n${context.getString(R.string.asset_summary_text)}")
                .color(Color.BLACK) {
                  backgroundColor(Color.YELLOW)
                  {
                    append(
                      "\n${
                        DecimalFormat(DecimalFormat0To4Digits).format(totalQuantity)
                      }@${
                        DecimalFormat(DecimalFormat2To4Digits).format(
                          totalPrice / totalQuantity
                        )
                      } = ${DecimalFormat(DecimalFormat2Digits).format(totalPrice)}"
                    )
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
            val localDateTime = LocalDateTime.ofEpochSecond(it.datetime, 0, ZoneOffset.UTC)
            val datetime = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
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
