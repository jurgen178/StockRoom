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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.TreeDataEntry
import com.anychart.enums.TreeFillingMethod.AS_TABLE
import com.thecloudsite.stockroom.utils.getAssets
import kotlinx.android.synthetic.main.fragment_treemap.view.treemap_view
import okhttp3.internal.toHexString

// https://github.com/AnyChart/AnyChart-Android/blob/master/sample/src/main/java/com/anychart/sample/charts/TreeMapChartActivity.java

private class CustomTreeDataEntry(
  id: String?,
  parent: String?,
  product: String?,
  value: Int?,
  color: String?
) : TreeDataEntry(id, parent, value) {
  init {
    setValue("product", product)
    setValue("fill", color)
  }
}

class StockRoomTreemapFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = StockRoomTreemapFragment()
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
    return inflater.inflate(R.layout.fragment_treemap, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    //val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    //val adapter = StockRoomTreemapListAdapter(requireContext(), clickListenerSummary)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItemSet ->
        updateTreemap(view, stockItemSet.stockItems)
      }
    })

    // Rotating device keeps sending alerts.
    // State changes of the lifecycle trigger the notification.
    stockRoomViewModel.resetAlerts()
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        stockRoomViewModel.runOnlineTaskNow("Request to get online data manually.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getColorStr(color: Int): String {
    val hexStr = "0x${color.toHexString()}"
    return hexStr.replace("0xff", "#")
  }

  private fun updateTreemap(
    view: View,
    stockItems: List<StockItem>
  ) {
    val anyChartView = view.treemap_view
    val treeMap = AnyChart.treeMap()
    val data: MutableList<DataEntry> = ArrayList()

    //val listColors = ArrayList<Int>()

    data class AssetSummary(
      val symbol: String,
      val assets: Double,
      val color: Int
    )

    val assetList: MutableList<AssetSummary> = mutableListOf()
    var totalAssets = 0.0
    stockItems.forEach { stockItem ->
      val (totalQuantity, totalPrice) = getAssets(stockItem.assets)

      val assets = totalQuantity * stockItem.onlineMarketData.marketPrice
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

    data.add(CustomTreeDataEntry("Stock", null, "", 0, ""))

    if (totalAssets > 0.0) {
      val sortedAssetList = assetList.filter { assetSummary ->
        assetSummary.assets > 0.0
      }
          .sortedByDescending { assetSummary ->
            assetSummary.assets
          }

      sortedAssetList //.take(n)
          .forEach { assetItem ->
            data.add(
                CustomTreeDataEntry(
                    assetItem.symbol,
                    "Stock",
                    assetItem.symbol,
                    assetItem.assets.toInt(),
                    getColorStr(assetItem.color)
                )
            )
            //listColors.add(getColorStr(assetItem.color))
          }

//      // Add the sum of the remaining values.
//      if (sortedAssetList.size == n + 1) {
//        val assetItem = sortedAssetList.last()
//
//        data.add(
//            CustomTreeDataEntry(
//                assetItem.symbol, "Stock", assetItem.symbol, assetItem.assets.toInt()
//            )
//        )
//        //listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
//        listColors.add(Color.GRAY)
//      } else
//        if (sortedAssetList.size > n + 1) {
//          val otherAssetList = sortedAssetList.drop(n)
//          val otherAssets = otherAssetList.sumByDouble { assetItem ->
//            assetItem.assets
//          }
//
//          val symbol = "[${otherAssetList.first().symbol}-${otherAssetList.last().symbol}]"
//          data.add(CustomTreeDataEntry(symbol, "Stock", symbol, otherAssets.toInt()))
////          listPie.add(
////              PieEntry(
////                  otherAssets.toFloat(),
////                  "[${otherAssetList.first().symbol}-${otherAssetList.last().symbol}]"
////              )
////          )
//          listColors.add(Color.GRAY)
//        }
    }

    treeMap.data(data, AS_TABLE)

//    treeMap.colorScale()
//        .colors(listColors)

    anyChartView.setChart(treeMap)

//    val pieDataSet = PieDataSet(listPie, "")
//    pieDataSet.colors = listColors
//    pieDataSet.valueTextSize = 10f
//    // pieDataSet.valueFormatter = DefaultValueFormatter(2)
//    pieDataSet.valueFormatter = object : ValueFormatter() {
//      override fun getFormattedValue(value: Float) =
//        DecimalFormat("0.00").format(value)
//    }
//
//    // Line start
//    pieDataSet.valueLinePart1OffsetPercentage = 80f
//    // Radial length
//    pieDataSet.valueLinePart1Length = 0.4f
//    // Horizontal length
//    pieDataSet.valueLinePart2Length = .2f
//    pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
//
//    val pieData = PieData(pieDataSet)
//    view.summaryPieChart.data = pieData
//
//    //view.summaryPieChart.setUsePercentValues(true)
//    view.summaryPieChart.isDrawHoleEnabled = true
//
//    val centerText = SpannableStringBuilder()
//        .append("${context?.getString(R.string.summary_total_assets)} ")
//        .underline { bold { append(DecimalFormat("0.00").format(totalAssets)) } }
//    view.summaryPieChart.centerText = centerText
//
//    view.summaryPieChart.description.isEnabled = false
//    view.summaryPieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
//    view.summaryPieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
//
//    view.summaryPieChart.setExtraOffsets(0f, 3f, 26f, 4f)
//
//    //val legendList: MutableList<LegendEntry> = mutableListOf()
//    //legendList.add(LegendEntry("test", SQUARE, 10f, 100f, null, Color.RED))
//    //view.summaryPieChart.legend.setCustom(legendList)
//
//    view.summaryPieChart.invalidate()
  }
}
