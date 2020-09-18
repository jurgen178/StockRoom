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
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import kotlinx.android.synthetic.main.stockroomlist_item.view.item_summary1
import kotlinx.android.synthetic.main.stockroomlist_item.view.item_summary2
import kotlinx.android.synthetic.main.stockroomlist_item.view.itemview_group
import java.text.DecimalFormat

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class StockRoomChartAdapter internal constructor(
  val context: Context,
  private val clickListenerGroup: (StockItem, View) -> Unit,
  private val clickListenerSummary: (StockItem) -> Unit,
) : ListAdapter<StockItem, StockRoomChartAdapter.StockRoomViewHolder>(StockRoomDiffCallback()) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  private var stockViewRange: StockViewRange = StockViewRange.OneDay
  private var stockViewMode: StockViewMode = StockViewMode.Line
  private var chartDataItems: HashMap<String, List<StockDataEntry>?> = hashMapOf()

  class StockRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindGroup(
      stockItem: StockItem,
      clickListener: (StockItem, View) -> Unit
    ) {
      itemView.itemview_group.setOnClickListener { clickListener(stockItem, itemView) }
    }

    fun bindSummary(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      itemView.item_summary1.setOnClickListener { clickListener(stockItem) }
      itemView.item_summary2.setOnClickListener { clickListener(stockItem) }
    }

    val itemViewSymbol: TextView = itemView.findViewById(R.id.textViewSymbol)
    val itemViewName: TextView = itemView.findViewById(R.id.textViewName)
    val itemViewMarketPrice: TextView = itemView.findViewById(R.id.textViewMarketPrice)
    val itemViewChange: TextView = itemView.findViewById(R.id.textViewChange)
    val itemViewChangePercent: TextView = itemView.findViewById(R.id.textViewChangePercent)
    val itemTextViewGroup: TextView = itemView.findViewById(R.id.itemview_group)
    val itemSummary: ConstraintLayout = itemView.findViewById(R.id.item_summary1)
    val itemRedGreen: ConstraintLayout = itemView.findViewById(R.id.item_summary2)
    val candleStickChart: CandleStickChart = itemView.findViewById(R.id.candleStickChart)
    val lineChart: LineChart = itemView.findViewById(R.id.lineChart)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomViewHolder {
    val itemView = inflater.inflate(R.layout.stockroomchart_item, parent, false)
    return StockRoomViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: StockRoomViewHolder,
    position: Int
  ) {
    val current = getItem(position)
    if (current != null) {
      holder.bindGroup(current, clickListenerGroup)
      holder.bindSummary(current, clickListenerSummary)

      if (stockViewMode == StockViewMode.Candle) {
        holder.candleStickChart.visibility = View.VISIBLE
        holder.lineChart.visibility = View.GONE

        setupCandleStickChart(holder.candleStickChart)

        val stockDataEntries: List<StockDataEntry>? = chartDataItems[current.onlineMarketData.symbol]
        loadCandleStickChart(
            holder.candleStickChart,
            current.onlineMarketData.symbol,
            stockDataEntries,
            stockViewRange
        )
      } else {
        holder.candleStickChart.visibility = View.GONE
        holder.lineChart.visibility = View.VISIBLE

        setupLineChart(holder.lineChart)

        val stockDataEntries: List<StockDataEntry>? = chartDataItems[current.onlineMarketData.symbol]
        loadLineChart(
            holder.lineChart,
            current.onlineMarketData.symbol,
            stockDataEntries,
            stockViewRange
        )
      }

      holder.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      holder.itemViewSymbol.text = current.onlineMarketData.symbol
      holder.itemViewName.text = current.onlineMarketData.name

      if (current.onlineMarketData.marketPrice > 0.0) {
        val marketPrice = if (current.onlineMarketData.marketPrice > 5.0) {
          DecimalFormat("0.00").format(current.onlineMarketData.marketPrice)
        } else {
          DecimalFormat("0.00##").format(current.onlineMarketData.marketPrice)
        }
        val change = DecimalFormat("0.00##").format(current.onlineMarketData.marketChange)
        val changePercent = "(${
          DecimalFormat("0.00").format(
              current.onlineMarketData.marketChangePercent
          )
        }%)"

        if (current.onlineMarketData.postMarketData) {
          holder.itemViewMarketPrice.text = SpannableStringBuilder()
              .italic { append(marketPrice) }

          holder.itemViewChange.text = SpannableStringBuilder()
              .italic { append(change) }

          holder.itemViewChangePercent.text = SpannableStringBuilder()
              .italic { append(changePercent) }
        } else {
          holder.itemViewMarketPrice.text = marketPrice
          holder.itemViewChange.text = change
          holder.itemViewChangePercent.text = changePercent
        }
      } else {
        holder.itemViewMarketPrice.text = ""
        holder.itemViewChange.text = ""
        holder.itemViewChangePercent.text = ""
      }

      val shares = current.assets.sumByDouble {
        it.shares
      }

      var asset: Double = 0.0
      var capital: Double = 0.0

      if (shares > 0.0) {
        asset = current.assets.sumByDouble {
          it.shares * it.price
        }

        if (current.onlineMarketData.marketPrice > 0.0) {
          capital = current.assets.sumByDouble {
            it.shares * current.onlineMarketData.marketPrice
          }
        }
      }

      when {
        capital > 0.0 && capital > asset -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.green))
        }
        capital > 0.0 && capital < asset -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.red))
        }
        else -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.backgroundListColor))
        }
      }

      var color = current.stockDBdata.groupColor
      if (color == 0) {
        color = context.getColor(R.color.backgroundListColor)
      }
      setBackgroundColor(holder.itemTextViewGroup, color)

      /*
      // Keep the corner radii and only change the background color.
      val gradientDrawable = holder.itemLinearLayoutGroup.background as GradientDrawable
      gradientDrawable.setColor(color)
      holder.itemLinearLayoutGroup.background = gradientDrawable
      */
    }
  }

  private fun setupCandleStickChart(candleStickChart: CandleStickChart) {
    candleStickChart.isDoubleTapToZoomEnabled = false
    candleStickChart.setTouchEnabled(false)

    candleStickChart.xAxis.setDrawLabels(false)
    candleStickChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    candleStickChart.xAxis.setDrawAxisLine(true)
    candleStickChart.xAxis.setDrawGridLines(false)

    candleStickChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    candleStickChart.axisRight.setDrawAxisLine(true)
    candleStickChart.axisRight.setDrawGridLines(true)
    candleStickChart.axisRight.isEnabled = true

    candleStickChart.axisLeft.setDrawGridLines(false)
    candleStickChart.axisLeft.setDrawAxisLine(false)
    candleStickChart.axisLeft.isEnabled = false

    candleStickChart.legend.isEnabled = false
    candleStickChart.description = null
    candleStickChart.setNoDataText("")
  }

  private fun loadCandleStickChart(
    candleStickChart: CandleStickChart,
    symbol: String,
    stockDataEntries: List<StockDataEntry>?,
    stockViewRange: StockViewRange
  ) {
    if (stockDataEntries == null || stockDataEntries.isEmpty()) {
      candleStickChart.invalidate()
      return
    }

    candleStickChart.candleData?.clearValues()

    val candleEntries: MutableList<CandleEntry> = mutableListOf()
    stockDataEntries.forEach { stockDataEntry ->
      candleEntries.add(stockDataEntry.candleEntry)
    }
    val series = CandleDataSet(candleEntries, symbol)
    series.color = Color.rgb(0, 0, 255)
    series.shadowColor = Color.rgb(255, 255, 0)
    series.shadowWidth = 1f
    series.decreasingColor = Color.rgb(255, 0, 0)
    series.decreasingPaintStyle = Paint.Style.FILL
    series.increasingColor = Color.rgb(0, 255, 0)
    series.increasingPaintStyle = Paint.Style.FILL
    series.neutralColor = Color.LTGRAY
    series.setDrawValues(false)

    val candleData = CandleData(series)
    candleStickChart.data = candleData

    val digits = if (candleData.yMax < 1.0) {
      4
    } else {
      2
    }
    candleStickChart.axisRight.valueFormatter = DefaultValueFormatter(digits)

    candleStickChart.invalidate()
  }

  private fun setupLineChart(lineChart: LineChart) {
    lineChart.isDoubleTapToZoomEnabled = false
    lineChart.setTouchEnabled(false)

    lineChart.xAxis.setDrawLabels(false)
    lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    lineChart.xAxis.setDrawGridLines(false)
    lineChart.xAxis.setDrawAxisLine(true)

    lineChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    lineChart.axisRight.setDrawAxisLine(true)
    lineChart.axisRight.setDrawGridLines(true)
    lineChart.axisRight.isEnabled = true

    lineChart.axisLeft.setDrawGridLines(false)
    lineChart.axisLeft.setDrawAxisLine(false)
    lineChart.axisLeft.isEnabled = false

    lineChart.legend.isEnabled = false
    lineChart.description = null
    lineChart.setNoDataText("")
  }

  private fun loadLineChart(
    lineChart: LineChart,
    symbol: String,
    stockDataEntries: List<StockDataEntry>?,
    stockViewRange: StockViewRange
  ) {
    if (stockDataEntries == null || stockDataEntries!!.isEmpty()) {
      lineChart.invalidate()
      return
    }

    lineChart.lineData?.clearValues()

    val dataPoints = ArrayList<DataPoint>()
    stockDataEntries.forEach { stockDataEntry ->
      dataPoints.add(DataPoint(stockDataEntry.candleEntry.x, stockDataEntry.candleEntry.y))
    }

    val series = LineDataSet(dataPoints as List<Entry>?, symbol)

    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    series.setDrawFilled(true)
    series.setDrawCircles(false)

    val lineData = LineData(series)
    lineChart.data = lineData

    val digits = if (lineData.yMax < 1.0) {
      4
    } else {
      2
    }
    lineChart.axisRight.valueFormatter = DefaultValueFormatter(digits)

    lineChart.invalidate()
  }

  internal fun setStockItems(stockItemSet: StockItemSet) {
    // Using allDataReady the list is updated only if all data sources are ready
    // which can take a few seconds because of the slow online data.
    // Without this check, the list is filled instantly, but might be reshuffled
    // for sorting when the online data is ready.

    //if (stockItemSet.allDataReady) {
    submitList(stockItemSet.stockItems)
    notifyDataSetChanged()
    //}
  }

  internal fun setChartItem(
    stockChartData: StockChartData,
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    chartDataItems[stockChartData.symbol] = stockChartData.stockDataEntries

    this.stockViewRange = stockViewRange
    this.stockViewMode = stockViewMode

    notifyDataSetChanged()
  }
}
