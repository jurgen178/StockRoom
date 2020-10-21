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
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.database.Group

enum class SortMode(val value: Int) {
  ByChange(0),
  ByName(1),
  ByAssets(2),
  ByProfit(3),
  ByDividend(4),
  ByGroup(5),
  ByUnsorted(6),
}

open class StockRoomBaseFragment : Fragment() {

  lateinit var stockRoomViewModel: StockRoomViewModel

  fun clickListenerGroup(
    stockItem: StockItem,
    itemView: View
  ) {
    val popupMenu = PopupMenu(context, itemView)

    var menuIndex: Int = Menu.FIRST
    stockRoomViewModel.getGroupsMenuList(getString(string.standard_group))
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
        name = getString(string.standard_group)
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

  fun clickListenerSummary(stockItem: StockItem) {
    val intent = Intent(context, StockDataActivity::class.java)
    intent.putExtra("symbol", stockItem.onlineMarketData.symbol)
    //stockRoomViewModel.runOnlineTaskNow()
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

    // Get a new or existing ViewModel from the ViewModelProvider.
    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    // Rotating device keeps sending alerts.
    // State changes of the lifecycle trigger the notification.
    stockRoomViewModel.resetAlerts()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    return inflater.inflate(layout.fragment_list, container, false)
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
  }
}
