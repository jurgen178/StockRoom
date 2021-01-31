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

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.scale
import androidx.core.text.underline
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.FragmentOverviewBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.epsilon
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import java.text.DecimalFormat
import kotlin.math.roundToInt

class StockRoomOverviewFragment : Fragment() {

  private var _binding: FragmentOverviewBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val separatorSymbol = "  ⋮"

  companion object {
    fun newInstance() = StockRoomOverviewFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    // Inflate the layout for this fragment
    _binding = FragmentOverviewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    val clickListenerSummary = { stockItem: StockItem -> clickListenerOverview(stockItem) }

    // Use the small list adapter for display.
    val stockRoomOverviewAdapter = StockRoomSmallListAdapter(requireContext(), clickListenerSummary)

    val recyclerView = binding.recyclerview
    recyclerView.adapter = stockRoomOverviewAdapter

    // Set column number depending on screen width.
    val scale = 494
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    recyclerView.layoutManager = GridLayoutManager(
        context,
        Integer.min(Integer.max(spanCount, 1), 10)
    )

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockitemList ->

        updateSummary(stockitemList)

        // sort the list by change %
        val sortedList = stockitemList.sortedByDescending { item ->
          item.onlineMarketData.marketChangePercent
        }

        // For more than 10 items show the top/bottom 5 only.
        val filteredList = if (sortedList.size > 10) {

          // top 5
          val list = mutableListOf<StockItem>()
          list.addAll(sortedList.take(5))

          // separator item
          list.add(
              StockItem(
                  onlineMarketData = OnlineMarketData(symbol = separatorSymbol),
                  stockDBdata = StockDBdata(symbol = separatorSymbol),
                  assets = emptyList(),
                  events = emptyList(),
                  dividends = emptyList()
              )
          )

          // bottom 5
          list.addAll(sortedList.takeLast(5))

          list
        } else {
          sortedList
        }

        stockRoomOverviewAdapter.setStockItems(filteredList)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
  }

  private fun updateSummary(stockitemList: List<StockItem>) {
    var totalPurchasePrice = 0.0
    var totalAssets = 0.0
    var totalGain = 0.0
    //var totalLoss = 0.0
    var totalQuantity = 0.0

    var capitalGain = 0.0
    var capitalLoss = 0.0

    stockitemList.forEach { stockItem ->
      val (quantity, price) = getAssets(stockItem.assets)

      totalPurchasePrice += price
      totalQuantity += quantity

      val (gain, loss, gainLossMap) = getAssetsCapitalGain(stockItem.assets)
      // Merge gain and loss of the individual stock to one gain/loss to prevent
      // having individual loss/gain reported in the summary.
      val capitalGainLoss = gain - loss
      when {
        capitalGainLoss > 0.0 -> {
          capitalGain += capitalGainLoss
        }
        capitalGainLoss < 0.0 -> {
          capitalLoss += -capitalGainLoss
        }
        else -> {
        }
      }

      if (stockItem.onlineMarketData.marketPrice > 0.0) {
        val assetsPrice = quantity * stockItem.onlineMarketData.marketPrice
        val gainLoss = assetsPrice - price

        if (gainLoss > 0.0) {
          totalGain += gainLoss
        }

        totalAssets += assetsPrice
      }
    }

    // Possible rounding error
    val gain = if (totalGain > 0.0) {
      totalGain
    } else {
      0.0
    }

    val totalLoss = if (totalAssets > 0.0) {
      totalGain - (totalAssets - totalPurchasePrice)
    } else {
      0.0
    }

    val loss = if (totalLoss > epsilon) {
      totalLoss
    } else {
      0.0
    }

    val total = if (totalAssets > 0.0) {
      totalAssets - totalPurchasePrice
    } else {
      0.0
    }

    val gainLossText = SpannableStringBuilder().append(
        "${requireContext().getString(R.string.summary_gain_loss)}  "
    )
        .append(
            getCapitalGainLossText(requireContext(), gain, loss, total, "-", "\n")
        )

    val totalAssetsStr =
      SpannableStringBuilder().append(
          "\n${requireContext().getString(R.string.summary_total_purchase_price)} "
      )
          .bold { append("${DecimalFormat(DecimalFormat2Digits).format(totalPurchasePrice)}") }

    if (totalAssets > 0.0) {
      totalAssetsStr.append(
          "\n${requireContext().getString(R.string.summary_total_assets)} "
      )
          .underline { bold { append(DecimalFormat(DecimalFormat2Digits).format(totalAssets)) } }
    }

    val summaryStr = SpannableStringBuilder()
        .append("${requireContext().getString(R.string.summary_stocks)} ")
        .bold { append("${stockitemList.size}\n\n") }
        .scale(1.4f) {
          append(gainLossText)
              .append(totalAssetsStr)
        }
    binding.textViewOverview.text = summaryStr
  }

  private fun clickListenerOverview(stockItem: StockItem) {
    if (stockItem.stockDBdata.symbol != separatorSymbol) {
      val intent = Intent(context, StockDataActivity::class.java)
      intent.putExtra("symbol", stockItem.onlineMarketData.symbol)
      //stockRoomViewModel.runOnlineTaskNow()
      startActivity(intent)
    }
  }
}
