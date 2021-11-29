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

import android.R.layout
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.text.bold
import androidx.core.text.scale
import androidx.core.text.underline
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.DialogAddRemoveSelectionBinding
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
  private var clickCounter = 10

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val separatorSymbol = "  ⋮"
  private var stockitemListCopy: List<StockItem> = emptyList()

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

    val clickListenerSymbolLambda = { stockItem: StockItem -> clickListenerOverview(stockItem) }

    // Use the small list adapter for display.
    val stockRoomOverviewAdapter =
      StockRoomSmallListAdapter(requireContext(), clickListenerSymbolLambda)
    val stockRoomOverviewSelectionAdapter =
      StockRoomSmallListAdapter(requireContext(), clickListenerSymbolLambda)

    val recyclerView = binding.recyclerview
    recyclerView.adapter = stockRoomOverviewAdapter

    val recyclerViewSelection = binding.recyclerviewSelection
    recyclerViewSelection.adapter = stockRoomOverviewSelectionAdapter

    // Set column number depending on screen width.
    val scale = 494
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    recyclerView.layoutManager = GridLayoutManager(
      context,
      Integer.min(Integer.max(spanCount, 1), 10)
    )

    recyclerViewSelection.layoutManager = GridLayoutManager(
      context,
      Integer.min(Integer.max(spanCount, 1), 10)
    )

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockitemList ->

        // used by the selection
        stockitemListCopy = stockitemList

        updateSummary(stockitemList)

        // Add top/bottom items.
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

        // Add selected items.
        val overviewSelection: Set<String> =
          sharedPreferences.getStringSet("overview_selection", emptySet()) ?: emptySet()
        val selectedList = stockitemList.filter { item ->
          overviewSelection.contains(item.stockDBdata.symbol)
        }

        stockRoomOverviewSelectionAdapter.setStockItems(selectedList)
      }
    })

    binding.addSelectionButton.setOnClickListener {
      val builder = android.app.AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())
      val dialogBinding = DialogAddRemoveSelectionBinding.inflate(inflater)

      // add all symbols
//      stockRoomViewModel.allStockDBdata.observe(requireActivity(), Observer { items ->
//        if (items != null) {
//          val spinnerData = items.map { stockItem ->
//            stockItem.symbol
//          }
//            .sorted()
//
//          dialogBinding.textViewSymbolSpinner.adapter =
//            ArrayAdapter(requireContext(), layout.simple_list_item_1, spinnerData)
//        }
//      })

      // add portfolio symbols
      val overviewSelection: Set<String> =
        sharedPreferences.getStringSet("overview_selection", emptySet()) ?: emptySet()
      val spinnerData = stockitemListCopy.filter { item ->
        !overviewSelection.contains(item.stockDBdata.symbol)
      }.map { stockItem ->
        stockItem.stockDBdata.symbol
      }
        .sorted()

      dialogBinding.textViewSymbolSpinner.adapter =
        ArrayAdapter(requireContext(), layout.simple_list_item_1, spinnerData)

      builder.setView(dialogBinding.root)
        .setTitle(R.string.add_selection)
        // Add action buttons
        .setPositiveButton(R.string.add)
        { _, _ ->

          if (dialogBinding.textViewSymbolSpinner.isEmpty()) {
            Toast.makeText(
              requireContext(),
              getString(string.no_symbols_available),
              Toast.LENGTH_LONG
            )
              .show()
            return@setPositiveButton
          }

          val symbol = dialogBinding.textViewSymbolSpinner.selectedItem.toString()

          val overviewSelection: MutableSet<String> = mutableSetOf()
          overviewSelection.addAll(
            sharedPreferences.getStringSet("overview_selection", emptySet()) ?: emptySet()
          )
          overviewSelection.add(symbol)
          sharedPreferences
            .edit()
            .putStringSet("overview_selection", overviewSelection)
            .apply()

          val selectedList = stockitemListCopy.filter { item ->
            overviewSelection.contains(item.stockDBdata.symbol)
          }
          stockRoomOverviewSelectionAdapter.setStockItems(selectedList)
        }
        .setNegativeButton(
          R.string.cancel
        )
        { _, _ ->
        }
      builder
        .create()
        .show()
    }

    binding.removeSelectionButton.setOnClickListener {
      val builder = android.app.AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())
      val dialogBinding = DialogAddRemoveSelectionBinding.inflate(inflater)

      val overviewSelection: MutableSet<String> =
        sharedPreferences.getStringSet("overview_selection", emptySet()) ?: mutableSetOf()
      val selectedList = stockitemListCopy.filter { item ->
        overviewSelection.contains(item.stockDBdata.symbol)
      }.map { item ->
        item.stockDBdata.symbol
      }.sorted()

      val spinnerData: List<String> = selectedList

      dialogBinding.textViewSymbolSpinner.adapter =
        ArrayAdapter(requireContext(), layout.simple_list_item_1, spinnerData)

      builder.setView(dialogBinding.root)
        .setTitle(R.string.remove_selection)
        // Add action buttons
        .setPositiveButton(R.string.delete) { _, _ ->

          if (dialogBinding.textViewSymbolSpinner.isEmpty()) {
            Toast.makeText(
              requireContext(),
              getString(string.no_symbols_available),
              Toast.LENGTH_LONG
            )
              .show()
            return@setPositiveButton
          }

          val symbol = dialogBinding.textViewSymbolSpinner.selectedItem.toString()
          val overviewSelection: MutableSet<String> = mutableSetOf()
          overviewSelection.addAll(
            sharedPreferences.getStringSet("overview_selection", emptySet()) ?: mutableSetOf()
          )
          overviewSelection.remove(symbol)
          sharedPreferences
            .edit()
            .putStringSet("overview_selection", overviewSelection)
            .apply()
          val selectedList = stockitemListCopy.filter { item ->
            overviewSelection.contains(item.stockDBdata.symbol)
          }
          stockRoomOverviewSelectionAdapter.setStockItems(selectedList)
        }
        .setNegativeButton(
          R.string.cancel
        ) { _, _ ->
        }
      builder
        .create()
        .show()
    }
  }

  override fun onResume() {
    super.onResume()

    clickCounter = 10
    stockRoomViewModel.runOnlineTaskNow()
  }

  private fun updateSummary(stockitemList: List<StockItem>) {
    var totalPurchasePrice = 0.0
    var totalAssets = 0.0
    var totalGain = 0.0
    var totalLoss = 0.0
    var totalQuantity = 0.0

    var capitalGain = 0.0
    var capitalLoss = 0.0

    stockitemList.forEach { stockItem ->
      val (quantity, price, fee) = getAssets(stockItem.assets)

      totalPurchasePrice += price + fee
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
        val gainLoss = assetsPrice - (price + fee)

        if (gainLoss > 0.0) {
          totalGain += gainLoss
        }

        if (gainLoss < 0.0) {
          totalLoss -= gainLoss
        }

        totalAssets += assetsPrice
      }
    }

    // Possible rounding error
    val gain = if (totalGain >= epsilon) {
      totalGain.times(100).roundToInt() / 100.0
    } else {
      0.0
    }

    // Alternative total: sum all gains, and get the loss by subtracting from the total
    // for minimal rounding error. But with no online data, only gain of the assets will be counted.
