/*
 * Copyright (C) 2017 Google Inc.
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dividend_announced_view_item.view.dividendAnnouncedLinearLayout
import kotlinx.android.synthetic.main.dividend_announced_view_item.view.textViewDividendAnnouncedDelete
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

  inner class DividendAnnouncedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdate: (Dividend) -> Unit
    ) {
      itemView.dividendAnnouncedLinearLayout.setOnClickListener { clickListenerUpdate(dividend) }
    }

    fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      clickListenerDelete: (String?, Dividend?) -> Unit
    ) {
      itemView.textViewDividendAnnouncedDelete.setOnClickListener {
        clickListenerDelete(
            symbol, dividend
        )
      }
    }

    val textViewDividendAnnouncedAmount: TextView =
      itemView.findViewById(R.id.textViewDividendAnnouncedAmount)
    val textViewDividendAnnouncedExDate: TextView =
      itemView.findViewById(R.id.textViewDividendAnnouncedExDate)
    val textViewDividendAnnouncedPayDate: TextView =
      itemView.findViewById(R.id.textViewDividendAnnouncedPayDate)
    val textViewDividendAnnouncedDelete: TextView =
      itemView.findViewById(R.id.textViewDividendAnnouncedDelete)
    val dividendAnnouncedConstraintLayout: ConstraintLayout =
      itemView.findViewById(R.id.dividendAnnouncedConstraintLayout)
    val dividendAnnouncedLinearLayout: LinearLayout =
      itemView.findViewById(R.id.dividendAnnouncedLinearLayout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DividendAnnouncedViewHolder {
    val itemView = inflater.inflate(R.layout.dividend_announced_view_item, parent, false)
    return DividendAnnouncedViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: DividendAnnouncedViewHolder,
    position: Int
  ) {
    val current: Dividend = dividendList[position]

    // First entry is headline.
    if (position == 0) {
      holder.textViewDividendAnnouncedAmount.text = context.getString(R.string.dividend)
      holder.textViewDividendAnnouncedExDate.text = context.getString(R.string.dividend_exdate)
      holder.textViewDividendAnnouncedPayDate.text = context.getString(R.string.dividend_date)
      holder.textViewDividendAnnouncedDelete.visibility = View.GONE
      holder.dividendAnnouncedConstraintLayout.setBackgroundColor(
          context.getColor(R.color.backgroundListColor)
      )

      val background = TypedValue()
      holder.dividendAnnouncedLinearLayout.setBackgroundResource(background.resourceId)
    } else {
      holder.bindUpdate(current, clickListenerUpdate)
      holder.bindDelete(null, current, clickListenerDelete)

      holder.textViewDividendAnnouncedAmount.text = DecimalFormat("0.00##").format(current.amount)
      val datetimeEx: LocalDateTime =
        LocalDateTime.ofEpochSecond(current.exdate, 0, ZoneOffset.UTC)
      holder.textViewDividendAnnouncedExDate.text =
        datetimeEx.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
      val datetimePay: LocalDateTime =
        LocalDateTime.ofEpochSecond(current.paydate, 0, ZoneOffset.UTC)
      holder.textViewDividendAnnouncedPayDate.text =
        datetimePay.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))

      holder.textViewDividendAnnouncedDelete.visibility = View.VISIBLE
      holder.dividendAnnouncedConstraintLayout.background = null

      val background = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
      holder.dividendAnnouncedLinearLayout.setBackgroundResource(background.resourceId)
    }
  }

  internal fun updateDividends(dividends: Dividends) {
    // Headline placeholder
    dividendList =
      mutableListOf(Dividend(symbol = "", amount = 0f, exdate = 0L, paydate = 0L, type = 0))
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
