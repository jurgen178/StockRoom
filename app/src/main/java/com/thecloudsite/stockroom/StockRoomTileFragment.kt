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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.databinding.FragmentTileBinding
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.roundToInt

class StockRoomTileFragment : Fragment() {

  private var _binding: FragmentTileBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = StockRoomTileFragment()
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
    _binding = FragmentTileBinding.inflate(inflater, container, false)
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

    val clickListenerListItem = { stockItem: StockItem -> clickListenerListItem(stockItem) }
    val stockRoomTileAdapter = StockRoomTileAdapter(requireContext(), clickListenerListItem)
    val stockRoomTile = binding.stockroomtile
    stockRoomTile.adapter = stockRoomTileAdapter

    /*
    // Set column number depending on orientation and size.
    val spanCountsPortrait = arrayOf(
        // small, standard, large, larger
        // 0.85, 1, 1.15, 1.3
        intArrayOf(4, 3, 3, 3),// 375
        intArrayOf(3, 3, 3, 2),// 440
        intArrayOf(3, 3, 2, 2),// 490
        intArrayOf(3, 2, 2, 2) // 540
    )

    val spanCountsLandscape = arrayOf(
        // 0.85, 1, 1.15, 1.3
        intArrayOf(7, 5, 5, 5),// 375
        intArrayOf(5, 5, 5, 4),// 440
        intArrayOf(5, 5, 4, 4),// 490
        intArrayOf(5, 4, 4, 4) // 540
    )

    //  x: fontScale
    // 0.85, 1, 1.15, 1.3
    val x = when {
      resources.configuration.fontScale <= 0.85 -> {
        0
      }
      resources.configuration.fontScale <= 1.0 -> {
        1
      }
      resources.configuration.fontScale <= 1.15 -> {
        2
      }
      else -> {
        3
      }
    }

    //  y: densityDpi
    // 375, 440, 490, 540
    val y = when {
      resources.configuration.densityDpi <= 375 -> {
        0
      }
      resources.configuration.densityDpi <= 440 -> {
        1
      }
      resources.configuration.densityDpi <= 490 -> {
        2
      }
      else -> {
        3
      }
    }

    // screenWidthDp Pixel 3a
    // font: 0.85, 1, 1.15, 1.3
    // portrait:  462 392 352 320
    // landscape: 901 759 676 609

    // screenWidthDp Surface Duo
    // font: 0.85, 1, 1.15, 1.3
    // portrait:   635 540 469 415
    // landscape:  847 720 626 553

    val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      spanCountsPortrait[y][x]
    } else {
      spanCountsLandscape[y][x]
    }
   */

//    5, 4, 4, 3,    // 540

//    4, 3, 3, 3,    // 462
//    3, 3, 3, 2,    // 392
//    3, 3, 2, 2,    // 352
//    3, 3, 2, 2,    // 320

//    7, 6, 6, 5,    // 901
//    6, 5, 5, 4,    // 759
//    6, 5, 4, 4,    // 676
//    5, 4, 4, 4,    // 609

    // Set column number depending on screen width.
    val scale = 156
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    stockRoomTile.layoutManager = GridLayoutManager(requireContext(), min(max(spanCount, 2), 20))

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItems ->
        stockRoomTileAdapter.updateData(stockItems)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.runOnlineTaskNow()
  }

  private fun clickListenerListItem(stockItem: StockItem) {
    val intent = Intent(context, StockDataActivity::class.java)
    intent.putExtra("symbol", stockItem.onlineMarketData.symbol)
    //stockRoomViewModel.runOnlineTaskNow()
    startActivity(intent)
  }
}
