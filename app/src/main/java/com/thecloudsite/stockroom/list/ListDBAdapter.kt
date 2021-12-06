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

package com.thecloudsite.stockroom.list

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.backgroundColor
import androidx.core.text.bold
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.dataProviderFromInt
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.database.StoreData
import com.thecloudsite.stockroom.databinding.DbAssetTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbDividendTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbEventTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbGroupTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbHeadlineTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbStockdbdataTypeItemBinding
import com.thecloudsite.stockroom.databinding.DbStoredataTypeItemBinding
import com.thecloudsite.stockroom.list.ListDBAdapter.BaseViewHolder
import com.thecloudsite.stockroom.utils.*
import okhttp3.internal.toHexString
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

const val db_headline_type: Int = 0
const val db_stockdbdata_type: Int = 1
const val db_group_type: Int = 2
const val db_asset_type: Int = 3
const val db_event_type: Int = 4
const val db_dividend_type: Int = 5
const val db_storedata_type: Int = 6

data class DBData(
    val viewType: Int,
    val isHeader: Boolean = false,
    val id: Long? = null,
    val symbol: String = "",
    val portfolio: String = "",
    val data: String = "",
    val keyId: String = "",
    val value: Double = 0.0,
    val groupColor: Int = 0,
    val marker: Int = 0,
    val note: String = "",
    val dividendNote: String = "",
    val annualDividendRate: Double = -1.0,
    val alertAbove: Double = 0.0,
    val alertAboveNote: String = "",
    val alertBelow: Double = 0.0,
    val alertBelowNote: String = "",
    val color: Int = 0,
    val name: String = "",
    val quantity: Double = 0.0,
    val price: Double = 0.0,
    val type: Int = 0,
    val account: String = "",
    val date: Long = 0L,
    val sharesPerQuantity: Int = 1,
    val expirationDate: Long = 0L,
    val premium: Double = 0.0,
    val fee: Double = 0.0,
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
    }

    private var dbDataList: MutableList<DBData> = mutableListOf()
    private var dbDataMap: MutableMap<String, MutableList<DBData>> = mutableMapOf()

    class HeadlineViewHolder(
        val binding: DbHeadlineTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class StockDBdataViewHolder(
        val binding: DbStockdbdataTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class GroupViewHolder(
        val binding: DbGroupTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class AssetViewHolder(
        val binding: DbAssetTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class EventViewHolder(
        val binding: DbEventTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class DividendViewHolder(
        val binding: DbDividendTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    class StoredataViewHolder(
        val binding: DbStoredataTypeItemBinding
    ) : BaseViewHolder<DBData>(binding.root) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<*> {

        return when (viewType) {
            db_headline_type -> {
                val binding =
                    DbHeadlineTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                HeadlineViewHolder(binding)
            }

            db_stockdbdata_type -> {
                val binding =
                    DbStockdbdataTypeItemBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                StockDBdataViewHolder(binding)
            }

            db_group_type -> {
                val binding =
                    DbGroupTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                GroupViewHolder(binding)
            }

            db_asset_type -> {
                val binding =
                    DbAssetTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                AssetViewHolder(binding)
            }

            db_event_type -> {
                val binding =
                    DbEventTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                EventViewHolder(binding)
            }

            db_dividend_type -> {
                val binding =
                    DbDividendTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                DividendViewHolder(binding)
            }

            db_storedata_type -> {
                val binding =
                    DbStoredataTypeItemBinding.inflate(LayoutInflater.from(context), parent, false)
                StoredataViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    //-----------onCreateViewHolder: bind view with data model---------
    override fun onBindViewHolder(
        holder: BaseViewHolder<*>,
        position: Int
    ) {

        val data: DBData = dbDataList[position]

        when (holder) {

            is HeadlineViewHolder -> {

                holder.binding.dbHeadline.text = data.title
            }

            is StockDBdataViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbStockdbdataLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbStockdbdataSymbol.text = getHeaderStr("symbol")
                    holder.binding.dbStockdbdataName.text = getHeaderStr("name")
                    holder.binding.dbStockdbdataPortfolio.text = getHeaderStr("portfolio")
                    holder.binding.dbStockdbdataType.text = getHeaderStr("type")
                    holder.binding.dbStockdbdataData.text = getHeaderStr("data")
                    holder.binding.dbStockdbdataGroupColor.text = getHeaderStr("groupColor")
                    holder.binding.dbStockdbdataMarker.text = getHeaderStr("marker")
                    holder.binding.dbStockdbdataNote.text = getHeaderStr("note")
                    holder.binding.dbStockdbdataDividendNote.text = getHeaderStr("dividendNote")
                    holder.binding.dbStockdbdataAnnualDividendRate.text =
                        getHeaderStr("annualDividendRate")
                    holder.binding.dbStockdbdataAlertAbove.text = getHeaderStr("alertAbove")
                    holder.binding.dbStockdbdataAlertAboveNote.text = getHeaderStr("alertAboveNote")
                    holder.binding.dbStockdbdataAlertBelow.text = getHeaderStr("alertBelow")
                    holder.binding.dbStockdbdataAlertBelowNote.text = getHeaderStr("alertBelowNote")
                } else {
                    holder.binding.dbStockdbdataLayout.setBackgroundColor(Color.rgb(0, 148, 255))
                    holder.binding.dbStockdbdataSymbol.text = data.symbol
                    holder.binding.dbStockdbdataName.text = data.name
                    holder.binding.dbStockdbdataPortfolio.text = data.portfolio
                    holder.binding.dbStockdbdataType.text =
                        dataProviderFromInt(data.type).toString()
                    holder.binding.dbStockdbdataData.text = data.data
                    holder.binding.dbStockdbdataGroupColor.text = getColorStr(data.groupColor)
                    holder.binding.dbStockdbdataMarker.text = if (data.marker > 0) {
                        holder.binding.dbStockdbdataMarker.setBackgroundColor(
                            getMarkerColor(
                                context,
                                data.marker
                            )
                        )
                        "${getMarkerText(context, data.marker)}"
                    } else {
                        holder.binding.dbStockdbdataMarker.setBackgroundColor(
                            Color.rgb(0, 148, 255)
                        )
                        ""
                    }
                    holder.binding.dbStockdbdataNote.text = data.note
                    holder.binding.dbStockdbdataDividendNote.text = data.dividendNote
                    holder.binding.dbStockdbdataAnnualDividendRate.text =
                        if (data.annualDividendRate >= 0.0) {
                            DecimalFormat(DecimalFormat2To4Digits).format(data.annualDividendRate)
                        } else {
                            ""
                        }
                    holder.binding.dbStockdbdataAlertAbove.text = if (data.alertAbove > 0.0) {
                        DecimalFormat(DecimalFormat2To4Digits).format(data.alertAbove)
                    } else {
                        ""
                    }
                    holder.binding.dbStockdbdataAlertAboveNote.text = data.alertAboveNote
                    holder.binding.dbStockdbdataAlertBelow.text = if (data.alertBelow > 0.0) {
                        DecimalFormat(DecimalFormat2To4Digits).format(data.alertBelow)
                    } else {
                        ""
                    }
                    holder.binding.dbStockdbdataAlertBelowNote.text = data.alertBelowNote
                }
            }

            is GroupViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbGroupLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbGroupColor.text = getHeaderStr("color")
                    holder.binding.dbGroupName.text = getHeaderStr("name")
                } else {
                    holder.binding.dbGroupLayout.setBackgroundColor(Color.rgb(255, 127, 182))
                    holder.binding.dbGroupColor.text = getColorStr(data.color)
                    holder.binding.dbGroupName.text = data.name
                }

//      groupTableRowsCount = items.size
//
//      items.forEach { groupItem ->
//        groupTableRows.append("<tr>")
//        groupTableRows.append("<td>${getColorStr(groupItem.color)}</td>")
//        groupTableRows.append("<td>${groupItem.name}</td>")
//        groupTableRows.append("</tr>")
//      }

            }

            is AssetViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbAssetLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbAssetId.text = getHeaderStr("id")
                    holder.binding.dbAssetSymbol.text = getHeaderStr("symbol")
                    holder.binding.dbAssetQuantity.text = getHeaderStr("quantity")
                    holder.binding.dbAssetPrice.text = getHeaderStr("price")
                    holder.binding.dbAssetType.text = getHeaderStr("type")
                    holder.binding.dbAssetAccount.text = getHeaderStr("account")
                    holder.binding.dbAssetNote.text = getHeaderStr("note")
                    holder.binding.dbAssetDate.text = getHeaderStr("date")
                    holder.binding.dbAssetSharesPerQuantity.text = getHeaderStr("sharesPerQuantity")
                    holder.binding.dbAssetExpirationDate.text = getHeaderStr("expirationDate")
                    holder.binding.dbAssetPremium.text = getHeaderStr("premium")
                    holder.binding.dbAssetFee.text = getHeaderStr("fee")
                } else {
                    holder.binding.dbAssetLayout.setBackgroundColor(Color.rgb(255, 106, 0))
                    holder.binding.dbAssetId.text = data.id?.toString() ?: ""
                    holder.binding.dbAssetSymbol.text = data.symbol
                    holder.binding.dbAssetQuantity.text =
                        DecimalFormat(DecimalFormatQuantityDigits).format(data.quantity)
                    holder.binding.dbAssetPrice.text =
                        DecimalFormat(DecimalFormat0To4Digits).format(data.price)
                    holder.binding.dbAssetType.text = data.type.toString()
                    holder.binding.dbAssetAccount.text = data.account.toString()
                    holder.binding.dbAssetNote.text = data.note
                    holder.binding.dbAssetDate.text = getDateTimeStr(data.date)
                    holder.binding.dbAssetSharesPerQuantity.text = "${data.sharesPerQuantity}"
                    holder.binding.dbAssetExpirationDate.text = getDateStr(data.expirationDate)
                    holder.binding.dbAssetPremium.text =
                        if (data.premium > 0.0) DecimalFormat(DecimalFormat0To4Digits).format(
                            data.premium
                        ) else ""
                    holder.binding.dbAssetFee.text =
                        if (data.fee > 0.0) DecimalFormat(DecimalFormat0To4Digits).format(
                            data.fee
                        ) else ""
                }
//      assetTableRowsCount = items.size
//
//      items.forEach { assetItem ->
//        assetTableRows.append("<tr>")
//        assetTableRows.append("<td>${assetItem.id}</td>")
//        assetTableRows.append("<td>${assetItem.symbol}</td>")
//        assetTableRows.append("<td>${DecimalFormat(DecimalFormat0To8Digits).format(assetItem.quantity)}</td>")
//        assetTableRows.append("<td>${assetItem.price}</td>")
//        assetTableRows.append("<td>${assetItem.type}</td>")
//        assetTableRows.append("<td>${assetItem.note}</td>")
//        assetTableRows.append("<td>${getDateTimeStr(assetItem.date)}</td>")
//        assetTableRows.append("<td>${assetItem.sharesPerQuantity}</td>")
//        assetTableRows.append("<td>${getDateStr(assetItem.expirationDate)}</td>")
//        assetTableRows.append(
//            "<td>${if (assetItem.premium != 0.0) assetItem.premium else ""}</td>"
//        )
//        assetTableRows.append(
//            "<td>${if (assetItem.fee != 0.0) assetItem.fee else ""}</td>"
//        )
//        assetTableRows.append("</tr>")
//      }

            }

            is EventViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbEventLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbEventId.text = getHeaderStr("id")
                    holder.binding.dbEventSymbol.text = getHeaderStr("symbol")
                    holder.binding.dbEventTitle.text = getHeaderStr("title")
                    holder.binding.dbEventDatetime.text = getHeaderStr("datetime")
                    holder.binding.dbEventType.text = getHeaderStr("type")
                    holder.binding.dbEventNote.text = getHeaderStr("note")
                } else {
                    holder.binding.dbEventLayout.setBackgroundColor(Color.rgb(127, 255, 197))
                    holder.binding.dbEventId.text = data.id?.toString() ?: ""
                    holder.binding.dbEventSymbol.text = data.symbol
                    holder.binding.dbEventTitle.text = data.title
                    holder.binding.dbEventDatetime.text = getDateTimeStr(data.datetime)
                    holder.binding.dbEventType.text = data.type.toString()
                    holder.binding.dbEventNote.text = data.note
                }

//      eventTableRowsCount = items.size
//
//      items.forEach { eventItem ->
//        eventTableRows.append("<tr>")
//        eventTableRows.append("<td>${eventItem.id}</td>")
//        eventTableRows.append("<td>${eventItem.symbol}</td>")
//        eventTableRows.append("<td>${eventItem.type}</td>")
//        eventTableRows.append("<td>${eventItem.title}</td>")
//        eventTableRows.append("<td>${eventItem.note}</td>")
//        eventTableRows.append("<td>${getDateTimeStr(eventItem.datetime)}</td>")
//        eventTableRows.append("</tr>")
//      }

            }

            is DividendViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbDividendLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbDividendId.text = getHeaderStr("id")
                    holder.binding.dbDividendSymbol.text = getHeaderStr("symbol")
                    holder.binding.dbDividendAmount.text = getHeaderStr("amount")
                    holder.binding.dbDividendCycle.text = getHeaderStr("cycle")
                    holder.binding.dbDividendPaydate.text = getHeaderStr("paydate")
                    holder.binding.dbDividendType.text = getHeaderStr("type")
                    holder.binding.dbDividendAccount.text = getHeaderStr("account")
                    holder.binding.dbDividendExdate.text = getHeaderStr("exdate")
                    holder.binding.dbDividendNote.text = getHeaderStr("note")
                } else {
                    holder.binding.dbDividendLayout.setBackgroundColor(Color.rgb(255, 233, 127))
                    holder.binding.dbDividendId.text = data.id?.toString() ?: ""
                    holder.binding.dbDividendSymbol.text = data.symbol
                    holder.binding.dbDividendAmount.text =
                        DecimalFormat(DecimalFormat2To4Digits).format(data.amount)
                    holder.binding.dbDividendCycle.text = data.cycle.toString()
                    holder.binding.dbDividendPaydate.text = getDateStr(data.paydate)
                    holder.binding.dbDividendType.text = data.type.toString()
                    holder.binding.dbDividendAccount.text = data.account.toString()
                    holder.binding.dbDividendExdate.text = getDateStr(data.exdate)
                    holder.binding.dbDividendNote.text = data.note
                }

//      dividendTableRowsCount = items.size
//
//      items.forEach { dividendItem ->
//        dividendTableRows.append("<tr>")
//        dividendTableRows.append("<td>${dividendItem.id}</td>")
//        dividendTableRows.append("<td>${dividendItem.symbol}</td>")
//        dividendTableRows.append("<td>${DecimalFormat(DecimalFormat2To4Digits).format(dividendItem.amount)}</td>")
//        dividendTableRows.append("<td>${dividendItem.type}</td>")
//        dividendTableRows.append("<td>${dividendItem.cycle}</td>")
//        dividendTableRows.append("<td>${getDateStr(dividendItem.paydate)}</td>")
//        dividendTableRows.append("<td>${getDateStr(dividendItem.exdate)}</td>")
//        dividendTableRows.append("<td>${dividendItem.note}</td>")
//        dividendTableRows.append("</tr>")
//      }

            }

            is StoredataViewHolder -> {

                if (data.isHeader) {
                    holder.binding.dbStoredataLayout.setBackgroundColor(Color.rgb(139, 0, 0))
                    holder.binding.dbStoredataKeyid.text = getHeaderStr("keyId")
                    holder.binding.dbStoredataData.text = getHeaderStr("data")
                    holder.binding.dbStoredataValue.text = getHeaderStr("value")
                    holder.binding.dbStoredataDatetime.text = getHeaderStr("datetime")
                    holder.binding.dbStoredataType.text = getHeaderStr("type")
                    holder.binding.dbStoredataNote.text = getHeaderStr("note")
                } else {
                    holder.binding.dbStoredataLayout.setBackgroundColor(Color.rgb(255, 0, 110))
                    holder.binding.dbStoredataKeyid.text = data.keyId
                    holder.binding.dbStoredataData.text = data.data
                    holder.binding.dbStoredataValue.text = data.value.toString()
                    holder.binding.dbStoredataDatetime.text = getDateTimeStr(data.datetime)
                    holder.binding.dbStoredataType.text = data.type.toString()
                    holder.binding.dbStoredataNote.text = data.note
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
            val gmtDateTime: LocalDateTime =
                LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
            val localDateTime: ZonedDateTime =
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneOffset.systemDefault())
            val dateTimeStr =
                "${datetime}\n${
                    gmtDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                }\nLokales Datum: ${
                    localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                }"
            dateTimeStr
        } else {
            ""
        }
    }

    private fun getDateTimeStr(datetime: Long): String {
        return if (datetime != 0L) {
            val gmtDateTime: LocalDateTime =
                LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
            val localDateTime: ZonedDateTime =
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneOffset.systemDefault())
            val dateTimeStr =
                "${datetime}\n${
                    gmtDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                } ${
                    gmtDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
                }\nLokale Zeit: ${
                    localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                } ${
                    localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
                }"
            dateTimeStr
        } else {
            ""
        }
    }

    private fun getHeaderStr(text: String): SpannableStringBuilder =
        SpannableStringBuilder()
            .color(Color.WHITE) {
                bold { append(text) }
            }

    private fun getHeaderStr2(text: String): SpannableStringBuilder =
        SpannableStringBuilder()
            .backgroundColor(Color.rgb(139, 0, 0)) {
                color(Color.WHITE) {
                    bold { append(text) }
                }
            }

    private fun getColorStr(color: Int): SpannableStringBuilder =
        if (color == 0) {
            SpannableStringBuilder()
        } else {
            val hexStr = "0x${color.toHexString()}"
            val colorCode = hexStr.replace("0xff", "#")
            SpannableStringBuilder()
                .append("$color\n")
                .backgroundColor(Color.WHITE) {
                    color(color) {
                        append("▐█████▌\n")
                    }
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

    private fun updateList() {

        dbDataList.clear()
        dbDataMap.toSortedMap()
            .forEach { (_, dbData) ->
                dbDataList.addAll(dbData)
            }

        notifyDataSetChanged()
    }

    fun updateStockDBdata(data: List<StockDBdata>) {

        this.dbDataMap["0_stock_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\nstock_table (${data.size})"
            ),
            DBData(
                viewType = db_stockdbdata_type,
                isHeader = true
            )
        )

        this.dbDataMap["0_stock_table"]?.addAll(
            data
                //.take(2)
                .map { stockDBdata ->
                    DBData(
                        viewType = db_stockdbdata_type,
                        symbol = stockDBdata.symbol,
                        name = stockDBdata.name,
                        portfolio = stockDBdata.portfolio,
                        type = stockDBdata.type,
                        data = stockDBdata.data,
                        groupColor = stockDBdata.groupColor,
                        marker = stockDBdata.marker,
                        note = stockDBdata.note,
                        dividendNote = stockDBdata.dividendNote,
                        annualDividendRate = stockDBdata.annualDividendRate,
                        alertAbove = stockDBdata.alertAbove,
                        alertAboveNote = stockDBdata.alertAboveNote,
                        alertBelow = stockDBdata.alertBelow,
                        alertBelowNote = stockDBdata.alertBelowNote
                    )
                })

        updateList()
    }

    fun updateGroup(data: List<Group>) {

        this.dbDataMap["1_group_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\ngroup_table (${data.size})"
            ),
            DBData(
                viewType = db_group_type,
                isHeader = true
            )
        )

        this.dbDataMap["1_group_table"]?.addAll(data.map { group ->
            DBData(
                viewType = db_group_type,
                color = group.color,
                name = group.name
            )
        })

        updateList()
    }

    fun updateAsset(data: List<Asset>) {

        this.dbDataMap["2_asset_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\nasset_table (${data.size})"
            ),
            DBData(
                viewType = db_asset_type,
                isHeader = true
            )
        )

        this.dbDataMap["2_asset_table"]?.addAll(
            data
                //.take(2)
                .map { asset ->
                    DBData(
                        viewType = db_asset_type,
                        id = asset.id,
                        symbol = asset.symbol,
                        quantity = asset.quantity,
                        price = asset.price,
                        type = asset.type,
                        account = asset.account,
                        note = asset.note,
                        date = asset.date,
                        sharesPerQuantity = asset.sharesPerQuantity,
                        expirationDate = asset.expirationDate,
                        premium = asset.premium,
                        fee = asset.fee
                    )
                })

        updateList()
    }

    fun updateEvent(data: List<Event>) {

        this.dbDataMap["3_event_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\nevent_table (${data.size})"
            ),
            DBData(
                viewType = db_event_type,
                isHeader = true
            )
        )

        this.dbDataMap["3_event_table"]?.addAll(
            data
                //.take(2)
                .map { event ->
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

        this.dbDataMap["4_dividend_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\ndividend_table (${data.size})"
            ),
            DBData(
                viewType = db_dividend_type,
                isHeader = true
            )
        )

        this.dbDataMap["4_dividend_table"]?.addAll(
            data
                //.take(2)
                .map { dividend ->
                    DBData(
                        viewType = db_dividend_type,
                        id = dividend.id,
                        symbol = dividend.symbol,
                        amount = dividend.amount,
                        cycle = dividend.cycle,
                        paydate = dividend.paydate,
                        type = dividend.type,
                        account = dividend.account,
                        exdate = dividend.exdate,
                        note = dividend.note
                    )
                })

        updateList()
    }

    fun updateStoreData(data: List<StoreData>) {

        this.dbDataMap["5_store_table"] = mutableListOf(
            DBData(
                viewType = db_headline_type,
                title = "\nstore_table (${data.size})"
            ),
            DBData(
                viewType = db_storedata_type,
                isHeader = true
            )
        )

        this.dbDataMap["5_store_table"]?.addAll(
            data
                //.take(2)
                .map { storedata ->
                    DBData(
                        viewType = db_storedata_type,
                        keyId = storedata.keyId,
                        data = storedata.data,
                        value = storedata.value,
                        datetime = storedata.datetime,
                        type = storedata.type,
                        note = storedata.note
                    )
                })

        updateList()
    }

    override fun getItemCount() = dbDataList.size
}
