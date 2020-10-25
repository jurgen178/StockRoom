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
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.thecloudsite.stockroom.utils.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class OnlineData(
  val desc: String,
  val text: String
)

class OnlineDataAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<OnlineDataAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<OnlineData>()

  private var symbol = ""

  private var detailViewClickCounter = 0

  class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val itemViewOnlineDataDesc: TextView = itemView.findViewById(R.id.textViewOnlineDataDesc)
    val itemViewOnlineData: TextView = itemView.findViewById(R.id.textViewOnlineData)
  }

  private fun processJsonObject(
    jsonObj: JsonObject,
    sorted: Boolean
  ) {

    // Sort json elements.
    if (sorted) {
      val sortedMap = sortedMapOf<String, JsonElement?>()
      val jsonObjCopy = jsonObj.deepCopy()

      // Remove all items.
      jsonObjCopy.entrySet()
          .forEach { element ->
            val key = element.key
            val value = element.value
            sortedMap[key] = value
            jsonObj.remove(key)
          }

      // Add back the items sorted.
      sortedMap.forEach { (key, jsonElement) ->
        jsonObj.add(key, jsonElement)
      }
    }

    val gmtOffSetMilliseconds = jsonObj["gmtOffSetMilliseconds"]?.asLong ?: 0L

    // Enumerate all json objects.
    jsonObj.entrySet()
        .forEach { element ->

          val key = element.key
          val value = element.value
          val valueString = value.toString()

          // Add date-time to time values.
          val dateTimeKey = key.contains("time", true)
              || key.contains("dividenddate", true)
          if (dateTimeKey && valueString.matches("^\\d+$".toRegex())
          ) {
            val datetime = valueString.toLong()
            val gmtOffSet = gmtOffSetMilliseconds / 1000
            val localDateTime: LocalDateTime =
              LocalDateTime.ofEpochSecond(datetime + gmtOffSet, 0, ZoneOffset.UTC)
            val dateTimeStr =
              "#$valueString <i>(${
                localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
              } ${
                localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
              })</i>#"

            element.setValue(JsonPrimitive(dateTimeStr))
          }

          if (key == "firstTradeDateMilliseconds" && valueString.matches("^\\d+$".toRegex())) {
            val datetimeMilliseconds = valueString.toLong()
            val localDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(
                (datetimeMilliseconds + gmtOffSetMilliseconds) / 1000, 0, ZoneOffset.UTC
            )
            val dateTimeStr =
              "#$valueString <i>(${
                localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
              } ${
                localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
              })</i>#"

            element.setValue(JsonPrimitive(dateTimeStr))
          }

          // Price/Change in bold
          if (key.endsWith("MarketPrice")
              || key.endsWith("MarketChange")
              || key.endsWith("MarketChangePercent")
          ) {
            element.setValue(JsonPrimitive("#<b>$valueString</b>#"))
          }

          // Add MarketCap size abbreviation.
          if (key == "marketCap") {
            val formattedMarketCap = formatInt(valueString.toLong())
            val marketCap = "#$valueString <i>($formattedMarketCap)</i>#"
            element.setValue(JsonPrimitive(marketCap))
          }

          // Recursion for json array.
          if (value is JsonArray) {
            for (jsonElement in value.iterator()) {
              processJsonObject(jsonElement.asJsonObject, sorted)
            }
          }

          // Recursion for json object.
          if (value is JsonObject) {
            processJsonObject(value, sorted)
          }
        }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.onlinedataview_item, parent, false)

    itemView.setOnClickListener {

      if (isOnline(context)) {
        detailViewClickCounter++

        if (detailViewClickCounter > 4) {
          detailViewClickCounter = 0

          // Get Raw data from the server.
          val stockRawMarketDataRepository: StockRawMarketDataRepository =
            StockRawMarketDataRepository { StockRawMarketDataApiFactory.yahooApi }

          val onlineRawJsonData: String
          runBlocking {
            withContext(Dispatchers.IO) {
              onlineRawJsonData = stockRawMarketDataRepository.getStockRawData(symbol)
            }
          }

          // Convert the data.
          var onlineJsonData = ""
          try {
            val gson: Gson = GsonBuilder()
                .setPrettyPrinting()
                .create()

            val parser = JsonParser()
            val jsonObj = parser.parse(onlineRawJsonData).asJsonObject

            // Add sorted json data.
            val jsonObjSorted = jsonObj.deepCopy()
            processJsonObject(jsonObjSorted, true)
            onlineJsonData += "<b>${context.getString(R.string.data_provider_details_sorted)}</b>\n"
            // Android supports only color and face attribute, no style attr.
            // No space within the attr because they get replaced with &nbsp; and invalidate the attr.
            onlineJsonData += "<font color='grey'face='monospace'>${
              gson.toJson(
                  jsonObjSorted
              )
            }</font>"

            // Add unsorted json data.
            val jsonObjUnsorted = jsonObj.deepCopy()
            processJsonObject(jsonObjUnsorted, false)
            onlineJsonData += "\n\n<b>${
              context.getString(
                  R.string.data_provider_details_unsorted
              )
            }</b>\n"
            onlineJsonData += "<font color='grey'face='monospace'>${
              gson.toJson(
                  jsonObjUnsorted
              )
            }</font>"
          } catch (e: Exception) {
            Log.d("Convert json data failed", e.toString())
            onlineJsonData = onlineRawJsonData
          }

          // Un-escape.
          onlineJsonData = onlineJsonData.replace(" ", "&nbsp;")
              .replace("\\u003c", "<")
              .replace("\\u003e", ">")
              .replace("\n", "<br>")
              .replace("\"#", "")
              .replace("#\"", "")
              .replace("<font&nbsp;", "<font ")

          // Convert to html spannable string.
          val htmlText = HtmlCompat.fromHtml(onlineJsonData, HtmlCompat.FROM_HTML_MODE_LEGACY)
              .toSpannable()

          // Set small font.
          htmlText.setSpan(AbsoluteSizeSpan(10, true), 0, htmlText.length, SPAN_INCLUSIVE_INCLUSIVE)

          // Display the data.
          android.app.AlertDialog.Builder(context)
              .setTitle(context.getString(R.string.data_provider_details, symbol))
              .setMessage(htmlText)
              .setPositiveButton(R.string.ok) { _, _ ->
              }
              .show()
        }
      }
    }

    return OnlineDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current: OnlineData = data[position]

    holder.itemViewOnlineDataDesc.text = current.desc
    holder.itemViewOnlineData.text = current.text
  }

  private fun formatInt(value: Long): String {
    return when {
      value >= 1000000000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000000000.0)}${
          context.getString(
              R.string.trillion_abbr
          )
        }"
      }
      value >= 1000000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000000.0)}${
          context.getString(
              R.string.billion_abbr
          )
        }"
      }
      value >= 1000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000.0)}${
          context.getString(
              R.string.million_abbr
          )
        }"
      }
      else -> {
        DecimalFormat("0.##").format(value)
      }
    }
  }

  fun updateData(onlineMarketData: OnlineMarketData) {
    data.clear()

    symbol = onlineMarketData.symbol

    val separatorChar: Char = DecimalFormatSymbols.getInstance()
        .decimalSeparator

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketPreviousClose),
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketPreviousClose)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketOpen),
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketOpen)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fiftyDayAverage),
            text = DecimalFormat("0.00").format(onlineMarketData.fiftyDayAverage)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_twoHundredDayAverage),
            text = DecimalFormat("0.00").format(onlineMarketData.twoHundredDayAverage)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fiftyTwoWeekRange),
            text = onlineMarketData.fiftyTwoWeekRange.replace('.', separatorChar)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketDayRange),
            text = onlineMarketData.regularMarketDayRange.replace('.', separatorChar)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketVolume),
            text = formatInt(onlineMarketData.regularMarketVolume)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_marketCap),
            text = formatInt(onlineMarketData.marketCap)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_forwardPE),
            text = DecimalFormat("0.00").format(onlineMarketData.forwardPE)
        )
    )

/*
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendRate),
            text = DecimalFormat("0.00##").format(onlineMarketData.annualDividendRate)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendYield),
            text = "${DecimalFormat("0.00##").format(
                onlineMarketData.annualDividendYield * 100.0
            )}%"
        )
    )
 */

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_epsTrailingTwelveMonths),
            text = DecimalFormat("0.00").format(onlineMarketData.epsTrailingTwelveMonths)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_epsCurrentYear),
            text = DecimalFormat("0.00").format(onlineMarketData.epsCurrentYear)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_epsForward),
            text = DecimalFormat("0.00").format(onlineMarketData.epsForward)
        )
    )

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_region),
            text = onlineMarketData.region
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_language),
            text = onlineMarketData.language
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fullExchangeName),
            text = onlineMarketData.fullExchangeName
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_messageBoardId),
            text = onlineMarketData.messageBoardId
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_financialCurrency),
            text = onlineMarketData.financialCurrency
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_marketState),
            text = onlineMarketData.marketState
        )
    )

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size
}
