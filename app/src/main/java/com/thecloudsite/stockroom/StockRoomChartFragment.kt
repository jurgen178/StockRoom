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

import android.content.res.Configuration
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
import androidx.recyclerview.widget.RecyclerView

class StockRoomChartFragment : StockRoomBaseFragment() {

  private lateinit var stockChartDataViewModel: StockChartDataViewModel

  companion object {
    fun newInstance() = StockRoomChartFragment()
  }

  lateinit var onlineChartHandler: Handler
  var symbolList: MutableList<String> = mutableListOf()

  // Settings.
  private val settingStockViewRange = "SettingStockViewRange"
  private var stockViewRange: StockViewRange
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewRange.OneDay
      return StockViewRange.values()[sharedPref.getInt(
          settingStockViewRange, StockViewRange.OneDay.value
      )]
    }
    set(value) {
    }

  private val settingStockViewMode = "SettingStockViewMode"
  private var stockViewMode: StockViewMode
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewMode.Line
      return StockViewMode.values()[sharedPref.getInt(
          settingStockViewMode, StockViewMode.Line.value
      )]
    }
    set(value) {
    }

  private val settingChartRefStockDefault = "^GSPC"
  private val settingChartRefStock = "ChartRefStock"
  private var chartRefStock: String
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity)
            ?: return settingChartRefStockDefault
      return sharedPref.getString(settingChartRefStock, settingChartRefStockDefault)
          ?: return settingChartRefStockDefault
    }
    set(value) {
    }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val clickListenerGroup =
      { stockItem: StockItem, itemView: View -> clickListenerGroup(stockItem, itemView) }
    val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    val adapter = StockRoomChartAdapter(
        requireContext(),
        clickListenerGroup,
        clickListenerSummary
    )

    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

    recyclerView.adapter = adapter

    // Set column number depending on orientation.
    val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      1
    } else {
      2
    }

    recyclerView.layoutManager = GridLayoutManager(context, spanCount)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItemSet ->
        symbolList = mutableListOf()
        stockItemSet.stockItems.forEach { stockItem ->
          symbolList.add(stockItem.stockDBdata.symbol)
        }

        adapter.setStockItems(stockItemSet)
      }
    })

    stockChartDataViewModel = ViewModelProvider(this).get(StockChartDataViewModel::class.java)

    stockChartDataViewModel.chartData.observe(viewLifecycleOwner, Observer { stockChartData ->
      adapter.setChartItem(stockChartData, chartRefStock, stockViewRange, stockViewMode)
    })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

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
      stockChartDataViewModel.getChartData(chartRefStock, stockViewRange)
      symbolList.forEach { symbol ->
        stockChartDataViewModel.getChartData(symbol, stockViewRange)
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
}
