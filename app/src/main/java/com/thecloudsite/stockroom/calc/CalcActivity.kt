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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.databinding.ActivityCalcBinding

class CalcActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCalcBinding
  private lateinit var calcViewModel: CalcViewModel

  private var calcData: CalcData = CalcData()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityCalcBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val calcAdapter = CalcAdapter(this)
    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(this)

    calcViewModel = ViewModelProvider(this).get(CalcViewModel::class.java)

    calcViewModel.calcData.observe(this, Observer { data ->
      if (data != null) {
        calcData = data

        calcAdapter.updateData(data)

        // scroll to always show last element at the bottom of the list
        binding.calclines.adapter?.itemCount?.minus(1)
          ?.let { binding.calclines.scrollToPosition(it) }
      }
    })

    binding.calcEnter.setOnClickListener {
      if (calcData.editMode) {
        calcData.editMode = false
      } else {
        if (calcData.numberList.size > 0) {
          val op = calcData.numberList.last()
          calcData.numberList.add(op)
        }
      }

      calcViewModel.updateData(calcData)
    }

    fun num(value: Double) {
      if (calcData.editMode) {
        if (calcData.numberList.isEmpty()) {
          calcData.numberList.add(value)
        } else {
          calcData.numberList[calcData.numberList.size - 1] =
            calcData.numberList[calcData.numberList.size - 1] * 10.0 + value
        }
      } else {
        calcData.editMode = true
        calcData.numberList.add(value)
      }

      calcViewModel.updateData(calcData)
    }

    binding.calc1.setOnClickListener { num(1.0) }
    binding.calc2.setOnClickListener { num(2.0) }
    binding.calc3.setOnClickListener { num(3.0) }

    binding.calcPlus.setOnClickListener {
      if (calcData.numberList.size >= 2) {
        calcData.editMode = false
        val op1 = calcData.numberList.removeLast()
        val op2 = calcData.numberList.removeLast()
        calcData.numberList.add(op1 + op2)

        calcViewModel.updateData(calcData)
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}
