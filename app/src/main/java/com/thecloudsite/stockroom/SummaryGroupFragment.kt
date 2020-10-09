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

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.underline
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.thecloudsite.stockroom.invaders.InvadersActivity
import com.thecloudsite.stockroom.utils.getAssets
import kotlinx.android.synthetic.main.fragment_summarygroup.view.imageView
import kotlinx.android.synthetic.main.fragment_summarygroup.view.summaryPieChart
import java.text.DecimalFormat
import kotlin.math.roundToInt

class SummaryGroupFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var longPressedCounter = 0

  companion object {
    fun newInstance() = SummaryGroupFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_summarygroup, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    val summaryGroupAdapter = SummaryGroupAdapter(requireContext())
    val summaryGroup = view.findViewById<RecyclerView>(R.id.summaryGroup)
    summaryGroup.adapter = summaryGroupAdapter

    // Set column number depending on screen width.
    val scale = 494
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    summaryGroup.layoutManager = GridLayoutManager(requireContext(),
        Integer.min(Integer.max(spanCount, 1), 10)
    )

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItemSet ->
        summaryGroupAdapter.updateData(stockItemSet.stockItems)
        updatePieData(view, stockItemSet.stockItems)
      }
    })

    stockRoomViewModel.allGroupTable.observe(viewLifecycleOwner, Observer { groups ->
      summaryGroupAdapter.addGroups(groups)
    })

    longPressedCounter = 0

    view.summaryPieChart.onChartGestureListener = object : OnChartGestureListener {
      override fun onChartGestureStart(
        me: MotionEvent?,
        lastPerformedGesture: ChartGesture?
      ) {
      }

      override fun onChartGestureEnd(
        me: MotionEvent?,
        lastPerformedGesture: ChartGesture?
      ) {
      }

      override fun onChartLongPressed(me: MotionEvent?) {
        longPressedCounter++

        if (longPressedCounter == 2) {
          view.summaryPieChart.visibility = View.GONE
          view.imageView.visibility = View.VISIBLE
        }
      }

      override fun onChartDoubleTapped(me: MotionEvent?) {
      }

      override fun onChartSingleTapped(me: MotionEvent?) {
      }

      override fun onChartFling(
        me1: MotionEvent?,
        me2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
      ) {
      }

      override fun onChartScale(
        me: MotionEvent?,
        scaleX: Float,
        scaleY: Float
      ) {
      }

      override fun onChartTranslate(
        me: MotionEvent?,
        dX: Float,
        dY: Float
      ) {
      }
    }

    view.imageView.setOnClickListener {
      AlertDialog.Builder(context)
          // https://convertcodes.com/unicode-converter-encode-decode-utf/
          .setTitle(
              "\u0054\u0068\u0065\u0020\u0041\u006c\u0069\u0065\u006e\u0073\u0020\u0061\u0072\u0065\u0020\u0063\u006f\u006d\u0069\u006e\u0067"
          )
          .setMessage(
              "\u0047\u0065\u0074\u0020\u0074\u0068\u0065\u006d"
          )
          .setNegativeButton("\u004c\u0061\u0074\u0065\u0072") { dialog, _ ->
            dialog.dismiss()
            view.summaryPieChart.visibility = View.VISIBLE
            view.imageView.visibility = View.GONE
          }
          .setPositiveButton("\u004e\u006f\u0077") { dialog, _ ->
            val intent = Intent(activity, InvadersActivity::class.java)
            activity?.startActivity(intent)
            dialog.dismiss()
            view.summaryPieChart.visibility = View.VISIBLE
            view.imageView.visibility = View.GONE
          }
          .setOnCancelListener {
            view.summaryPieChart.visibility = View.VISIBLE
            view.imageView.visibility = View.GONE
          }
          .show()

      longPressedCounter = 0
    }
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
    longPressedCounter = 0
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        stockRoomViewModel.runOnlineTaskNow("Schedule to get online data manually.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updatePieData(
    view: View,
    stockItems: List<StockItem>
  ) {
    val listPie = ArrayList<PieEntry>()
    val listColors = ArrayList<Int>()

    data class AssetSummary(
      val symbol: String,
      val assets: Double,
      val color: Int
    )

    val assetList: MutableList<AssetSummary> = mutableListOf()
    var totalAssets = 0.0
    stockItems.forEach { stockItem ->
      val (totalShares, totalPric) = getAssets(stockItem.assets)

//      val totalShares: Double = stockItem.assets.sumByDouble { asset ->
//        asset.shares
//      }
      val assets = totalShares * stockItem.onlineMarketData.marketPrice
      totalAssets += assets
      val color = if (stockItem.stockDBdata.groupColor != 0) {
        stockItem.stockDBdata.groupColor
      } else {
        context?.getColor(R.color.backgroundListColor)
      }

      assetList.add(
          AssetSummary(stockItem.stockDBdata.symbol, assets, color!!)
      )
    }

    if (totalAssets > 0.0) {
      val sortedAssetList = assetList.sortedByDescending { item -> item.assets }

      // Display first 10 values from asset high to low.
      val n = 10
      sortedAssetList.take(n)
          .forEach { assetItem ->
            listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
            //listPie.add(PieEntry(assetItem.assets.toFloat(), "${assetItem.symbol} ${DecimalFormat("0.00").format(assetItem.assets)}"))
            listColors.add(assetItem.color)
          }

      // Add the sum of the remaining values.
      if (sortedAssetList.size == n + 1) {
        val assetItem = sortedAssetList.last()

        listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
        listColors.add(Color.GRAY)
      } else
        if (sortedAssetList.size > n + 1) {
          val otherAssetList = sortedAssetList.drop(n)
          val otherAssets = otherAssetList.sumByDouble { assetItem ->
            assetItem.assets
          }

          listPie.add(
              PieEntry(
                  otherAssets.toFloat(),
                  "[${otherAssetList.first().symbol}-${otherAssetList.last().symbol}]"
              )
          )
          listColors.add(Color.GRAY)
        }
    }

    val pieDataSet = PieDataSet(listPie, "")
    pieDataSet.colors = listColors
    pieDataSet.valueTextSize = 10f
    // pieDataSet.valueFormatter = DefaultValueFormatter(2)
    pieDataSet.valueFormatter = object : ValueFormatter() {
      override fun getFormattedValue(value: Float) =
        DecimalFormat("0.00").format(value)
    }

    // Line start
    pieDataSet.valueLinePart1OffsetPercentage = 80f
    // Radial length
    pieDataSet.valueLinePart1Length = 0.4f
    // Horizontal length
    pieDataSet.valueLinePart2Length = .2f
    pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

    val pieData = PieData(pieDataSet)
    view.summaryPieChart.data = pieData

    //view.summaryPieChart.setUsePercentValues(true)
    view.summaryPieChart.isDrawHoleEnabled = true

    val centerText = SpannableStringBuilder()
        .append("${context?.getString(R.string.summary_total_assets)} ")
        .underline { bold { append(DecimalFormat("0.00").format(totalAssets)) } }
    view.summaryPieChart.centerText = centerText

    view.summaryPieChart.description.isEnabled = false
    view.summaryPieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
    view.summaryPieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER

    view.summaryPieChart.setExtraOffsets(0f, 3f, 26f, 4f)

    //val legendList: MutableList<LegendEntry> = mutableListOf()
    //legendList.add(LegendEntry("test", SQUARE, 10f, 100f, null, Color.RED))
    //view.summaryPieChart.legend.setCustom(legendList)

    view.summaryPieChart.invalidate()
  }
}

