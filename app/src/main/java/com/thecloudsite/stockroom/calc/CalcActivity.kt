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

import android.R.layout
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.MainActivity
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.StockAssetsLiveData
import com.thecloudsite.stockroom.StockItem
import com.thecloudsite.stockroom.StockRoomViewModel
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.ActivityCalcBinding
import com.thecloudsite.stockroom.setBackgroundColor
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class CalcActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCalcBinding
  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var stockitemListCopy: List<StockItem> = emptyList()
  private lateinit var calcViewModel: CalcViewModel
  private var separatorChar = ','
  private var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
  private var listLoaded = false
  lateinit var onlineDataHandler: Handler

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityCalcBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    onlineDataHandler = Handler(Looper.getMainLooper())

    val calcAdapter = CalcAdapter(this)
    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(this)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allStockItems.observe(this, Observer { stockitemList ->
      if (stockitemList != null && stockitemList.isNotEmpty()) {

        // used by the selection
        stockitemListCopy = stockitemList.sortedBy { stockItem ->
          stockItem.stockDBdata.symbol
        }

        if (!listLoaded) {

          val selectedList = stockitemListCopy.map { item ->
            item.stockDBdata.symbol
          }

          binding.calcStocks.adapter =
            ArrayAdapter(this, layout.simple_list_item_1, selectedList)
        }
      }
    })

    calcViewModel = ViewModelProvider(this).get(CalcViewModel::class.java)

    calcViewModel.calcData.observe(this, Observer { data ->
      if (data != null) {

        calcAdapter.updateData(data, numberFormat)

        // scroll to always show last element at the bottom of the list
        binding.calclines.adapter?.itemCount?.minus(1)
          ?.let { binding.calclines.scrollToPosition(it) }
      }
    })

    fun touchHelper(view: View, event: MotionEvent) {
      if (event.action == MotionEvent.ACTION_DOWN) {
        setBackgroundColor(view, Color.LTGRAY)
      } else
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
          setBackgroundColor(view, Color.DKGRAY)
        }
    }

    binding.calcStocks.setOnTouchListener { view, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {

        // Get the latest market value for the stock.
        stockRoomViewModel.runOnlineTaskNow()

        listLoaded = true
      }

      false
    }
    binding.calcStocks.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        // skip the first selection caused by the initial list loading
        if (listLoaded && position >= 0 && position < stockitemListCopy.size) {
          var marketPrice = stockitemListCopy[position].onlineMarketData.marketPrice
          if (marketPrice == 0.0) {
            val (quantity, price, commission) = com.thecloudsite.stockroom.utils.getAssets(
              stockitemListCopy[position].assets
            )
            if (quantity != 0.0 && price != 0.0) {
              marketPrice = price / quantity
            }
          }
          if (marketPrice != 0.0) {
            calcViewModel.add(marketPrice)
          }
        }
      }
    }

    binding.calcCopyToClipboard.setOnTouchListener { view, event ->
      touchHelper(
        view,
        event
      )
      false
    }
    binding.calcCopyToClipboard.setOnClickListener {
      val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clipData = ClipData.newPlainText("text", calcViewModel.getText())
      clipboardManager.setPrimaryClip(clipData)
    }

    binding.calcCopyFromClipboard.setOnTouchListener { view, event ->
      touchHelper(
        view,
        event
      )
      false
    }
    binding.calcCopyFromClipboard.setOnClickListener {
      val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      calcViewModel.setText(clipboardManager.primaryClip?.getItemAt(0)?.text.toString())
    }

    binding.calcSwap.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSwap.setOnClickListener { calcViewModel.opBinary(BinaryOperation.SWAP) }
    binding.calcSQR.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSQR.setOnClickListener { calcViewModel.opUnary(UnaryOperation.SQR) }
    binding.calcSQ.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSQ.setOnClickListener { calcViewModel.opUnary(UnaryOperation.SQ) }
    binding.calcPOW.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPOW.setOnClickListener { calcViewModel.opBinary(BinaryOperation.POW) }
    binding.calcINV.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcINV.setOnClickListener { calcViewModel.opUnary(UnaryOperation.INV) }
    binding.calcEnter.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcEnter.setOnClickListener { calcViewModel.enter() }
    binding.calcSign.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSign.setOnClickListener { calcViewModel.opUnary(UnaryOperation.SIGN) }
    binding.calcDrop.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcDrop.setOnClickListener { calcViewModel.drop() }

    binding.calc1.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc1.setOnClickListener { calcViewModel.addNum('1') }
    binding.calc2.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc2.setOnClickListener { calcViewModel.addNum('2') }
    binding.calc3.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc3.setOnClickListener { calcViewModel.addNum('3') }
    binding.calc4.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc4.setOnClickListener { calcViewModel.addNum('4') }
    binding.calc5.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc5.setOnClickListener { calcViewModel.addNum('5') }
    binding.calc6.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc6.setOnClickListener { calcViewModel.addNum('6') }
    binding.calc7.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc7.setOnClickListener { calcViewModel.addNum('7') }
    binding.calc8.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc8.setOnClickListener { calcViewModel.addNum('8') }
    binding.calc9.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc9.setOnClickListener { calcViewModel.addNum('9') }
    binding.calc0.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calc0.setOnClickListener { calcViewModel.addNum('0') }
    binding.calcDot.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcDot.setOnClickListener { calcViewModel.addNum(separatorChar, this) }

    binding.calcDiv.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcDiv.setOnClickListener { calcViewModel.opBinary(BinaryOperation.DIV) }
    binding.calcMult.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcMult.setOnClickListener { calcViewModel.opBinary(BinaryOperation.MULT) }
    binding.calcSub.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSub.setOnClickListener { calcViewModel.opBinary(BinaryOperation.SUB) }
    binding.calcAdd.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcAdd.setOnClickListener { calcViewModel.opBinary(BinaryOperation.ADD) }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.calc_menu, menu)
    return true
  }

  private val onlineDataTask = object : Runnable {
    override fun run() {
      stockRoomViewModel.runOnlineTask()
      onlineDataHandler.postDelayed(this, MainActivity.onlineDataTimerDelay)
    }
  }

  override fun onPause() {
    onlineDataHandler.removeCallbacks(onlineDataTask)
    super.onPause()
  }

  override fun onResume() {
    super.onResume()

    onlineDataHandler.post(onlineDataTask)
    stockRoomViewModel.runOnlineTaskNow()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)

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

    calcViewModel.separatorChar = separatorChar
    calcViewModel.numberFormat = numberFormat

    binding.calcDot.text = separatorChar.toString()

    // Redraw the valued displayed in the adapter with the new number format.
    calcViewModel.updateData()
  }

  fun onSettings(item: MenuItem) {
    val intent = Intent(this@CalcActivity, CalcSettingsActivity::class.java)
    startActivity(intent)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}
