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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.StockItem
import com.thecloudsite.stockroom.StockRoomViewModel
import com.thecloudsite.stockroom.databinding.FragmentCalcBinding

class CalcFragment : CalcBaseFragment() {

  private var _binding: FragmentCalcBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var stockitemListCopy: List<StockItem> = emptyList()
  private var listLoaded = false

  companion object {
    fun newInstance() = CalcFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    _binding = FragmentCalcBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun updateUI()
  {
    // scroll to always show last element at the bottom of the list
    binding.calclines.adapter?.itemCount?.minus(1)
      ?.let { binding.calclines.scrollToPosition(it) }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(requireActivity())

    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { stockitemList ->
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
            context?.let { ArrayAdapter(it, R.layout.calc_spinner_item, selectedList) }
        }
      }
    })

    calcViewModel = ViewModelProvider(requireActivity()).get(CalcViewModel::class.java)

    calcViewModel.calcData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {

        calcAdapter.updateData(data, numberFormat)

        // scroll to always show last element at the bottom of the list
        binding.calclines.adapter?.itemCount?.minus(1)
          ?.let { binding.calclines.scrollToPosition(it) }
      }
    })

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
            calcViewModel.add(marketPrice, "${stockitemListCopy[position].stockDBdata.symbol}=")
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
      val clipboardManager =
        context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
      val clipboardManager =
        context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      calcViewModel.setText(
        clipboardManager.primaryClip?.getItemAt(0)?.text.toString(),
        getString(R.string.clipboard)
      )
    }

    binding.calcPercentChange.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPercentChange.setOnClickListener { calcViewModel.opBinary(BinaryArgument.PERC) }
    binding.calcPercent.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPercent.setOnClickListener { calcViewModel.opBinary(BinaryArgument.PER) }
    binding.calcOver.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcOver.setOnClickListener { calcViewModel.opBinary(BinaryArgument.OVER) }
    binding.calcSwap.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSwap.setOnClickListener { calcViewModel.opBinary(BinaryArgument.SWAP) }
    binding.calcSQR.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSQR.setOnClickListener { calcViewModel.opUnary(UnaryArgument.SQR) }
    binding.calcSQ.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSQ.setOnClickListener { calcViewModel.opUnary(UnaryArgument.SQ) }
    binding.calcPOW.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPOW.setOnClickListener { calcViewModel.opBinary(BinaryArgument.POW) }
    binding.calcINV.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcINV.setOnClickListener { calcViewModel.opUnary(UnaryArgument.INV) }
    binding.calcEnter.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcEnter.setOnClickListener { calcViewModel.enter() }
    binding.calcSign.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSign.setOnClickListener { calcViewModel.opUnary(UnaryArgument.SIGN) }
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
    binding.calcDot.setOnClickListener { calcViewModel.addNum(separatorChar, context) }

    binding.calcDiv.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcDiv.setOnClickListener { calcViewModel.opBinary(BinaryArgument.DIV) }
    binding.calcMult.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcMult.setOnClickListener { calcViewModel.opBinary(BinaryArgument.MULT) }
    binding.calcSub.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSub.setOnClickListener { calcViewModel.opBinary(BinaryArgument.SUB) }
    binding.calcAdd.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcAdd.setOnClickListener { calcViewModel.opBinary(BinaryArgument.ADD) }
  }

  override fun onResume() {
    super.onResume()

    binding.calcDot.text = separatorChar.toString()
  }
}
