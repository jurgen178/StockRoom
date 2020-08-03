package com.android.stockroom

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SummaryListFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = SummaryListFragment()
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
    return inflater.inflate(R.layout.fragment_summarylist, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Summary list activity started.")

    val clickListenerListItem = { stockItem: StockItem -> clickListenerListItem(stockItem) }
    val summaryListAdapter = SummaryListAdapter(requireContext(), clickListenerListItem)
    val summaryList = view.findViewById<RecyclerView>(R.id.summaryList)
    summaryList.adapter = summaryListAdapter

    // Set column number depending on orientation.
    val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      3
    } else {
      5
    }

    summaryList.layoutManager = GridLayoutManager(requireContext(), spanCount)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItems ->
        summaryListAdapter.updateData(stockItems)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.updateOnlineDataManually()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sort_name -> {
        updateSortMode(SortMode.ByName)
        true
      }
      R.id.menu_sort_assets -> {
        updateSortMode(SortMode.ByAssets)
        true
      }
      R.id.menu_sort_profit -> {
        updateSortMode(SortMode.ByProfit)
        true
      }
      R.id.menu_sort_change -> {
        updateSortMode(SortMode.ByChange)
        true
      }
      R.id.menu_sort_dividend -> {
        updateSortMode(SortMode.ByDividend)
        true
      }
      R.id.menu_sort_group -> {
        updateSortMode(SortMode.ByGroup)
        true
      }
      R.id.menu_sort_unsorted -> {
        updateSortMode(SortMode.ByUnsorted)
        true
      }
      R.id.menu_sync -> {
        stockRoomViewModel.updateOnlineDataManually()
        stockRoomViewModel.logDebug("Update online data manually for summary list data.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updateSortMode(sortMode: SortMode) {
    stockRoomViewModel.updateSortMode(sortMode)
  }

  private fun clickListenerListItem(stockItem: StockItem) {
    val intent = Intent(context, StockDataActivity::class.java)
    intent.putExtra("symbol", stockItem.onlineMarketData.symbol)
    startActivity(intent)
  }
}
