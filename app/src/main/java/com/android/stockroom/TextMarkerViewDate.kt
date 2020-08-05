package com.android.stockroom

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TextMarkerViewCandleChart(
  context: Context,
  private val dateTimeFormatter: DateTimeFormatter,
  private val stockDataEntries: List<StockDataEntry>
) : MarkerView(context, R.layout.text_marker_layout) {
  private var tvContent = findViewById<TextView>(R.id.tvContent)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    e: Entry?,
    highlight: Highlight?
  ) {
    if (e is CandleEntry && stockDataEntries.isNotEmpty()) {
      val index: Int = e.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          LocalDateTime.ofEpochSecond(
              stockDataEntries[index].dateTimePoint, 0, ZoneOffset.UTC
          )
              .format(dateTimeFormatter)
/*
        val price = when (index) {
          0 -> AppPreferences.DECIMAL_FORMAT.format(e.open)
          stockDataEntries.size - 1 -> AppPreferences.DECIMAL_FORMAT.format(e.close)
          else -> if (e.low != e.high) {
            "${AppPreferences.DECIMAL_FORMAT.format(e.low)}..${AppPreferences.DECIMAL_FORMAT.format(
                e.high
            )}"
          } else {
            AppPreferences.DECIMAL_FORMAT.format(e.high)
          }
        }
      */
        val price =e.close
          tvContent.text = "${price}\n$date"
      } else {
        tvContent.text = ""
      }
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

class TextMarkerViewLineChart(
  context: Context,
  private val dateTimeFormatter: DateTimeFormatter,
  private val stockDataEntries: List<StockDataEntry>
) : MarkerView(context, R.layout.text_marker_layout) {
  private var tvContent = findViewById<TextView>(R.id.tvContent)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    e: Entry?,
    highlight: Highlight?
  ) {
    if (e is Entry && stockDataEntries.isNotEmpty()) {
      val index: Int = e.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          LocalDateTime.ofEpochSecond(
              stockDataEntries[index].dateTimePoint, 0, ZoneOffset.UTC
          )
              .format(dateTimeFormatter)
        //val price = AppPreferences.DECIMAL_FORMAT.format(e.y)
        val price = e.y
        tvContent.text = "${price}\n$date"
      } else {
        tvContent.text = ""
      }
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

