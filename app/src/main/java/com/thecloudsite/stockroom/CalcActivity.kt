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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.databinding.ActivityCalcBinding

class CalcActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCalcBinding
  private var numberList: MutableList<Double> = mutableListOf()
  private var editMode: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityCalcBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val calcAdapter = CalcAdapter(this)
    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(this)

    binding.calcEnter.setOnClickListener {
      if (editMode) {
        editMode = false
      }

      if (numberList.size > 0) {
        val op = numberList.last()
        numberList.add(op)
      }

      calcAdapter.updateData(numberList, editMode)
    }

    binding.calc1.setOnClickListener {
      if (!editMode) {
        editMode = true
        numberList.add(1.0)
        calcAdapter.updateData(numberList, editMode)
      } else {
        if (numberList.isEmpty()) {
          numberList.add(1.0)
        } else {
          numberList[numberList.size - 1] = numberList[numberList.size - 1] * 10.0 + 1.0
        }

        calcAdapter.updateData(numberList, editMode)
      }
    }

    binding.calcPlus.setOnClickListener {
      if (numberList.size >= 2) {
        editMode = false
        val op1 = numberList.removeLast()
        val op2 = numberList.removeLast()
        numberList.add(op1 + op2)

        calcAdapter.updateData(numberList, editMode)
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}
