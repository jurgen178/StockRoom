package com.example.android.stockroom

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// https://stackoverflow.com/questions/55372259/how-to-use-tablayout-with-viewpager2-in-android

enum class SortMode(val value: Int) {
  ByName(0),
  ByAssets(1),
  ByProfit(2),
  ByChange(3),
  ByDividend(4),
  ByGroup(5),
  ByUnsorted(6),
}

class ListFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = ListFragment()
  }

  private fun clickListenerGroup(
    stockItem: StockItem,
    itemView: View
  ) {
    val popupMenu = PopupMenu(context, itemView)

    var menuIndex: Int = Menu.FIRST
    stockRoomViewModel.getGroupsMenuList(getString(R.string.standard_group))
        .forEach {
          popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
        }

    popupMenu.show()

    val groups: List<Group> = stockRoomViewModel.getGroupsSync()
    popupMenu.setOnMenuItemClickListener { menuitem ->
      val i: Int = menuitem.itemId - 1
      val clr: Int
      val name: String

      if (i >= groups.size) {
        clr = 0
        name = getString(R.string.standard_group)
      } else {
        clr = groups[i].color
        name = groups[i].name
      }
      // Set the preview color in the activity.
      //setBackgroundColor(textViewGroupColor, clr)
      //textViewGroup.text = name

      // Store the selection.
      stockRoomViewModel.setGroup(stockItem.stockDBdata.symbol, name, clr)
      true
    }
  }

  private fun clickListenerSummary(stockItem: StockItem) {
    val intent = Intent(context, StockDataActivity::class.java)
    intent.putExtra("symbol", stockItem.onlineMarketData.symbol)
    startActivity(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val clickListenerGroup =
      { stockItem: StockItem, itemView: View -> clickListenerGroup(stockItem, itemView) }
    val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    val adapter = StockRoomListAdapter(requireContext(), clickListenerGroup, clickListenerSummary)

    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

    recyclerView.adapter = adapter
    recyclerView.layoutManager = LinearLayoutManager(context)

    // Get a new or existing ViewModel from the ViewModelProvider.
    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("List activity started.")

    // Rotating device keeps sending alerts.
    // State changes of the lifecycle trigger the notification.
    stockRoomViewModel.resetAlerts()

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let {
        adapter.setStockItems(it)
      }
    })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_list, container, false)
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
        stockRoomViewModel.logDebug("Update online data manually for list data.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updateSortMode(sortMode: SortMode) {
    stockRoomViewModel.updateSortMode(sortMode)
  }
}
