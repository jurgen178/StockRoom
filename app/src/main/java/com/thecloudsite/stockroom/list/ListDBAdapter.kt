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

package com.thecloudsite.stockroom.list

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.list.ListDBAdapter.BaseViewHolder
import okhttp3.internal.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

const val db_type_headline: Int = 0
const val db_stockdbdata_type: Int = 1
const val db_asset_type: Int = 2
const val db_event_type: Int = 3
const val db_dividend_type: Int = 4

data class DBData(
  val viewType: Int,
  val isHeader: Boolean = false,
  val id: Long? = null,
  val symbol: String = "",
  val portfolio: String = "",
  val data: String = "",
  val groupColor: Int = 0,
  val notes: String = "",
  val dividendNotes: String = "",
  val alertAbove: Double = 0.0,
  val alertBelow: Double = 0.0,
  val color: Int = 0,
  val name: String = "",
  val quantity: Double = 0.0,
  val price: Double = 0.0,
  val type: Int = 0,
  val note: String = "",
  val date: Long = 0L,
  val sharesPerQuantity: Int = 1,
  val expirationDate: Long = 0L,
  val premium: Double = 0.0,
  val commission: Double = 0.0,
  val title: String = "",
  val datetime: Long = 0L,
  val amount: Double = 0.0,
  val cycle: Int = 0,
  val paydate: Long = 0L,
  val exdate: Long = 0L
)

