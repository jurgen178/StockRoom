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
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.calc.CalcAdapter.CalcViewHolder
import com.thecloudsite.stockroom.databinding.CalcItemBinding
import java.text.DecimalFormat

const val DecimalFormatCalcDigits = "#,##0.########"

class CalcAdapter internal constructor(
  private val context: Context
) : RecyclerView.Adapter<CalcViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var calcData: CalcData = CalcData()

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

    val lineText =
      if (this.calcData.editMode && position == this.calcData.numberList.size) {

        // edit line
        holder.binding.calclineNumber.gravity = Gravity.START
        this.calcData.editline + "‹"

      } else
        if (position >= 0 && position < this.calcData.numberList.size) {

          // number list
          val current = this.calcData.numberList[position]
          holder.binding.calclineNumber.gravity = Gravity.END
          DecimalFormat(DecimalFormatCalcDigits).format(current)

        } else {

          holder.binding.calclineNumber.gravity = Gravity.END
          ""

        }

    holder.binding.calclineNumber.text =
      SpannableStringBuilder().color(Color.BLACK) { append(lineText) }

    val prefixText =
      if (position >= 0 && position < this.calcData.numberList.size) {

        // number list
        "${this.calcData.numberList.size - position}:"

      } else {

        ""

      }

    holder.binding.calclinePrefix.text =
      SpannableStringBuilder().color(Color.BLACK) { append(prefixText) }
  }

  fun updateData(calcData: CalcData, displayedDecimals: Int, separatorChar: Char) {
    this.calcData = calcData

    notifyDataSetChanged()
  }

  override fun getItemCount() = this.calcData.numberList.size + 1 // numberlist + editline
}
