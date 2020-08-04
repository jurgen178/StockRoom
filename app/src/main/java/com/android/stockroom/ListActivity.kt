package com.android.stockroom

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_list.webview
import okhttp3.internal.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

class ListActivity : AppCompatActivity() {

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private val stockTable = StringBuilder()
  private val groupTable = StringBuilder()
  private val assetTable = StringBuilder()
  private val eventTable = StringBuilder()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_list)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

/*
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let { stockItemSet ->
        val htmlText = updateHtmlText(stockItemSet)
        val mimeType: String = "text/html"
        val utfType: String = "UTF-8"
        webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null);
      }
    })
  */

    stockRoomViewModel.allProperties.observe(this, Observer { items ->

      items.forEach { stockItem ->
        stockTable.append("<tr>")
        stockTable.append("<td>${stockItem.symbol}</td>")
        stockTable.append("<td>${getColorStr(stockItem.groupColor)}</td>")
        stockTable.append("<td>${stockItem.notes}</td>")
        stockTable.append("<td>${DecimalFormat("0.########").format(stockItem.alertAbove)}</td>")
        stockTable.append(
            "<td>${DecimalFormat("0.########")
                .format(stockItem.alertBelow)}</td>"
        )
        stockTable.append("</tr>")
      }

      stockRoomViewModel.allProperties.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allGroupTable.observe(this, Observer { items ->

      items.forEach { groupItem ->
        groupTable.append("<tr>")
        groupTable.append("<td>${getColorStr(groupItem.color)}</td>")
        groupTable.append("<td>${groupItem.name}</td>")
        groupTable.append("</tr>")
      }

      stockRoomViewModel.allGroupTable.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allAssetTable.observe(this, Observer { items ->

      items.forEach { assetItem ->
        assetTable.append("<tr>")
        assetTable.append("<td>${assetItem.id}</td>")
        assetTable.append("<td>${assetItem.symbol}</td>")
        assetTable.append("<td>${DecimalFormat("0.########").format(assetItem.shares)}</td>")
        assetTable.append("<td>${assetItem.price}</td>")
        assetTable.append("</tr>")
      }

      stockRoomViewModel.allAssetTable.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allEventTable.observe(this, Observer { items ->

      items.forEach { eventItem ->
        eventTable.append("<tr>")
        eventTable.append("<td>${eventItem.id}</td>")
        eventTable.append("<td>${eventItem.symbol}</td>")
        eventTable.append("<td>${eventItem.type}</td>")
        eventTable.append("<td>${eventItem.title}</td>")
        eventTable.append("<td>${eventItem.note}</td>")
        eventTable.append("<td>${getDateTimeStr(eventItem.datetime)}</td>")
        eventTable.append("</tr>")
      }

      stockRoomViewModel.allEventTable.removeObservers(this)
      updateHtmlText()
    })
  }

  private fun getDateTimeStr(datetime: Long): String {
    val localDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
    val dateTimeStr =
      "${datetime}</br>${localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
      }&nbsp;${localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
      }"
    return dateTimeStr
  }

  private fun getColorStr(color: Int): String =
    if (color == 0) {
      "$color"
    } else {
      val hexStr = "0x${color.toHexString()}"
      val colorCode = hexStr.replace("0xff", "#")
      val colorSample =
        "<font style=\"background-color: $colorCode;\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>"
      "$color</br>$colorSample&nbsp;$colorCode"
    }

  private fun updateHtmlText() {
    var htmlText = resources.getRawTextFile(R.raw.debug)

    htmlText = htmlText.replace("<!-- stock_table -->", stockTable.toString())
    htmlText = htmlText.replace("<!-- group_table -->", groupTable.toString())
    htmlText = htmlText.replace("<!-- asset_table -->", assetTable.toString())
    htmlText = htmlText.replace("<!-- event_table -->", eventTable.toString())

    val mimeType: String = "text/html"
    val utfType: String = "UTF-8"
    webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null);
  }

  private fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader()
        .use { it.readText() }
}