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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.databinding.FragmentListBinding
import com.thecloudsite.stockroom.utils.getMarkerText

enum class SortMode(val value: Int) {
    ByChangePercentage(0),
    ByName(1),
    ByPurchaseprice(2),
    ByAssets(3),
    ByProfit(4),
    ByProfitPercentage(5),
    ByMarketCap(6),
    ByDividendPercentage(7),
    ByGroup(8),
    ByMarker(9),
    ByActivity(10)
    //ByUnsorted(11),
}

// Implements the lambda functions used by the StockDataFragment (inherits StockRoomBaseLambdaFragment)
// and the list fragments to support clicking the group and the marker at the left hand border.
// Otherwise the same functions need to be implemented in both places.
open class StockRoomBaseLambdaFragment : Fragment() {

    lateinit var stockRoomViewModel: StockRoomViewModel

    fun clickListenerGroup(
            stockItem: StockItem,
            itemView: View
    ) {
        val popupMenu = PopupMenu(context, itemView)

        var menuIndex: Int = Menu.FIRST
        stockRoomViewModel.getGroupsMenuList(
                requireContext(),
                getString(string.standard_group),
                requireContext().getColor(R.color.black) ?: 0
        )
                .forEach {
                    popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
                }

        popupMenu.show()

        val groups: List<Group> = stockRoomViewModel.getGroupsSync()
        popupMenu.setOnMenuItemClickListener { menuitem ->
            val i: Int = menuitem.itemId - 2
            val clr: Int
            val name: String

            // Check if first item (menuitem.itemId=1, i=-1) is selected.
            if (i < 0) {
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

    fun clickListenerMarker(
            stockItem: StockItem,
            itemView: View
    ) {
        val popupMenu = PopupMenu(context, itemView)

        var menuIndex: Int = Menu.FIRST

        (0..10).map { context?.let { it1 -> getMarkerText(it1, it) } }.forEach {
            popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
        }

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuitem ->
            val marker: Int = menuitem.itemId - 1

            // Store the selected marker.
            stockRoomViewModel.setMarker(stockItem.stockDBdata.symbol, marker)
            true
        }
    }

    fun clickListenerSymbol(stockItem: StockItem) {
        val intent = Intent(context, StockDataActivity::class.java)
        intent.putExtra(EXTRA_SYMBOL, stockItem.onlineMarketData.symbol)
        intent.putExtra(EXTRA_TYPE, stockItem.stockDBdata.type)
        //stockRoomViewModel.runOnlineTaskNow()
        startActivity(intent)
    }
}

open class StockRoomBaseFragment : StockRoomBaseLambdaFragment() {

    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    val binding get() = _binding!!

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
        stockRoomViewModel =
                ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

        // Rotating device keeps sending alerts.
        // State changes of the lifecycle trigger the notification.
        stockRoomViewModel.resetAlerts()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        stockRoomViewModel.runOnlineTaskNow()
    }
}
