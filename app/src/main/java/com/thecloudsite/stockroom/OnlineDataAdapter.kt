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
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.thecloudsite.stockroom.StockMarketDataRepository.StockRawMarketDataRepository
import com.thecloudsite.stockroom.databinding.OnlinedataviewItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.enNumberStrToDouble
import com.thecloudsite.stockroom.utils.formatInt
import com.thecloudsite.stockroom.utils.getFormatStr
import com.thecloudsite.stockroom.utils.isOnline
import com.thecloudsite.stockroom.utils.minValueCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class OnlineData(
  val desc: String,
  val text: SpannableStringBuilder
)

class OnlineDataAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<OnlineDataAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<OnlineData>()

  private var stockSymbol: StockSymbol = StockSymbol()

  private var detailViewClickCounter = 0

  class OnlineDataViewHolder(
    val binding: OnlinedataviewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
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
            || key == "startDate"
            || key == "expireDate"

        if (dateTimeKey && valueString.matches("^\\d+$".toRegex())
        ) {
          val datetime = valueString.toLong()
          val gmtOffSet = gmtOffSetMilliseconds / 1000
          val gmtOffsetDateTime: LocalDateTime =
            LocalDateTime.ofEpochSecond(datetime + gmtOffSet, 0, ZoneOffset.UTC)
          val localDateTime: ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneOffset.systemDefault())
          val dateTimeStr =
            "#$valueString <i>(Ortszeit: ${
              gmtOffsetDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
            } ${
              gmtOffsetDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
            }, Lokale Zeit: ${
              localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
            } ${
              localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
            })</i>#"

          element.setValue(JsonPrimitive(dateTimeStr))
        }

        if (key == "firstTradeDateMilliseconds" && valueString.matches("^\\d+$".toRegex())) {
          val datetimeMilliseconds = valueString.toLong()
          val gmtOffsetDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(
            (datetimeMilliseconds + gmtOffSetMilliseconds) / 1000,
            0,
            ZoneOffset.UTC
          )
          val localDateTime: ZonedDateTime =
            ZonedDateTime.ofInstant(
              Instant.ofEpochSecond(datetimeMilliseconds / 1000),
              ZoneOffset.systemDefault()
            )
          val dateTimeStr =
            "#$valueString <i>(Ortszeit: ${
              gmtOffsetDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
            } ${
              gmtOffsetDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
            }, Lokale Zeit:${
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

        // Add size abbreviation.
        if (key == "marketCap"
          || key == "sharesOutstanding"
          || key == "circulatingSupply"
          || key == "openInterest"
          || key.contains("volume", true)
        ) {
          val formattedLong = formatInt(valueString.toLong())
          element.setValue(JsonPrimitive("#$valueString<i>${formattedLong.second}</i>#"))
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

    val binding = OnlinedataviewItemBinding.inflate(inflater, parent, false)

    binding.root.setOnClickListener {

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
              onlineRawJsonData = stockRawMarketDataRepository.getStockRawData(stockSymbol.symbol)
            }
          }

          // Convert the data.
          var onlineJsonData = ""
          try {
            val gson: Gson = GsonBuilder()
              .setPrettyPrinting()
              .create()

            val jsonObj = JsonParser.parseString(onlineRawJsonData).asJsonObject

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
            .setTitle(context.getString(R.string.data_provider_details, stockSymbol.symbol))
            .setMessage(htmlText)
            .setPositiveButton(R.string.ok) { _, _ ->
            }
            .show()
        }
      }
    }

    return OnlineDataViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current: OnlineData = data[position]

    holder.binding.textViewOnlineData.text = current.text
    holder.binding.textViewOnlineDataDesc.text = current.desc
  }

  // first: abbr
  // second: add optional (abbr)
  private fun formatInt(value: Long): Pair<SpannableStringBuilder, String> {
    return formatInt(value, context)
  }

  private fun formatDoubleNegInRed(
    formatStr: String,
    value: Double
  ): SpannableStringBuilder {
    // only negative values in red
    if (value.isNaN()) {
      // requested value is not in the JSON data
      return SpannableStringBuilder().append(context.getString(R.string.onlinedata_not_applicable))
    }
    val str = DecimalFormat(formatStr).format(minValueCheck(value))
    return if (value < 0.0) {
      SpannableStringBuilder().bold {
        color(context.getColor(R.color.red)) {
          append(str)
        }
      }
    } else {
      SpannableStringBuilder().bold { append(str) }
    }
  }

  private fun formatDoubleNegInRed2(
    value: Double
  ): SpannableStringBuilder = formatDoubleNegInRed(DecimalFormat2Digits, value)

  private fun formatDoubleNegInRed2To4(
    value: Double
  ): SpannableStringBuilder = formatDoubleNegInRed(getFormatStr(value), value)

  private fun formatDouble(
    formatStr: String,
    value: Double
  ): SpannableStringBuilder {
    return if (value.isNaN()) {
      // requested value is not in the JSON data
      SpannableStringBuilder().append(context.getString(R.string.onlinedata_not_applicable))
    } else {

      SpannableStringBuilder().bold {
        append(
          DecimalFormat(formatStr).format(minValueCheck(value))
        )
      }
    }
  }

  private fun formatDouble2To4(
    value: Double
  ): SpannableStringBuilder = formatDouble(getFormatStr(value), value)

  private fun convertRangeStr(rangeStr: String): SpannableStringBuilder {

    // "1.23 - 4.56" = "1,23 - 4,56"
    // "1.0E-4 - 2.00" = "0,0001 - 2,00"

    val delimiter = " - "
    val rangeList = rangeStr.split(delimiter)

    if (rangeList.size == 2) {
      val value1 = enNumberStrToDouble(rangeList[0])
      val rangeStr1 =
        DecimalFormat(getFormatStr(value1)).format(value1)

      val value2 = enNumberStrToDouble(rangeList[1])
      val rangeStr2 =
        DecimalFormat(getFormatStr(value2)).format(value2)

      return SpannableStringBuilder().bold {
        append("$rangeStr1$delimiter$rangeStr2")
      }
    }

    return SpannableStringBuilder().append(rangeStr)
  }

  fun updateData(stockSymbol: StockSymbol, onlineMarketData: OnlineMarketData) {
    data.clear()

    this.stockSymbol = stockSymbol

    // val separatorChar: Char = DecimalFormatSymbols.getInstance().decimalSeparator

    if (stockSymbol.type == DataProvider.Standard) {
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_regularMarketPreviousClose),
          text = formatDouble2To4(onlineMarketData.regularMarketPreviousClose)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_regularMarketOpen),
          text = formatDouble2To4(onlineMarketData.regularMarketOpen)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_fiftyDayAverage),
          text = formatDouble2To4(onlineMarketData.fiftyDayAverage)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_twoHundredDayAverage),
          text = formatDouble2To4(onlineMarketData.twoHundredDayAverage)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_fiftyTwoWeekRange),
          text = convertRangeStr(onlineMarketData.fiftyTwoWeekRange)
          //onlineMarketData.fiftyTwoWeekRange.replace('.', separatorChar)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_regularMarketDayRange),
          text = convertRangeStr(onlineMarketData.regularMarketDayRange)
          //onlineMarketData.regularMarketDayRange.replace('.', separatorChar)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_regularMarketVolume),
          text = formatInt(onlineMarketData.regularMarketVolume).first
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_sharesOutstanding),
          text = formatInt(onlineMarketData.sharesOutstanding).first
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_marketCap),
          text = formatInt(onlineMarketData.marketCap).first
        )
      )

