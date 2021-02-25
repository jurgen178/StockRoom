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
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.databinding.DividendReceivedViewItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.dividendCycleStr
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class DividendReceivedListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdateLambda: (Dividend) -> Unit,
  private val clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
) : RecyclerView.Adapter<DividendReceivedListAdapter.DividendReceivedViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var dividendList = mutableListOf<Dividend>()

  private var data: StockAssetsLiveData? = null
  private var dividends: Dividends? = null
  private var marketValue: Double = 0.0

  class DividendReceivedViewHolder(
    val binding: DividendReceivedViewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdateLambda: (Dividend) -> Unit
    ) {
      binding.dividendReceivedLinearLayout.setOnClickListener { clickListenerUpdateLambda(dividend) }
    }

    fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      dividendList: List<Dividend>?,
      clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
    ) {
      binding.textViewDividendReceivedDelete.setOnClickListener {
        clickListenerDeleteLambda(
          symbol, dividend, dividendList
        )
      }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DividendReceivedViewHolder {

    val binding = DividendReceivedViewItemBinding.inflate(inflater, parent, false)
    return DividendReceivedViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: DividendReceivedViewHolder,
    position: Int
  ) {
    val current: Dividend = dividendList[position]

    // First entry is headline.
    if (position == 0) {
      holder.binding.textViewDividendReceivedAmount.text = context.getString(R.string.dividend)
      holder.binding.textViewDividendReceivedDate.text = context.getString(R.string.dividend_date)
      holder.binding.textViewDividendReceivedCycle.text = context.getString(R.string.dividend_cycle)
      holder.binding.textViewDividendReceivedNote.text = context.getString(R.string.note)
      holder.binding.textViewDividendReceivedDelete.visibility = View.GONE
      holder.binding.dividendReceivedSummaryView.visibility = View.GONE
      holder.binding.dividendReceivedConstraintLayout.setBackgroundColor(
        context.getColor(R.color.backgroundListColor)
      )

      val background = TypedValue()
      holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
    } else {
      // Last entry is summary.
      if (position == dividendList.size - 1) {
        // handler for delete all
        holder.bindDelete(current.symbol, null, dividendList, clickListenerDeleteLambda)

        // Summary line is always black on yellow
        holder.binding.textViewDividendReceivedAmount.text =
          SpannableStringBuilder()
            .color(Color.BLACK) {
              append(DecimalFormat(DecimalFormat2To4Digits).format(current.amount))
            }
        holder.binding.textViewDividendReceivedDate.text = ""
        holder.binding.textViewDividendReceivedCycle.text = ""
        holder.binding.textViewDividendReceivedNote.text = ""

        // no delete icon for empty list, headline + summaryline = 2
        if (dividendList.size <= 2) {
          holder.binding.textViewDividendReceivedDelete.visibility = View.GONE
        } else {
          holder.binding.textViewDividendReceivedDelete.visibility = View.VISIBLE
        }

        holder.binding.dividendReceivedSummaryView.visibility = View.VISIBLE
        holder.binding.dividendReceivedConstraintLayout.setBackgroundColor(Color.YELLOW)

        val background = TypedValue()
        holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      } else {
        holder.bindUpdate(current, clickListenerUpdateLambda)
        holder.bindDelete(null, current, null, clickListenerDeleteLambda)

        val dividendYield =
          if ((current.cycle == DividendCycle.Monthly.value
                || current.cycle == DividendCycle.Quarterly.value
                || current.cycle == DividendCycle.SemiAnnual.value
                || current.cycle == DividendCycle.Annual.value)
            && current.amount > 0.0 && marketValue > 0.0
          ) {
            "\n${
              DecimalFormat(DecimalFormat2Digits).format(
                (current.cycle * 100.0 * current.amount / marketValue)
              )
            }% p. a."
          } else {
            ""
          }
        holder.binding.textViewDividendReceivedAmount.text =
          DecimalFormat(DecimalFormat2To4Digits).format(current.amount) + dividendYield

        val datetime: ZonedDateTime =
          ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.paydate), ZonedDateTime.now().zone)
        holder.binding.textViewDividendReceivedDate.text =
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        holder.binding.textViewDividendReceivedCycle.text = dividendCycleStr(current.cycle, context)
        holder.binding.textViewDividendReceivedNote.text = current.note

        holder.binding.textViewDividendReceivedDelete.visibility = View.VISIBLE
        holder.binding.dividendReceivedSummaryView.visibility = View.GONE
        holder.binding.dividendReceivedConstraintLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      }
    }
  }

  internal fun updateAssetData(_data: StockAssetsLiveData) {
    data = _data
    updateData()
  }

  internal fun updateDividends(_dividends: Dividends) {
    dividends = _dividends
    updateData()
  }

  internal fun updateData() {
    if (data != null) {
      marketValue = if (data!!.assets != null) {
        val (totalQuantity, totalPrice, totalCommission) = getAssets(data!!.assets?.assets)

//        val totalQuantity = data!!.assets?.assets?.sumByDouble {
//          it.shares
//        } ?: 0.0

        if (totalQuantity > 0.0) {
          val marketPrice: Double = data!!.onlineMarketData?.marketPrice ?: 0.0
          totalQuantity * marketPrice
        } else {
          0.0
        }
      } else {
        0.0
      }
    }

    if (dividends != null) {
      // Headline placeholder
      dividendList =
        mutableListOf(
          Dividend(symbol = "", amount = 0.0, exdate = 0L, paydate = 0L, type = 0, cycle = 0)
        )
      dividendList.addAll(dividends!!.dividends.filter { dividend ->
        dividend.type == DividendType.Received.value
      }
        .sortedBy { dividend ->
          dividend.paydate
        })

      val dividendTotal = dividendList.sumByDouble {
        it.amount
      }

      // Summary
      if (dividendList.size > 1) {
        val symbol: String = dividendList.firstOrNull()?.symbol ?: ""
        dividendList.add(
          Dividend(
            symbol = symbol,
            amount = dividendTotal,
            type = 0,
            cycle = 0,
            paydate = 0L,
            exdate = 0L
          )
        )
      }
    }

    notifyDataSetChanged()
  }

  override fun getItemCount() = dividendList.size
}
