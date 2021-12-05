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
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.databinding.StockroomChartItemBinding
import com.thecloudsite.stockroom.utils.*

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class StockRoomChartAdapter internal constructor(
    val context: Context,
    private val clickListenerGroupLambda: (StockItem, View) -> Unit,
    private val clickListenerMarkerLambda: (StockItem, View) -> Unit,
    private val clickListenerSymbolLambda: (StockItem) -> Unit,
) : ListAdapter<StockItem, StockRoomChartAdapter.StockRoomViewHolder>(StockRoomDiffCallback()) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var defaultTextColor: Int? = null

    private var stockViewRange: StockViewRange = StockViewRange.OneDay
    private var stockViewMode: StockViewMode = StockViewMode.Line
    private var chartDataItems: HashMap<String, List<StockDataEntry>?> = hashMapOf()

    // Comma separated symbols.
    private var chartOverlaySymbols: String = ""
    private var useChartOverlaySymbols: Boolean = false

    class StockRoomViewHolder(
        val binding: StockroomChartItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindGroupOnClickListener(
            stockItem: StockItem,
            clickListener: (StockItem, View) -> Unit
        ) {
            binding.itemviewGroup.setOnClickListener { clickListener(stockItem, itemView) }
        }

        fun bindMarkerOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem, View) -> Unit
        ) {
            binding.itemviewGroupMarker.setOnClickListener {
                clickListenerLambda(
                    stockItem,
                    itemView
                )
            }
        }

        fun bindSummaryOnClickListener(
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
    ): StockRoomViewHolder {

        val binding = StockroomChartItemBinding.inflate(inflater, parent, false)
        return StockRoomViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StockRoomViewHolder,
        position: Int
    ) {
        val current = getItem(position)
        if (current != null) {
            if (defaultTextColor == null) {
                defaultTextColor = holder.binding.textViewMarketPrice.currentTextColor
            }

            holder.bindGroupOnClickListener(current, clickListenerGroupLambda)
            holder.bindMarkerOnClickListener(current, clickListenerMarkerLambda)
            holder.bindSummaryOnClickListener(current, clickListenerSymbolLambda)

            val stockDataEntries: List<StockDataEntry>? =
                chartDataItems[current.onlineMarketData.symbol]

            if (stockDataEntries != null
                && stockDataEntries.isNotEmpty()
            ) {
                if (stockViewMode == StockViewMode.Candle) {
                    holder.binding.candleStickChart.visibility = View.VISIBLE
                    holder.binding.lineChart.visibility = View.GONE

                    setupCandleStickChart(holder.binding.candleStickChart)
                    loadCandleStickChart(
                        holder.binding.candleStickChart,
                        chartDataItems,
                        current.onlineMarketData.symbol,
                        stockDataEntries
                    )
                } else {
                    holder.binding.candleStickChart.visibility = View.GONE
                    holder.binding.lineChart.visibility = View.VISIBLE

                    setupLineChart(holder.binding.lineChart)
                    loadLineChart(
                        holder.binding.lineChart,
                        chartDataItems,
                        current.onlineMarketData.symbol,
                        stockDataEntries
                    )
                }
            } else {
                holder.binding.candleStickChart.visibility = View.GONE
                holder.binding.lineChart.visibility = View.GONE
            }

            holder.binding.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))

            val displayName =
                if (current.stockDBdata.name.isEmpty()) current.stockDBdata.symbol else current.stockDBdata.name
            holder.binding.textViewSymbol.text = displayName

            holder.binding.imageViewSymbol.visibility = View.GONE
            // val imgUrl = "https://s.yimg.com/uc/fin/img/reports-thumbnails/1.png"
            val imgUrl = current.onlineMarketData.coinImageUrl
            if (imgUrl.isNotEmpty()) {
                val imgView: ImageView = holder.binding.imageViewSymbol
                val imgUri = imgUrl.toUri()
                // use imgUrl as it is, no need to build upon the https scheme (https://...)
                //.buildUpon()
                //.scheme("https")
                //.build()

                Glide.with(imgView.context)
                    .load(imgUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.binding.imageViewSymbol.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(imgView)
            }

            holder.binding.textViewName.text = getName(current.onlineMarketData)

            if (current.onlineMarketData.marketPrice > 0.0) {
                val marketValues = getMarketValues(current.onlineMarketData)

                if (current.onlineMarketData.postMarketData) {
                    holder.binding.textViewMarketPrice.text = SpannableStringBuilder()
                        .italic { append(marketValues.first) }

                    holder.binding.textViewChange.text = SpannableStringBuilder()
                        .italic { append(marketValues.second) }

                    holder.binding.textViewChangePercent.text = SpannableStringBuilder()
                        .italic { append(marketValues.third) }
                } else {
                    holder.binding.textViewMarketPrice.text = marketValues.first
                    holder.binding.textViewChange.text = marketValues.second
                    holder.binding.textViewChangePercent.text = marketValues.third
                }
            } else {
                holder.binding.textViewMarketPrice.text = ""
                holder.binding.textViewChange.text = ""
                holder.binding.textViewChangePercent.text = ""
            }

            // set background to market change
            holder.binding.itemRedGreen.setBackgroundColor(
                getChangeColor(
                    current.onlineMarketData.marketChange,
                    current.onlineMarketData.postMarketData,
                    context.getColor(color.backgroundListColor),
                    context
                )
            )

            if (useWhiteOnRedGreen && current.onlineMarketData.marketChange != 0.0) {
                holder.binding.textViewMarketPrice.setTextColor(Color.WHITE)
                holder.binding.textViewChange.setTextColor(Color.WHITE)
                holder.binding.textViewChangePercent.setTextColor(Color.WHITE)
            } else {
                holder.binding.textViewMarketPrice.setTextColor(defaultTextColor!!)
                holder.binding.textViewChange.setTextColor(defaultTextColor!!)
                holder.binding.textViewChangePercent.setTextColor(defaultTextColor!!)
            }

            var color = current.stockDBdata.groupColor
            if (color == 0) {
                color = context.getColor(R.color.backgroundListColor)
            }
            setGroupBackground(
                context,
                current.stockDBdata.marker,
                color,
                holder.binding.itemviewGroup,
                holder.binding.itemviewGroupSep,
                holder.binding.itemviewGroupMarker
            )
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
        candleStickChart.axisRight.textColor = context.getColor(R.color.black)
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
        chartDataItems: HashMap<String, List<StockDataEntry>?>,
        symbol: String,
        stockDataEntries: List<StockDataEntry>?
    ) {
        candleStickChart.candleData?.clearValues()

        if (stockDataEntries == null || stockDataEntries.isEmpty()) {
            candleStickChart.invalidate()
            return
        }

        val seriesList: MutableList<ICandleDataSet> = mutableListOf()

        val candleEntries: MutableList<CandleEntry> = mutableListOf()
        var minY = Float.MAX_VALUE
        var maxY = 0f
        stockDataEntries.forEach { stockDataEntry ->
            minY = minOf(minY, stockDataEntry.candleEntry.y)
            maxY = maxOf(maxY, stockDataEntry.candleEntry.y)
            candleEntries.add(stockDataEntry.candleEntry)
        }

        // Chart data is constant, add a zero point for correct scaling of the control.
        if (minY == maxY) {
            candleEntries.add(CandleEntry(0f, 0f, 0f, 0f, 0f))
        }

        val series: CandleDataSet = CandleDataSet(candleEntries, symbol)
        series.color = Color.rgb(0, 0, 255)
        series.shadowColor = Color.rgb(255, 255, 0)
        series.shadowWidth = 1f
        series.decreasingColor = Color.rgb(255, 0, 0)
        series.decreasingPaintStyle = Paint.Style.FILL
        series.increasingColor = Color.rgb(0, 255, 0)
        series.increasingPaintStyle = Paint.Style.FILL
        series.neutralColor = Color.LTGRAY
        series.setDrawValues(false)

        // Get the ref chart data.
        if (useChartOverlaySymbols && chartOverlaySymbols.isNotEmpty()) {
            var chartOverlayColorIndex = 0
            chartOverlaySymbols.split(",").take(MaxChartOverlays).forEach { symbolRef ->
                val stockDataEntriesRef = chartDataItems[symbolRef]
                if (stockDataEntriesRef != null && stockDataEntriesRef.size > 1) {

                    var minRefY = Float.MAX_VALUE
                    var maxRefY = 0f

                    val refList = ArrayList<StockDataEntry>()

                    // Reference charts might not cover the same time line.
                    // Map the time points from the stockDataEntries to the stockDataEntriesRef points.
                    var indexRef = 0
                    var entriesRef1 = stockDataEntriesRef[indexRef]
                    var entriesRef2 = stockDataEntriesRef[indexRef + 1]

                    // Align the date points on stockDataEntries.
                    stockDataEntries.forEach { stockDataEntry ->

                        // Match stockDataEntriesRef to the stockDataEntry point.
                        while (stockDataEntry.dateTimePoint >= entriesRef2.dateTimePoint
                        ) {
                            // Check the next point to match.
                            if (indexRef < stockDataEntriesRef.size - 1) {
                                indexRef++
                                entriesRef1 = stockDataEntriesRef[indexRef]
                                if (indexRef < stockDataEntriesRef.size - 2) {
                                    entriesRef2 = stockDataEntriesRef[indexRef + 1]
                                } else {
                                    break
                                }
                            } else {
                                break
                            }
                        }

                        minRefY = minOf(minRefY, entriesRef1.candleEntry.y)
                        maxRefY = maxOf(maxRefY, entriesRef1.candleEntry.y)

                        // clone entry and update the x value to match the original stock chart
                        val refEntry = StockDataEntry(
                            dateTimePoint = entriesRef1.dateTimePoint,
                            x = stockDataEntry.candleEntry.x.toDouble(),
                            high = entriesRef1.candleEntry.high.toDouble(),
                            low = entriesRef1.candleEntry.low.toDouble(),
                            open = entriesRef1.candleEntry.open.toDouble(),
                            close = entriesRef1.candleEntry.close.toDouble(),
                        )
                        refList.add(refEntry)
                    }

                    // Include the right side value of the last entry.
                    minRefY = minOf(minRefY, entriesRef2.candleEntry.y)
                    maxRefY = maxOf(maxRefY, entriesRef2.candleEntry.y)

                    // Scale ref data to stock data so that the ref stock data will always look the same in each stock chart.
                    if (refList.isNotEmpty() && maxRefY > minRefY && maxRefY > 0f && maxY > minY && maxY > 0f) {
                        val scale = (maxY - minY) / (maxRefY - minRefY)

                        val candleEntriesRef = refList.map { stockDataEntry ->
                            CandleEntry(
                                stockDataEntry.candleEntry.x,
                                (stockDataEntry.candleEntry.high - minRefY) * scale + minY,
                                (stockDataEntry.candleEntry.low - minRefY) * scale + minY,
                                (stockDataEntry.candleEntry.open - minRefY) * scale + minY,
                                (stockDataEntry.candleEntry.close - minRefY) * scale + minY
                            )
                        }

                        val seriesRef: CandleDataSet = CandleDataSet(candleEntriesRef, symbolRef)
                        val color =
                            chartOverlayColors[chartOverlayColorIndex++ % chartOverlayColors.size]

                        seriesRef.color = color
                        seriesRef.shadowColor = color
                        seriesRef.shadowWidth = 1f
                        seriesRef.decreasingColor = Color.rgb(255, 204, 204)
                        seriesRef.decreasingPaintStyle = Paint.Style.FILL
                        seriesRef.increasingColor = Color.rgb(204, 255, 204)
                        seriesRef.increasingPaintStyle = Paint.Style.FILL
                        seriesRef.neutralColor = color
                        seriesRef.setDrawValues(false)

                        seriesList.add(seriesRef)
                    }
                }
            }
        }

        seriesList.add(series)

        // https://github.com/PhilJay/MPAndroidChart/wiki/Setting-Data
        val candleData = CandleData(seriesList)
        candleStickChart.data = candleData

//        val digits = if (candleData.yMax < 1.0) {
//            4
//        } else {
//            2
//        }
//        candleStickChart.axisRight.valueFormatter = DefaultValueFormatter(digits)
        candleStickChart.axisRight.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) =
                to2To8Digits(value)
        }

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
        lineChart.axisRight.textColor = context.getColor(R.color.black)
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
        chartDataItems: HashMap<String, List<StockDataEntry>?>,
        symbol: String,
        stockDataEntries: List<StockDataEntry>?
    ) {
        lineChart.lineData?.clearValues()

        if (stockDataEntries == null || stockDataEntries.isEmpty()) {
            lineChart.invalidate()
            return
        }

        val seriesList: MutableList<ILineDataSet> = mutableListOf()

        // Get the chart data.
        val dataPoints = ArrayList<DataPoint>()
        var minY = Float.MAX_VALUE
        var maxY = 0f
        stockDataEntries.forEach { stockDataEntry ->
            minY = minOf(minY, stockDataEntry.candleEntry.y)
            maxY = maxOf(maxY, stockDataEntry.candleEntry.y)
            dataPoints.add(
                DataPoint(
                    stockDataEntry.candleEntry.x,
                    stockDataEntry.candleEntry.y
                )
            )
        }

        // Chart data is constant, add a zero point for correct scaling of the control.
        if (minY == maxY) {
            dataPoints.add(DataPoint(0f, 0f))
        }

        val series = LineDataSet(dataPoints as List<DataPoint>, symbol)
        series.setDrawHorizontalHighlightIndicator(false)
        series.setDrawValues(false)
        series.setDrawFilled(true)
        series.setDrawCircles(false)
        series.color = context.getColor(R.color.chartLine)
        series.fillColor = context.getColor(R.color.chartLine)

        // Get the ref chart data.
        if (useChartOverlaySymbols && chartOverlaySymbols.isNotEmpty()) {
            var chartOverlayColorIndex = 0
            chartOverlaySymbols.split(",").take(MaxChartOverlays).forEach { symbolRef ->
                val stockDataEntriesRef = chartDataItems[symbolRef]
                if (stockDataEntriesRef != null && stockDataEntriesRef.size > 1) {

                    var minRefY = Float.MAX_VALUE
                    var maxRefY = 0f

                    val refList = ArrayList<StockDataEntry>()

                    // Reference charts might not cover the same time line.
                    // Map the time points from the stockDataEntries to the stockDataEntriesRef points.
                    var indexRef = 0
                    var entriesRef1 = stockDataEntriesRef[indexRef]
                    var entriesRef2 = stockDataEntriesRef[indexRef + 1]

                    // Align the date points on stockDataEntries.
                    stockDataEntries.forEach { stockDataEntry ->

                        // Match stockDataEntriesRef to the stockDataEntry point.
                        while (stockDataEntry.dateTimePoint >= entriesRef2.dateTimePoint
                        ) {
                            // Check the next point to match.
                            if (indexRef < stockDataEntriesRef.size - 1) {
                                indexRef++
                                entriesRef1 = stockDataEntriesRef[indexRef]
                                if (indexRef < stockDataEntriesRef.size - 2) {
                                    entriesRef2 = stockDataEntriesRef[indexRef + 1]
                                } else {
                                    break
                                }
                            } else {
                                break
                            }
                        }

                        minRefY = minOf(minRefY, entriesRef1.candleEntry.y)
                        maxRefY = maxOf(maxRefY, entriesRef1.candleEntry.y)

                        // clone entry and update the x value to match the original stock chart
                        val refEntry = StockDataEntry(
                            dateTimePoint = entriesRef1.dateTimePoint,
                            x = stockDataEntry.candleEntry.x.toDouble(),
                            high = entriesRef1.candleEntry.high.toDouble(),
                            low = entriesRef1.candleEntry.low.toDouble(),
                            open = entriesRef1.candleEntry.open.toDouble(),
                            close = entriesRef1.candleEntry.close.toDouble(),
                        )
                        refList.add(refEntry)
                    }

                    // Include the right side value of the last entry.
                    minRefY = minOf(minRefY, entriesRef2.candleEntry.y)
                    maxRefY = maxOf(maxRefY, entriesRef2.candleEntry.y)

                    // Scale ref data to stock data so that the ref stock data will always look the same in each stock chart.
                    if (refList.isNotEmpty() && maxRefY > minRefY && maxRefY > 0f && maxY > minY && maxY > 0f) {
                        val scale = (maxY - minY) / (maxRefY - minRefY)

                        val dataPointsRef = refList.map { stockDataEntry ->
                            DataPoint(
                                x = stockDataEntry.candleEntry.x,
                                y = (stockDataEntry.candleEntry.y - minRefY) // shift down ref data
                                        * scale                              // scale ref to match stock data range
                                        + minY                               // shift up to min stock data
                            )
                        }

                        val seriesRef = LineDataSet(dataPointsRef as List<DataPoint>, symbolRef)
                        val color =
                            chartOverlayColors[chartOverlayColorIndex++ % chartOverlayColors.size]

                        seriesRef.setDrawHorizontalHighlightIndicator(false)
                        seriesRef.setDrawValues(false)
                        seriesRef.setDrawCircles(false)
                        seriesRef.color = color

                        // No filling for overlay graphs.
                        seriesRef.setDrawFilled(false)
                        //seriesRef.fillColor = color

                        seriesList.add(seriesRef)
                    }
                }
            }
        }

        seriesList.add(series)

        val lineData = LineData(seriesList)
        lineChart.data = lineData

//        val digits = if (lineData.yMax < 1.0) {
//            4
//        } else {
//            2
//        }
//        lineChart.axisRight.valueFormatter = DefaultValueFormatter(digits)
        lineChart.axisRight.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) =
                to2To8Digits(value)
        }

        lineChart.invalidate()
    }

    internal fun setStockItems(stockItems: List<StockItem>) {
        submitList(stockItems)
        notifyDataSetChanged()
    }

    internal fun updateChartItem(
        stockChartData: StockChartData,
        useChartOverlaySymbols: Boolean,
        chartOverlaySymbols: String,
        stockViewRange: StockViewRange,
        stockViewMode: StockViewMode
    ) {
        chartDataItems[stockChartData.symbol] = stockChartData.stockDataEntries

        this.useChartOverlaySymbols = useChartOverlaySymbols
        this.chartOverlaySymbols = chartOverlaySymbols
        this.stockViewRange = stockViewRange
        this.stockViewMode = stockViewMode

        notifyDataSetChanged()
    }
}
