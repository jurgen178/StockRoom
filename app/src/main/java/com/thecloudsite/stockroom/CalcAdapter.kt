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

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.databinding.CalcItemBinding
import java.text.DecimalFormat

const val DecimalFormatCalcDigits = "#,##0.00######"

class CalcAdapter internal constructor(
  private val context: Context
) : RecyclerView.Adapter<CalcAdapter.CalcViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var numberList: List<Double> = emptyList()
  private var cursorPos: Int = 0
  private var editMode: Boolean = false

  class CalcViewHolder(
    val binding: CalcItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CalcViewHolder {

    val binding = CalcItemBinding.inflate(inflater, parent, false)
    return CalcViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: CalcViewHolder,
    position: Int
  ) {
    val current = numberList[position]

    if (this.editMode && position == numberList.size - 1) {
      holder.binding.calclineNumber.gravity = Gravity.START
    } else {
      holder.binding.calclineNumber.gravity = Gravity.END
    }

    holder.binding.calclineNumber.text = DecimalFormat(DecimalFormatCalcDigits).format(
      current
    )
  }

  fun updateData(numbers: List<Double>, editMode: Boolean) {
    numberList = numbers
    this.editMode = editMode

    notifyDataSetChanged()
  }

  override fun getItemCount() = numberList.size
}
