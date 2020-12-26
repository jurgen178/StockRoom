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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.StockRoomTableAdapter.BaseViewHolder
import com.thecloudsite.stockroom.list.DBData
import com.thecloudsite.stockroom.list.ListDBAdapter
import com.thecloudsite.stockroom.list.db_dividend_type
import com.thecloudsite.stockroom.list.db_headline_type

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

const val table_headline_type: Int = 0
const val table_data_type: Int = 1

data class StockData(
  val viewType: Int,
  val title: String = "",
  val stockItem: StockItem? = null,
)

class StockRoomTableAdapter internal constructor(
  val context: Context,
  private val clickListenerSummary: (StockItem) -> Unit
) : RecyclerView.Adapter<BaseViewHolder<*>>() {
//  ) : ListAdapter<StockItem, StockRoomTableAdapter.StockRoomViewHolder>(StockRoomDiffCallback()) {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var stockDataItems: MutableList<StockData> = mutableListOf()
  private var defaultTextColor: Int? = null

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
  }

  class TableHeadlineViewHolder(
    val binding: StockRoomTableHeadlineItemBinding
  ) : ListDBAdapter.BaseViewHolder<DBData>(binding.root) {
    override fun bind(item: DBData) {
    }
  }

  class TableDataViewHolder(
    val binding: StockRoomTableHeadlineItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindGroup(
      stockItem: StockItem,
      clickListener: (StockItem, View) -> Unit
    ) {
      binding.itemviewGroup.setOnClickListener { clickListener(stockItem, itemView) }
    }

    fun bindSummary(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      binding.itemSummary.setOnClickListener { clickListener(stockItem) }
      binding.itemRedGreen.setOnClickListener { clickListener(stockItem) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomTableAdapter.BaseViewHolder<*> {

    return when (viewType) {
      table_headline_type -> {
        val binding =
          StockRoomTableHeadlineItemBinding.inflate(LayoutInflater.from(context), parent, false)
        TableHeadlineViewHolder(binding)
      }

      table_data_type -> {
        val binding =
          StockRoomTableDataItemBinding.inflate(LayoutInflater.from(context), parent, false)
        TableDataViewHolder(binding)
      }

      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  override fun onBindViewHolder(
    holder: StockRoomTableAdapter.BaseViewHolder<*>,
    position: Int
  ) {

//    val data: DBData = dbDataList[position]
//
//    when (holder) {
//
//      is HeadlineViewHolder -> {
//        holder.bind(data)
//
//        holder.binding.dbHeadline.text = data.title
//      }
//
//      is StockDBdataViewHolder -> {
//        holder.bind(data)
//
//        if (data.isHeader) {
//          holder.binding.dbStockdbdataLayout.setBackgroundColor(Color.rgb(139, 0, 0))
//          holder.binding.dbStockdbdataSymbol.text = getHeaderStr("symbol")
//          holder.binding.dbStockdbdataPortfolio.text = getHeaderStr("portfolio")
//          holder.binding.dbStockdbdataData.text = getHeaderStr("data")
//          holder.binding.dbStockdbdataGroupColor.text = getHeaderStr("groupColor")
//          holder.binding.dbStockdbdataNote.text = getHeaderStr("note")
//          holder.binding.dbStockdbdataDividendNote.text = getHeaderStr("dividendNote")
//          holder.binding.dbStockdbdataAnnualDividendRate.text = getHeaderStr("annualDividendRate")
//          holder.binding.dbStockdbdataAlertAbove.text = getHeaderStr("alertAbove")
//          holder.binding.dbStockdbdataAlertAboveNote.text = getHeaderStr("alertAboveNote")
//          holder.binding.dbStockdbdataAlertBelow.text = getHeaderStr("alertBelow")
//          holder.binding.dbStockdbdataAlertBelowNote.text = getHeaderStr("alertBelowNote")
//        } else {
//          holder.binding.dbStockdbdataLayout.setBackgroundColor(Color.rgb(0, 148, 255))
//          holder.binding.dbStockdbdataSymbol.text = data.symbol
//          holder.binding.dbStockdbdataPortfolio.text = data.portfolio
//          holder.binding.dbStockdbdataData.text = data.data
//          holder.binding.dbStockdbdataGroupColor.text = getColorStr(data.groupColor)
//          holder.binding.dbStockdbdataNote.text = data.note
//          holder.binding.dbStockdbdataDividendNote.text = data.dividendNote
//          holder.binding.dbStockdbdataAnnualDividendRate.text = if (data.annualDividendRate >= 0.0) {
//            DecimalFormat(DecimalFormat2To4Digits).format(data.annualDividendRate)
//          } else {
//            ""
//          }
//          holder.binding.dbStockdbdataAlertAbove.text = if (data.alertAbove > 0.0) {
//            DecimalFormat(DecimalFormat2To4Digits).format(data.alertAbove)
//          } else {
//            ""
//          }
//          holder.binding.dbStockdbdataAlertAboveNote.text = data.alertAboveNote
//          holder.binding.dbStockdbdataAlertBelow.text = if (data.alertBelow > 0.0) {
//            DecimalFormat(DecimalFormat2To4Digits).format(data.alertBelow)
//          } else {
//            ""
//          }
//          holder.binding.dbStockdbdataAlertBelowNote.text = data.alertBelowNote
//        }
//      }
//
//      is GroupViewHolder -> {
//        holder.bind(data)
//
//        if (data.isHeader) {
//          holder.binding.dbGroupLayout.setBackgroundColor(Color.rgb(139, 0, 0))
//          holder.binding.dbGroupColor.text = getHeaderStr("color")
//          holder.binding.dbGroupName.text = getHeaderStr("name")
//        } else {
//          holder.binding.dbGroupLayout.setBackgroundColor(Color.rgb(255, 127, 182))
//          holder.binding.dbGroupColor.text = getColorStr(data.color)
//          holder.binding.dbGroupName.text = data.name
//        }
//
////      groupTableRowsCount = items.size
////
////      items.forEach { groupItem ->
////        groupTableRows.append("<tr>")
////        groupTableRows.append("<td>${getColorStr(groupItem.color)}</td>")
////        groupTableRows.append("<td>${groupItem.name}</td>")
////        groupTableRows.append("</tr>")
////      }
//
//      }
//
//      is AssetViewHolder -> {
//        holder.bind(data)
//
//        if (data.isHeader) {
//          holder.binding.dbAssetLayout.setBackgroundColor(Color.rgb(139, 0, 0))
//          holder.binding.dbAssetId.text = getHeaderStr("id")
//          holder.binding.dbAssetSymbol.text = getHeaderStr("symbol")
//          holder.binding.dbAssetQuantity.text = getHeaderStr("quantity")
//          holder.binding.dbAssetPrice.text = getHeaderStr("price")
//          holder.binding.dbAssetType.text = getHeaderStr("type")
//          holder.binding.dbAssetNote.text = getHeaderStr("note")
//          holder.binding.dbAssetDate.text = getHeaderStr("date")
//          holder.binding.dbAssetSharesPerQuantity.text = getHeaderStr("sharesPerQuantity")
//          holder.binding.dbAssetExpirationDate.text = getHeaderStr("expirationDate")
//          holder.binding.dbAssetPremium.text = getHeaderStr("premium")
//          holder.binding.dbAssetCommission.text = getHeaderStr("commission")
//        } else {
//          holder.binding.dbAssetLayout.setBackgroundColor(Color.rgb(255, 106, 0))
//          holder.binding.dbAssetId.text = data.id?.toString() ?: ""
//          holder.binding.dbAssetSymbol.text = data.symbol
//          holder.binding.dbAssetQuantity.text =
//            DecimalFormat(DecimalFormat0To4Digits).format(data.quantity)
//          holder.binding.dbAssetPrice.text = DecimalFormat(DecimalFormat0To4Digits).format(data.price)
//          holder.binding.dbAssetType.text = data.type.toString()
//          holder.binding.dbAssetNote.text = data.note
//          holder.binding.dbAssetDate.text = getDateTimeStr(data.date)
//          holder.binding.dbAssetSharesPerQuantity.text = "${data.sharesPerQuantity}"
//          holder.binding.dbAssetExpirationDate.text = getDateStr(data.expirationDate)
//          holder.binding.dbAssetPremium.text =
//            if (data.premium > 0.0) DecimalFormat(DecimalFormat0To4Digits).format(
//                data.premium
//            ) else ""
//          holder.binding.dbAssetCommission.text =
//            if (data.commission > 0.0) DecimalFormat(DecimalFormat0To4Digits).format(
//                data.commission
//            ) else ""
//        }
////      assetTableRowsCount = items.size
////
////      items.forEach { assetItem ->
////        assetTableRows.append("<tr>")
////        assetTableRows.append("<td>${assetItem.id}</td>")
////        assetTableRows.append("<td>${assetItem.symbol}</td>")
////        assetTableRows.append("<td>${DecimalFormat(DecimalFormat0To4Digits).format(assetItem.quantity)}</td>")
////        assetTableRows.append("<td>${assetItem.price}</td>")
////        assetTableRows.append("<td>${assetItem.type}</td>")
////        assetTableRows.append("<td>${assetItem.note}</td>")
////        assetTableRows.append("<td>${getDateTimeStr(assetItem.date)}</td>")
////        assetTableRows.append("<td>${assetItem.sharesPerQuantity}</td>")
////        assetTableRows.append("<td>${getDateStr(assetItem.expirationDate)}</td>")
////        assetTableRows.append(
////            "<td>${if (assetItem.premium != 0.0) assetItem.premium else ""}</td>"
////        )
////        assetTableRows.append(
////            "<td>${if (assetItem.commission != 0.0) assetItem.commission else ""}</td>"
////        )
////        assetTableRows.append("</tr>")
////      }
//
//      }
//
//      is EventViewHolder -> {
//        holder.bind(data)
//
//        if (data.isHeader) {
//          holder.binding.dbEventLayout.setBackgroundColor(Color.rgb(139, 0, 0))
//          holder.binding.dbEventId.text = getHeaderStr("id")
//          holder.binding.dbEventSymbol.text = getHeaderStr("symbol")
//          holder.binding.dbEventTitle.text = getHeaderStr("title")
//          holder.binding.dbEventDatetime.text = getHeaderStr("datetime")
//          holder.binding.dbEventType.text = getHeaderStr("type")
//          holder.binding.dbEventNote.text = getHeaderStr("note")
//        } else {
//          holder.binding.dbEventLayout.setBackgroundColor(Color.rgb(127, 255, 197))
//          holder.binding.dbEventId.text = data.id?.toString() ?: ""
//          holder.binding.dbEventSymbol.text = data.symbol
//          holder.binding.dbEventTitle.text = data.title
//          holder.binding.dbEventDatetime.text = getDateTimeStr(data.datetime)
//          holder.binding.dbEventType.text = data.type.toString()
//          holder.binding.dbEventNote.text = data.note
//        }
//
////      eventTableRowsCount = items.size
////
////      items.forEach { eventItem ->
////        eventTableRows.append("<tr>")
////        eventTableRows.append("<td>${eventItem.id}</td>")
////        eventTableRows.append("<td>${eventItem.symbol}</td>")
////        eventTableRows.append("<td>${eventItem.type}</td>")
////        eventTableRows.append("<td>${eventItem.title}</td>")
////        eventTableRows.append("<td>${eventItem.note}</td>")
////        eventTableRows.append("<td>${getDateTimeStr(eventItem.datetime)}</td>")
////        eventTableRows.append("</tr>")
////      }
//
//      }
//
//      is DividendViewHolder -> {
//        holder.bind(data)
//
//        if (data.isHeader) {
//          holder.binding.dbDividendLayout.setBackgroundColor(Color.rgb(139, 0, 0))
//          holder.binding.dbDividendId.text = getHeaderStr("id")
//          holder.binding.dbDividendSymbol.text = getHeaderStr("symbol")
//          holder.binding.dbDividendAmount.text = getHeaderStr("amount")
//          holder.binding.dbDividendCycle.text = getHeaderStr("cycle")
//          holder.binding.dbDividendPaydate.text = getHeaderStr("paydate")
//          holder.binding.dbDividendType.text = getHeaderStr("type")
//          holder.binding.dbDividendExdate.text = getHeaderStr("exdate")
//          holder.binding.dbDividendNote.text = getHeaderStr("note")
//        } else {
//          holder.binding.dbDividendLayout.setBackgroundColor(Color.rgb(255, 233, 127))
//          holder.binding.dbDividendId.text = data.id?.toString() ?: ""
//          holder.binding.dbDividendSymbol.text = data.symbol
//          holder.binding.dbDividendAmount.text =
//            DecimalFormat(DecimalFormat2To4Digits).format(data.amount)
//          holder.binding.dbDividendCycle.text = data.cycle.toString()
//          holder.binding.dbDividendPaydate.text = getDateStr(data.paydate)
//          holder.binding.dbDividendType.text = data.type.toString()
//          holder.binding.dbDividendExdate.text = getDateStr(data.exdate)
//          holder.binding.dbDividendNote.text = data.note
//        }
//
////      dividendTableRowsCount = items.size
////
////      items.forEach { dividendItem ->
////        dividendTableRows.append("<tr>")
////        dividendTableRows.append("<td>${dividendItem.id}</td>")
////        dividendTableRows.append("<td>${dividendItem.symbol}</td>")
////        dividendTableRows.append("<td>${DecimalFormat(DecimalFormat2To4Digits).format(dividendItem.amount)}</td>")
////        dividendTableRows.append("<td>${dividendItem.type}</td>")
////        dividendTableRows.append("<td>${dividendItem.cycle}</td>")
////        dividendTableRows.append("<td>${getDateStr(dividendItem.paydate)}</td>")
////        dividendTableRows.append("<td>${getDateStr(dividendItem.exdate)}</td>")
////        dividendTableRows.append("<td>${dividendItem.note}</td>")
////        dividendTableRows.append("</tr>")
////      }
//
//      }
//
//      else -> {
//        throw IllegalArgumentException()
//      }
//    }
  }

//  override fun onBindViewHolder(
//    holder: StockRoomViewHolder,
//    position: Int
//  ) {
//    val current = getItem(position)
//
//    return
//
//    if (defaultTextColor == null) {
//      defaultTextColor = holder.binding.textViewAssets.currentTextColor
//    }
//
//    if (current != null) {
//      holder.bindGroup(current, clickListenerGroup)
//      holder.bindSummary(current, clickListenerSummary)
//
//      holder.binding.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))
//
//      holder.binding.textViewSymbol.text = current.onlineMarketData.symbol
//      holder.binding.textViewName.text = getName(current.onlineMarketData)
//
//      if (current.onlineMarketData.marketPrice > 0.0) {
//        val marketValues = getMarketValues(current.onlineMarketData)
//
//        if (current.onlineMarketData.postMarketData) {
//          holder.binding.textViewMarketPrice.text = SpannableStringBuilder()
//              .italic { append(marketValues.first) }
//
//          holder.binding.textViewChange.text = SpannableStringBuilder()
//              .italic { append(marketValues.second) }
//
//          holder.binding.textViewChangePercent.text = SpannableStringBuilder()
//              .italic { append(marketValues.third) }
//        } else {
//          holder.binding.textViewMarketPrice.text = marketValues.first
//          holder.binding.textViewChange.text = marketValues.second
//          holder.binding.textViewChangePercent.text = marketValues.third
//        }
//      } else {
//        holder.binding.textViewMarketPrice.text = ""
//        holder.binding.textViewChange.text = ""
//        holder.binding.textViewChangePercent.text = ""
//        holder.binding.textViewAssets.text = ""
//      }
//
//      val (quantity, asset) = getAssets(current.assets)
////      val quantity = current.assets.sumByDouble {
////        it.quantity
////      }
//
//      val assets = SpannableStringBuilder()
//
////      var asset: Double = 0.0
//      var capital: Double = 0.0
//
//      if (quantity > 0.0 && asset > 0.0) {
////        asset = current.assets.sumByDouble {
////          it.quantity * it.price
////        }
//
//        assets.append(
//            "${DecimalFormat(DecimalFormat0To4Digits).format(quantity)}@${
//              DecimalFormat(DecimalFormat2To4Digits).format(
//                  asset / quantity
//              )
//            }"
//        )
//
//        if (current.onlineMarketData.marketPrice > 0.0) {
//          capital = quantity * current.onlineMarketData.marketPrice
////          capital = current.assets.sumByDouble {
////            it.quantity * current.onlineMarketData.marketPrice
////          }
//
//          assets.append(
//              "\n${
//                DecimalFormat(
//                    DecimalFormat2Digits
//                ).format(asset)
//              } "
//          )
//
//          val assetChange = capital - asset
//          val capitalPercent = assetChange * 100.0 / asset
//
//          assets.color(
//              getChangeColor(
//                  assetChange,
//                  current.onlineMarketData.postMarketData,
//                  defaultTextColor!!,
//                  context
//              )
//          )
//          {
//            assets.append(
//                "${
//                  if (capital >= asset) {
//                    "+"
//                  } else {
//                    "-"
//                  }
//                } ${
//                  DecimalFormat(DecimalFormat2Digits).format(
//                      (assetChange).absoluteValue
//                  )
//                }"
//            )
//            if (capitalPercent < 10000.0) {
//              assets.append(
//                  " (${
//                    if (capital >= asset) {
//                      "+"
//                    } else {
//                      ""
//                    }
//                  }${DecimalFormat(DecimalFormat2Digits).format(capitalPercent)}%)"
//              )
//            }
//          }
//
//          assets.append(" = ")
//          assets.bold { append(DecimalFormat(DecimalFormat2Digits).format(capital)) }
//        }
//      }
//
////      // set background to asset change
////      holder.itemRedGreen.setBackgroundColor(
////          getChangeColor(capital, asset, context.getColor(R.color.backgroundListColor), context)
////      )
//      // set background to market change
//      holder.binding.itemRedGreen.setBackgroundColor(
//          getChangeColor(
//              current.onlineMarketData.marketChange,
//              current.onlineMarketData.postMarketData,
//              context.getColor(color.backgroundListColor),
//              context
//          )
//      )
//
//      val dividendStr = getDividendStr(current, context)
//      if (dividendStr.isNotEmpty()) {
//        if (assets.isNotEmpty()) {
//          assets.append("\n")
//        }
//
//        assets.append(
//            dividendStr
//        )
//      }
//
//      if (current.stockDBdata.alertAbove > 0.0) {
//        if (assets.isNotEmpty()) {
//          assets.append("\n")
//        }
//
//        assets.append(
//            "${context.getString(R.string.alert_above_in_list)} ${
//              DecimalFormat(
//                  DecimalFormat2To4Digits
//              ).format(current.stockDBdata.alertAbove)
//            }"
//        )
//      }
//      if (current.stockDBdata.alertBelow > 0.0) {
//        if (assets.isNotEmpty()) {
//          assets.append("\n")
//        }
//
//        assets.append(
//            "${context.getString(R.string.alert_below_in_list)} ${
//              DecimalFormat(
//                  DecimalFormat2To4Digits
//              ).format(current.stockDBdata.alertBelow)
//            }"
//        )
//      }
//      if (current.events.isNotEmpty()) {
//        val count = current.events.size
//        val eventStr =
//          context.resources.getQuantityString(R.plurals.events_in_list, count, count)
//
//        if (assets.isNotEmpty()) {
//          assets.append("\n")
//        }
//
//        assets.append(eventStr)
//        current.events.forEach {
//          val localDateTime = LocalDateTime.ofEpochSecond(it.datetime, 0, ZoneOffset.UTC)
//          val datetime = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
//          assets.append(
//              "\n${
//                context.getString(
//                    R.string.event_datetime_format, it.title, datetime
//                )
//              }"
//          )
//        }
//      }
//      if (current.stockDBdata.note.isNotEmpty()) {
//        if (assets.isNotEmpty()) {
//          assets.append("\n")
//        }
//
//        assets.append(
//            "${
//              context.getString(
//                  R.string.note_in_list
//              )
//            } ${current.stockDBdata.note}"
//        )
//      }
//
//      holder.binding.textViewAssets.text = assets
//
//      var color = current.stockDBdata.groupColor
//      if (color == 0) {
//        color = context.getColor(R.color.backgroundListColor)
//      }
//      setBackgroundColor(holder.binding.itemviewGroup, color)
//
//      /*
//      // Keep the corner radii and only change the background color.
//      val gradientDrawable = holder.itemLinearLayoutGroup.background as GradientDrawable
//      gradientDrawable.setColor(color)
//      holder.itemLinearLayoutGroup.background = gradientDrawable
//      */
//    }
//  }

  override fun getItemViewType(position: Int): Int {
    val element: StockData = stockDataItems[position]
    return element.viewType
  }

  internal fun setStockItems(stockItems: List<StockItem>) {
    this.stockDataItems = mutableListOf(
        StockData(
            viewType = table_headline_type,
            title = "\ntest_table",
        )
    )

    this.stockDataItems.addAll(stockItems.map { stockItem ->
      StockData(
          viewType = table_data_type,
          stockItem = stockItem
      )
    })

    notifyDataSetChanged()
  }

  override fun getItemCount() = stockDataItems.size
}
