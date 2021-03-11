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

package com.thecloudsite.stockroom.calc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.StockItem
import com.thecloudsite.stockroom.StockRoomViewModel
import com.thecloudsite.stockroom.setBackgroundColor
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

// Support selection of the same item
class CustomSpinner(
  context: Context,
  attrs: AttributeSet?
) :
  androidx.appcompat.widget.AppCompatSpinner(context, attrs) {
  override fun setSelection(position: Int) {
    val sameSelected = position == selectedItemPosition
    super.setSelection(position)
    if (sameSelected) {
      // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
      onItemSelectedListener!!.onItemSelected(this, selectedView, position, selectedItemId)
    }
  }
}

open class CalcBaseFragment(val stockSymbol: String) : Fragment() {

  lateinit var calcViewModel: CalcViewModel
  lateinit var calcAdapter: CalcAdapter
  lateinit var stockRoomViewModel: StockRoomViewModel
  var stockitemListCopy: List<StockItem> = emptyList()

  var radian = 1.0
  var separatorChar = ','
  var numberFormat: NumberFormat = NumberFormat.getNumberInstance()

  fun touchHelper(
    view: View,
    event: MotionEvent,
    colorDown: Int = Color.LTGRAY,
    colorUp: Int = Color.DKGRAY
  ) {
    if (event.action == MotionEvent.ACTION_DOWN) {
      setBackgroundColor(view, colorDown)
    } else
      if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
        setBackgroundColor(view, colorUp)
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  open fun updateCalcAdapter() {
  }

  open fun updateStockListSpinner(symbol: String) {
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    calcAdapter = CalcAdapter(requireActivity())
    calcViewModel = ViewModelProvider(requireActivity()).get(CalcViewModel::class.java)

    // Update symbol
    if (stockSymbol.isNotEmpty()) {
      calcViewModel.symbol = stockSymbol
    }

    calcViewModel.calcData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {

        calcAdapter.updateData(data, numberFormat)

        updateCalcAdapter()
      }
    })

    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { stockitemList ->
      if (stockitemList != null && stockitemList.isNotEmpty()) {

        // used by the selection
        stockitemListCopy = stockitemList.sortedBy { stockItem ->
          stockItem.stockDBdata.symbol
        }

        calcViewModel.stockitemList = stockitemListCopy

        updateStockListSpinner(calcViewModel.symbol)
      }
    })

//    stockRoomViewModel.onlineMarketDataList.observe(
//      viewLifecycleOwner,
//      Observer { onlineMarketDataList ->
//      })
  }

  override fun onResume() {
    super.onResume()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)

    when (sharedPreferences.getString("calc_format_decimal_separator", "0")) {
      "0" -> {
        separatorChar = DecimalFormatSymbols.getInstance().decimalSeparator
        numberFormat = NumberFormat.getNumberInstance()
      }
      "1" -> {
        separatorChar = '.'
        numberFormat = NumberFormat.getInstance(Locale.ENGLISH)
      }
      else -> {
        separatorChar = ','
        numberFormat = NumberFormat.getInstance(Locale.GERMAN)
      }
    }

    when (sharedPreferences.getString("calc_format_displayed_decimals", "1")) {
      "0" -> {
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
      }
      "1" -> {
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 4
      }
      "2" -> {
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 8
      }
      else -> {
        // https://developer.android.com/reference/java/text/DecimalFormat#setMaximumFractionDigits(int)
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 340
      }
    }

    numberFormat.isGroupingUsed =
      sharedPreferences.getBoolean("calc_format_display_group_separator", true)

    radian = if (
      sharedPreferences.getBoolean("calc_format_radian", true)) {
      1.0
    } else {
      Math.PI / 180
    }

    calcViewModel.radian = radian
    calcViewModel.separatorChar = separatorChar
    calcViewModel.numberFormat = numberFormat

    // Redraw the valued displayed in the adapter with the new number format.
    calcViewModel.updateData()
  }
}
