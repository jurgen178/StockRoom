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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.thecloudsite.stockroom.utils.MaxChartOverlays
import kotlin.math.roundToInt

class StockRoomChartFragment : StockRoomBaseFragment() {

  private lateinit var stockChartDataViewModel: StockChartDataViewModel

  companion object {
    fun newInstance() = StockRoomChartFragment()
  }

  lateinit var onlineChartHandler: Handler
  var symbolList: MutableList<String> = mutableListOf()
  private val symbolTypesMap = HashMap<String, StockType>()

  // Settings.
  private val settingStockViewRange = "SettingStockViewRange"
  private var stockViewRange: StockViewRange
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewRange.OneDay
      val index = sharedPref.getInt(settingStockViewRange, StockViewRange.OneDay.value)
      return if (index >= 0 && index < StockViewRange.values().size) {
        StockViewRange.values()[index]
      } else {
        StockViewRange.OneDay
      }
    }
    set(value) {
    }

  private val settingStockViewMode = "SettingStockViewMode"
  private var stockViewMode: StockViewMode
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewMode.Line
      val index = sharedPref.getInt(settingStockViewMode, StockViewMode.Line.value)
      return if (index >= 0 && index < StockViewMode.values().size) {
        StockViewMode.values()[index]
      } else {
        StockViewMode.Line
      }
    }
    set(value) {
    }

  private val settingOverlaySymbols = "chart_overlay_symbols"
  private var chartOverlaySymbols: String
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity)
          ?: return settingChartOverlaySymbolsDefault
      return sharedPref.getString(settingOverlaySymbols, settingChartOverlaySymbolsDefault)
        ?: return settingChartOverlaySymbolsDefault
    }
    set(value) {
    }

  private val settingUseChartOverlaySymbols = "use_chart_overlay_symbols"
  private var useChartOverlaySymbols: Boolean
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity)
          ?: return false
      return sharedPref.getBoolean(settingUseChartOverlaySymbols, false)
    }
    set(value) {
    }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val clickListenerGroupLambda =
      { stockItem: StockItem, itemView: View -> clickListenerGroup(stockItem, itemView) }
    val clickListenerSymbolLambda = { stockItem: StockItem -> clickListenerSymbol(stockItem) }
    val adapter = StockRoomChartAdapter(
      requireContext(),
      clickListenerGroupLambda,
      clickListenerSymbolLambda
    )

    val recyclerView = binding.recyclerview
    recyclerView.adapter = adapter

    // Set column number depending on screen width.
    val scale = 494
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    recyclerView.layoutManager = GridLayoutManager(
      context,
      Integer.min(Integer.max(spanCount, 1), 10)
    )

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItems ->
        val emptyList = symbolList.isEmpty()

        stockItems.forEach { stockItem ->

          val symbol = stockItem.stockDBdata.symbol
          if (!symbolList.contains(symbol)) {
            symbolList.add(symbol)

            // onlineChartTask runs first (emptyList==true) and no update needed
            if (!emptyList) {
              stockChartDataViewModel.getChartData(getStockSymbol(symbol), stockViewRange)
            }
          }
        }

        adapter.setStockItems(stockItems)
      }
    })

    stockChartDataViewModel = ViewModelProvider(this).get(StockChartDataViewModel::class.java)

    stockChartDataViewModel.chartData.observe(viewLifecycleOwner, Observer { stockChartData ->
      if (stockChartData != null) {
        val overlaySymbols =
          if (useChartOverlaySymbols) {
            chartOverlaySymbols
          } else {
            ""
          }

        adapter.updateChartItem(
          stockChartData,
          useChartOverlaySymbols,
          overlaySymbols,
          stockViewRange,
          stockViewMode
        )
      }
    })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    // Setup chart data update every 5min/24h.
    onlineChartHandler = Handler(Looper.getMainLooper())

    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onPause() {
    onlineChartHandler.removeCallbacks(onlineChartTask)
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    onlineChartHandler.post(onlineChartTask)
  }

  private val onlineChartTask = object : Runnable {
    override fun run() {
      // Refresh the charts.
      // getChartData triggers stockChartDataViewModel.chartData.observe
      if (useChartOverlaySymbols) {
        chartOverlaySymbols.split(",").take(MaxChartOverlays).forEach { symbolRef ->
          stockChartDataViewModel.getChartData(getStockSymbol(symbolRef), stockViewRange)
        }
      }

      symbolList.forEach { symbol ->
        stockChartDataViewModel.getChartData(getStockSymbol(symbol), stockViewRange)
      }

      val onlineChartTimerDelay: Long =
        if (stockViewRange == StockViewRange.OneDay
          || stockViewRange == StockViewRange.FiveDays
        ) {
          // Update daily and 5-day chart every 5min
          5 * 60 * 1000L
        } else {
          // Update other charts every day
          24 * 60 * 60 * 1000L
        }

      onlineChartHandler.postDelayed(this, onlineChartTimerDelay)
    }
  }

  private fun getStockSymbol(symbol: String): StockSymbol {
    if (!symbolTypesMap.containsKey(symbol)) {
      symbolTypesMap[symbol] = StockTypeFromInt(stockRoomViewModel.getTypeSync(symbol))
    }

    return StockSymbol(
      symbol = symbol,
      type = symbolTypesMap[symbol]!!
    )
  }
}