//    val totalLoss = if (totalAssets >= epsilon) {
//      totalGain - (totalAssets - totalPurchasePrice)
//    } else {
//      0.0
//    }

    val loss = if (totalLoss >= epsilon) {
      totalLoss.times(100).roundToInt() / 100.0
    } else {
      0.0
    }

//    val total = if (totalAssets >= epsilon) {
//      totalAssets - totalPurchasePrice
//    } else {
//      0.0
//    }

    val total = gain - loss

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
        .bold { append(DecimalFormat(DecimalFormat2Digits).format(totalPurchasePrice)) }

    if (totalAssets >= epsilon) {
      totalAssetsStr.append(
        "\n${requireContext().getString(R.string.summary_total_assets)} "
      )
        .underline { bold { append(DecimalFormat(DecimalFormat2Digits).format(totalAssets)) } }
    }

    val summaryStr = SpannableStringBuilder()
    if (stockitemList.size > 3) {
      summaryStr.append("${requireContext().getString(R.string.summary_stocks)} ")
        .bold { append("${stockitemList.size}\n\n") }
    }
    summaryStr.scale(1.4f) {
      append(gainLossText)
        .append(totalAssetsStr)
    }

    binding.textViewOverview.text = summaryStr
  }

  private fun clickListenerOverview(stockItem: StockItem) {
    if (stockItem.stockDBdata.symbol != separatorSymbol) {
      val intent = Intent(context, StockDataActivity::class.java)
      intent.putExtra(EXTRA_SYMBOL, stockItem.onlineMarketData.symbol)
      intent.putExtra(EXTRA_TYPE, stockItem.stockDBdata.type)
      //stockRoomViewModel.runOnlineTaskNow()
      startActivity(intent)
    } else {
      clickCounter--

      if (clickCounter == 0) {
        AlertDialog.Builder(requireContext())
          // https://convertcodes.com/unicode-converter-encode-decode-utf/
          .setTitle(
            "\u0044\u0069\u0064\u0020\u0079\u006f\u0075\u0020\u0067\u0065\u0074\u0020\u0074\u0068\u0065\u0020\u0041\u006c\u0069\u0065\u006e\u0073\u003f"
          )
          .setMessage(
            "\u004c\u006f\u006f\u006b\u0020\u006f\u0075\u0074\u0020\u0066\u006f\u0072\u0020\u0074\u0068\u0065\u006d\u002e"
          )
          .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
          .show()
      }
    }
  }
}