class ListDBAdapter(
  private val context: Context
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
  }

  private var dbDataList: MutableList<DBData> = mutableListOf()
  private var dbDataMap: MutableMap<String, MutableList<DBData>> = mutableMapOf()

  class HeadlineViewHolder(itemView: View) : BaseViewHolder<DBData>(itemView) {
    override fun bind(item: DBData) {
    }

    val db_type_headline: TextView = itemView.findViewById(id.db_type_headline)
  }

  class StockDBdataViewHolder(itemView: View) : BaseViewHolder<DBData>(itemView) {
    override fun bind(item: DBData) {
    }

    val db_stockdbdata_layout: ConstraintLayout = itemView.findViewById(id.db_stockdbdata_layout)
    val db_stockdbdata_symbol: TextView = itemView.findViewById(id.db_stockdbdata_symbol)
    val db_stockdbdata_portfolio: TextView = itemView.findViewById(id.db_stockdbdata_portfolio)
    val db_stockdbdata_data: TextView = itemView.findViewById(id.db_stockdbdata_data)
    val db_stockdbdata_groupColor: TextView = itemView.findViewById(id.db_stockdbdata_groupColor)
    val db_stockdbdata_notes: TextView = itemView.findViewById(id.db_stockdbdata_notes)
    val db_stockdbdata_dividendNotes: TextView =
      itemView.findViewById(id.db_stockdbdata_dividendNotes)
    val db_stockdbdata_alertAbove: TextView = itemView.findViewById(id.db_stockdbdata_alertAbove)
    val db_stockdbdata_alertBelow: TextView = itemView.findViewById(id.db_stockdbdata_alertBelow)
  }

  class AssetViewHolder(itemView: View) : BaseViewHolder<DBData>(itemView) {
    override fun bind(item: DBData) {
    }

    val db_asset_layout: ConstraintLayout = itemView.findViewById(id.db_asset_layout)
    val db_asset_id: TextView = itemView.findViewById(id.db_asset_id)
    val db_asset_symbol: TextView = itemView.findViewById(id.db_asset_symbol)
    val db_asset_quantity: TextView = itemView.findViewById(id.db_asset_quantity)
    val db_asset_price: TextView = itemView.findViewById(id.db_asset_price)
    val db_asset_type: TextView = itemView.findViewById(id.db_asset_type)
    val db_asset_note: TextView = itemView.findViewById(id.db_asset_note)
    val db_asset_date: TextView = itemView.findViewById(id.db_asset_date)
    val db_asset_sharesPerQuantity: TextView = itemView.findViewById(id.db_asset_sharesPerQuantity)
    val db_asset_expirationDate: TextView = itemView.findViewById(id.db_asset_expirationDate)
    val db_asset_premium: TextView = itemView.findViewById(id.db_asset_premium)
    val db_asset_commission: TextView = itemView.findViewById(id.db_asset_commission)
  }

  class EventViewHolder(itemView: View) : BaseViewHolder<DBData>(itemView) {
    override fun bind(item: DBData) {
    }

    val db_event_layout: ConstraintLayout = itemView.findViewById(id.db_event_layout)
    val db_event_id: TextView = itemView.findViewById(id.db_event_id)
    val db_event_symbol: TextView = itemView.findViewById(id.db_event_symbol)
    val db_event_title: TextView = itemView.findViewById(id.db_event_title)
    val db_event_datetime: TextView = itemView.findViewById(id.db_event_datetime)
    val db_event_type: TextView = itemView.findViewById(id.db_event_type)
    val db_event_note: TextView = itemView.findViewById(id.db_event_note)
  }

  class DividendViewHolder(itemView: View) : BaseViewHolder<DBData>(itemView) {
    override fun bind(item: DBData) {
    }

    val db_dividend_layout: ConstraintLayout = itemView.findViewById(id.db_dividend_layout)
    val db_dividend_id: TextView = itemView.findViewById(id.db_dividend_id)
    val db_dividend_symbol: TextView = itemView.findViewById(id.db_dividend_symbol)
    val db_dividend_amount: TextView = itemView.findViewById(id.db_dividend_amount)
    val db_dividend_cycle: TextView = itemView.findViewById(id.db_dividend_cycle)
    val db_dividend_paydate: TextView = itemView.findViewById(id.db_dividend_paydate)
    val db_dividend_type: TextView = itemView.findViewById(id.db_dividend_type)
    val db_dividend_exdate: TextView = itemView.findViewById(id.db_dividend_exdate)
    val db_dividend_note: TextView = itemView.findViewById(id.db_dividend_note)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {

    return when (viewType) {
      db_type_headline -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.db_type_headline_item, parent, false)
        HeadlineViewHolder(view)
      }
      db_stockdbdata_type -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.db_stockdbdata_type_item, parent, false)
        StockDBdataViewHolder(view)
      }

      db_asset_type -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.db_asset_type_item, parent, false)
        AssetViewHolder(view)
      }

      db_event_type -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.db_event_type_item, parent, false)
        EventViewHolder(view)
      }

      db_dividend_type -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.db_dividend_type_item, parent, false)
        DividendViewHolder(view)
      }

      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  //-----------onCreateViewHolder: bind view with data model---------
  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {

    val element: DBData = dbDataList[position]

    when (holder) {

      is HeadlineViewHolder -> {
        holder.bind(element)

        val data: DBData = dbDataList[position]
        holder.db_type_headline.text = data.title
      }

      is StockDBdataViewHolder -> {
        holder.bind(element)

        val data: DBData = dbDataList[position]

        if (data.isHeader) {
          holder.db_stockdbdata_layout.setBackgroundColor(Color.BLUE)
          holder.db_stockdbdata_symbol.text = "symbol"
          holder.db_stockdbdata_portfolio.text = "portfolio"
          holder.db_stockdbdata_data.text = "data"
          holder.db_stockdbdata_groupColor.text = "groupColor"
          holder.db_stockdbdata_notes.text = "notes"
          holder.db_stockdbdata_dividendNotes.text = "dividendNotes"
          holder.db_stockdbdata_alertAbove.text = "alertAbove"
          holder.db_stockdbdata_alertBelow.text = "alertBelow"
        } else {
          holder.db_stockdbdata_layout.setBackgroundColor(Color.BLUE)
          holder.db_stockdbdata_symbol.text = data.symbol
          holder.db_stockdbdata_portfolio.text = data.portfolio
          holder.db_stockdbdata_data.text = data.data
          holder.db_stockdbdata_groupColor.text = getColorStr(data.groupColor)
          holder.db_stockdbdata_notes.text = data.notes
          holder.db_stockdbdata_dividendNotes.text = data.dividendNotes
          holder.db_stockdbdata_alertAbove.text = if (data.alertAbove != 0.0) {
            DecimalFormat("0.00##").format(data.alertAbove)
          } else {
            ""
          }
          holder.db_stockdbdata_alertBelow.text = if (data.alertBelow != 0.0) {
            DecimalFormat("0.00##").format(data.alertBelow)
          } else {
            ""
          }
        }
      }

      is AssetViewHolder -> {
        holder.bind(element)

        val data: DBData = dbDataList[position]

        if (data.isHeader) {
          holder.db_asset_layout.setBackgroundColor(Color.GREEN)
          holder.db_asset_id.text = "id"
          holder.db_asset_symbol.text = "symbol"
          holder.db_asset_quantity.text = "quantity"
          holder.db_asset_price.text = "price"
          holder.db_asset_type.text = "type"
          holder.db_asset_note.text = "note"
          holder.db_asset_date.text = "date"
          holder.db_asset_sharesPerQuantity.text = "sharesPerQuantity"
          holder.db_asset_expirationDate.text = "expirationDate"
          holder.db_asset_premium.text = "premium"
          holder.db_asset_commission.text = "commission"
        } else {
          holder.db_asset_layout.setBackgroundColor(Color.GREEN)
          holder.db_asset_id.text = data.id?.toString() ?: ""
          holder.db_asset_symbol.text = data.symbol
          holder.db_asset_quantity.text = DecimalFormat("0.####").format(data.quantity)
          holder.db_asset_price.text = data.price.toString()
          holder.db_asset_type.text = data.type.toString()
          holder.db_asset_note.text = data.note
          holder.db_asset_date.text = getDateTimeStr(data.date)
          holder.db_asset_sharesPerQuantity.text = "${data.sharesPerQuantity}"
          holder.db_asset_expirationDate.text = getDateStr(data.expirationDate)
          holder.db_asset_premium.text = if (data.premium != 0.0) data.premium.toString() else ""
          holder.db_asset_commission.text =
            if (data.commission != 0.0) data.commission.toString() else ""
        }
      }

      is EventViewHolder -> {
        holder.bind(element)

        val data: DBData = dbDataList[position]

        if (data.isHeader) {
          holder.db_event_layout.setBackgroundColor(Color.RED)
          holder.db_event_id.text = "id"
          holder.db_event_symbol.text = "symbol"
          holder.db_event_title.text = "title"
          holder.db_event_datetime.text = "datetime"
          holder.db_event_type.text = "type"
          holder.db_event_note.text = "note"
        } else {
          holder.db_event_layout.setBackgroundColor(Color.RED)
          holder.db_event_id.text = data.id?.toString() ?: ""
          holder.db_event_symbol.text = data.symbol
          holder.db_event_title.text = data.title
          holder.db_event_datetime.text = getDateTimeStr(data.datetime)
          holder.db_event_type.text = data.type.toString()
          holder.db_event_note.text = data.note
        }
      }

      is DividendViewHolder -> {
        holder.bind(element)

        val data: DBData = dbDataList[position]

        holder.db_dividend_layout.setBackgroundColor(Color.MAGENTA)

        if (data.isHeader) {
          holder.db_dividend_id.text = "id"
          holder.db_dividend_symbol.text = "symbol"
          holder.db_dividend_amount.text = "amount"
          holder.db_dividend_cycle.text = "cycle"
          holder.db_dividend_paydate.text = "paydate"
          holder.db_dividend_type.text = "type"
          holder.db_dividend_exdate.text = "exdate"
          holder.db_dividend_note.text = "note"
        } else {
          holder.db_dividend_id.text = data.id?.toString() ?: ""
          holder.db_dividend_symbol.text = data.symbol
          holder.db_dividend_amount.text = DecimalFormat("0.00##").format(data.amount)
          holder.db_dividend_cycle.text = data.cycle.toString()
          holder.db_dividend_paydate.text = getDateStr(data.paydate)
          holder.db_dividend_type.text = data.type.toString()
          holder.db_dividend_exdate.text = getDateStr(data.exdate)
          holder.db_dividend_note.text = data.note
        }
      }

      else -> {
        throw IllegalArgumentException()
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    val element: DBData = dbDataList[position]
    return element.viewType
  }

  private fun getDateStr(datetime: Long): String {
    return if (datetime != 0L) {
      val localDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
      val dateTimeStr =
        "${datetime}</br>${
          localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        }"
      dateTimeStr
    } else {
      ""
    }
  }

  private fun getDateTimeStr(datetime: Long): String {
    return if (datetime != 0L) {
      val localDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
      val dateTimeStr =
        "${datetime}</br>${
          localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        }&nbsp;${
          localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
        }"
      dateTimeStr
    } else {
      ""
    }
  }

  private fun getColorStr(color: Int): SpannableStringBuilder =
    if (color == 0) {
      SpannableStringBuilder()
    } else {
      val hexStr = "0x${color.toHexString()}"
      val colorCode = hexStr.replace("0xff", "#")
      SpannableStringBuilder()
          .append("$color")
          .color(color) {
            append("   ")
          }
          .append(colorCode)
    }

  private fun getHtmlColorStr(color: Int): String =
    if (color == 0) {
      ""
    } else {
      val hexStr = "0x${color.toHexString()}"
      val colorCode = hexStr.replace("0xff", "#")
      val colorSample =
        "<font style=\"background-color: $colorCode;\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>"
      "$color</br>$colorSample&nbsp;$colorCode"
    }

  fun updateList() {

    dbDataList.clear()
    dbDataMap.toSortedMap()
        .forEach { (_, dbData) ->
          dbDataList.addAll(dbData)
        }

    notifyDataSetChanged()
  }

  fun updateStockDBdata(data: List<StockDBdata>) {

    this.dbDataMap["0_StockDBdata"] = mutableListOf(
        DBData(
            viewType = db_type_headline,
            title = "StockDBdata (${data.size})"
        ),
        DBData(
            viewType = db_stockdbdata_type,
            isHeader = true
        )
    )

    this.dbDataMap["0_StockDBdata"]?.addAll(data.map { stockDBdata ->
      DBData(
          viewType = db_stockdbdata_type,
          symbol = stockDBdata.symbol,
          portfolio = stockDBdata.portfolio,
          data = stockDBdata.data,
          groupColor = stockDBdata.groupColor,
          notes = stockDBdata.notes,
          dividendNotes = stockDBdata.dividendNotes,
          alertAbove = stockDBdata.alertAbove,
          alertBelow = stockDBdata.alertBelow
      )
    })

    updateList()
  }

  fun updateAsset(data: List<Asset>) {

    this.dbDataMap["1_Asset"] = mutableListOf(
        DBData(
            viewType = db_type_headline,
            title = "Asset (${data.size})"
        ),
        DBData(
            viewType = db_asset_type,
            isHeader = true
        )
    )

    this.dbDataMap["1_Asset"]?.addAll(data.map { asset ->
      DBData(
          viewType = db_asset_type,
          id = asset.id,
          symbol = asset.symbol,
          quantity = asset.quantity,
          price = asset.price,
          type = asset.type,
          note = asset.note,
          date = asset.date,
          sharesPerQuantity = asset.sharesPerQuantity,
          expirationDate = asset.expirationDate,
          premium = asset.premium,
          commission = asset.commission
      )
    })

    updateList()
  }

  fun updateEvent(data: List<Event>) {

    this.dbDataMap["2_Event"] = mutableListOf(
        DBData(
            viewType = db_type_headline,
            title = "Event (${data.size})"
        ),
        DBData(
            viewType = db_event_type,
            isHeader = true
        )
    )

    this.dbDataMap["2_Event"]?.addAll(data.map { event ->
      DBData(
          viewType = db_event_type,
          id = event.id,
          symbol = event.symbol,
          title = event.title,
          datetime = event.datetime,
          type = event.type,
          note = event.note
      )
    })

    updateList()
  }

  fun updateDividend(data: List<Dividend>) {

    this.dbDataMap["3_Dividend"] = mutableListOf(
        DBData(
            viewType = db_type_headline,
            title = "Dividend (${data.size})"
        ),
        DBData(
            viewType = db_dividend_type,
            isHeader = true
        )
    )

    this.dbDataMap["3_Dividend"]?.addAll(data.map { dividend ->
      DBData(
          viewType = db_dividend_type,
          id = dividend.id,
          symbol = dividend.symbol,
          amount = dividend.amount,
          cycle = dividend.cycle,
          paydate = dividend.paydate,
          type = dividend.type,
          exdate = dividend.exdate,
          note = dividend.note
      )
    })

    updateList()
  }

  override fun getItemCount() = dbDataList.size
}
