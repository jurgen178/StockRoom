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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.databinding.DividendAnnouncedViewItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.dividendCycleStr
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class DividendAnnouncedListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Dividend) -> Unit,
  private val clickListenerDelete: (String?, Dividend?) -> Unit
) : RecyclerView.Adapter<DividendAnnouncedListAdapter.DividendAnnouncedViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var dividendList = mutableListOf<Dividend>()

  class DividendAnnouncedViewHolder(
    val binding: DividendAnnouncedViewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdate: (Dividend) -> Unit
    ) {
      binding.dividendAnnouncedLinearLayout.setOnClickListener { clickListenerUpdate(dividend) }
    }

    fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      clickListenerDelete: (String?, Dividend?) -> Unit
    ) {
      binding.textViewDividendAnnouncedDelete.setOnClickListener {
        clickListenerDelete(
            symbol, dividend
        )
      }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DividendAnnouncedViewHolder {

    val binding = DividendAnnouncedViewItemBinding.inflate(inflater, parent, false)
    return DividendAnnouncedViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: DividendAnnouncedViewHolder,
    position: Int
  ) {
    val current: Dividend = dividendList[position]

    // First entry is headline.
    if (position == 0) {
      holder.binding.textViewDividendAnnouncedAmount.text = context.getString(R.string.dividend)
      holder.binding.textViewDividendAnnouncedPayDate.text =
        context.getString(R.string.dividend_date)
      holder.binding.textViewDividendAnnouncedExDate.text =
        context.getString(R.string.dividend_exdate)
      holder.binding.textViewDividendAnnouncedCycle.text =
        context.getString(R.string.dividend_cycle)
      holder.binding.textViewDividendAnnouncedNote.text = context.getString(R.string.note)
      holder.binding.textViewDividendAnnouncedDelete.visibility = View.GONE
      holder.binding.dividendAnnouncedConstraintLayout.setBackgroundColor(
          context.getColor(R.color.backgroundListColor)
      )

      val background = TypedValue()
      holder.binding.dividendAnnouncedLinearLayout.setBackgroundResource(background.resourceId)
    } else {
      holder.bindUpdate(current, clickListenerUpdate)
      holder.bindDelete(null, current, clickListenerDelete)

      holder.binding.textViewDividendAnnouncedAmount.text = if (current.amount > 0.0) {
        DecimalFormat(DecimalFormat2To4Digits).format(current.amount)
      } else {
        ""
      }

      val datetimePay: LocalDateTime =
        LocalDateTime.ofEpochSecond(current.paydate, 0, ZoneOffset.UTC)
      holder.binding.textViewDividendAnnouncedPayDate.text =
        datetimePay.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
      val datetimeEx: LocalDateTime =
        LocalDateTime.ofEpochSecond(current.exdate, 0, ZoneOffset.UTC)
      holder.binding.textViewDividendAnnouncedExDate.text =
        datetimeEx.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
      holder.binding.textViewDividendAnnouncedCycle.text = dividendCycleStr(current.cycle, context)
      holder.binding.textViewDividendAnnouncedNote.text = current.note

      holder.binding.textViewDividendAnnouncedDelete.visibility = View.VISIBLE
      holder.binding.dividendAnnouncedConstraintLayout.background = null

      val background = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
      holder.binding.dividendAnnouncedLinearLayout.setBackgroundResource(background.resourceId)
    }
  }

  internal fun updateDividends(dividends: Dividends) {
    // Headline placeholder
    dividendList =
      mutableListOf(
          Dividend(
              symbol = "", amount = 0.0, exdate = 0L, paydate = 0L, type = 0, cycle = 0, note = ""
          )
      )
    dividendList.addAll(dividends.dividends.filter { dividend ->
      dividend.type == DividendType.Announced.value
    }
        .sortedBy { dividend ->
          dividend.exdate
        })

    notifyDataSetChanged()
  }

  override fun getItemCount() = dividendList.size
}