/*
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendRate),
            text = DecimalFormat(DecimalFormat2To4Digits.format(onlineMarketData.annualDividendRate)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendYield),
            text = "${DecimalFormat(DecimalFormat2To4Digits).format(
                onlineMarketData.annualDividendYield * 100.0
            )}%"
        )
    )
 */

      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_epsTrailingTwelveMonths),
          text = formatDoubleNegInRed2To4(onlineMarketData.epsTrailingTwelveMonths)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_epsCurrentYear),
          text = formatDoubleNegInRed2To4(onlineMarketData.epsCurrentYear)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_epsForward),
          text = formatDoubleNegInRed2To4(onlineMarketData.epsForward)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_trailingPE),
          text = formatDoubleNegInRed2To4(onlineMarketData.trailingPE)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_priceEpsCurrentYear),
          text = formatDoubleNegInRed2To4(onlineMarketData.priceEpsCurrentYear)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_forwardPE),
          text = formatDoubleNegInRed2To4(onlineMarketData.forwardPE)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_bookValue),
          text = formatDoubleNegInRed2To4(onlineMarketData.bookValue)
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_priceToBook),
          text = formatDoubleNegInRed2(onlineMarketData.priceToBook)
        )
      )

      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_market),
          text = SpannableStringBuilder().bold {
            append(
              onlineMarketData.market.replace('_', ' ')
            )
          }
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_fullExchangeName),
          text = SpannableStringBuilder().bold { append(onlineMarketData.fullExchangeName) }
        )
      )
      data.add(
        OnlineData(
          desc = context.getString(R.string.onlinedata_marketState),
          text = SpannableStringBuilder().bold {
            append(
              getMarketText(context, onlineMarketData.marketState)
            )
          }
        )
      )
    }

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size
}
