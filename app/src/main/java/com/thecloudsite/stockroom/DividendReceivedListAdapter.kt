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
import com.thecloudsite.stockroom.DividendReceivedListAdapter.BaseViewHolder
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.databinding.DividendReceivedViewItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.dividendCycleStr
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

const val dividend_headline_type: Int = 0
const val dividend_item_type: Int = 1
const val dividend_summary_type: Int = 2

data class DividendListData(
  val viewType: Int,
  val deleteAll: Boolean = false,
  val summaryText: String = "",
  var dividend: Dividend
)

class DividendReceivedListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdateLambda: (Dividend) -> Unit,
  private val clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var dividendList = mutableListOf<DividendListData>()

  private var data: StockAssetsLiveData? = null
  private var dividends: Dividends? = null
  private var marketValue: Double = 0.0

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdateLambda: (Dividend) -> Unit
    )

    abstract fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      dividendList: List<DividendListData>?,
      clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
    )
  }

  class HeadlineViewHolder(
    val binding: DividendReceivedViewItemBinding
  ) : BaseViewHolder<DividendListData>(binding.root) {

    override fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdateLambda: (Dividend) -> Unit
    ) {
    }

    override fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      dividendList: List<DividendListData>?,
      clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
    ) {
    }
  }

  class DividendReceivedViewHolder(
    val binding: DividendReceivedViewItemBinding
  ) : BaseViewHolder<DividendListData>(binding.root) {
    override fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdateLambda: (Dividend) -> Unit
    ) {
      binding.dividendReceivedLinearLayout.setOnClickListener { clickListenerUpdateLambda(dividend) }
    }

    override fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      dividendList: List<DividendListData>?,
      clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
    ) {
      binding.textViewDividendReceivedDelete.setOnClickListener {
        clickListenerDeleteLambda(
          symbol, dividend, dividendList?.map { dividendListData ->
            dividendListData.dividend
          }
        )
      }
    }
  }

  class SummaryViewHolder(
    val binding: DividendReceivedViewItemBinding
  ) : BaseViewHolder<DividendListData>(binding.root) {

    override fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdateLambda: (Dividend) -> Unit
    ) {
    }

    override fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      dividendList: List<DividendListData>?,
      clickListenerDeleteLambda: (String?, Dividend?, List<Dividend>?) -> Unit
    ) {
      binding.textViewDividendReceivedDelete.setOnClickListener {
        clickListenerDeleteLambda(
          symbol, dividend, dividendList?.map { dividendListData ->
            dividendListData.dividend
          }
        )
      }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {

    return when (viewType) {
      dividend_headline_type -> {
        val binding = DividendReceivedViewItemBinding.inflate(inflater, parent, false)
        HeadlineViewHolder(binding)
      }

      dividend_item_type -> {
        val binding = DividendReceivedViewItemBinding.inflate(inflater, parent, false)
        DividendReceivedViewHolder(binding)
      }

      dividend_summary_type -> {
        val binding = DividendReceivedViewItemBinding.inflate(inflater, parent, false)
        SummaryViewHolder(binding)
      }

      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {

    val current: DividendListData = dividendList[position]

    when (holder) {

      is HeadlineViewHolder -> {

        holder.binding.textViewDividendReceivedAmount.text = context.getString(R.string.dividend)
        holder.binding.textViewDividendReceivedDate.text = context.getString(R.string.dividend_date)
        holder.binding.textViewDividendReceivedCycle.text =
          context.getString(R.string.dividend_cycle)
        holder.binding.textViewDividendReceivedAccount.text = context.getString(R.string.account)
        holder.binding.textViewDividendReceivedNote.text = context.getString(R.string.note)
        holder.binding.textViewDividendReceivedDelete.visibility = View.GONE
        holder.binding.dividendReceivedSummaryView.visibility = View.GONE
        holder.binding.dividendReceivedConstraintLayout.setBackgroundColor(
          context.getColor(R.color.backgroundListColor)
        )

        val background = TypedValue()
        holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)

      }

      is DividendReceivedViewHolder -> {

        holder.bindUpdate(current.dividend, clickListenerUpdateLambda)
        holder.bindDelete(null, current.dividend, null, clickListenerDeleteLambda)

        val dividendYield =
          if ((current.dividend.cycle == DividendCycle.Monthly.value
                || current.dividend.cycle == DividendCycle.Quarterly.value
                || current.dividend.cycle == DividendCycle.SemiAnnual.value
                || current.dividend.cycle == DividendCycle.Annual.value)
            && current.dividend.amount > 0.0 && marketValue > 0.0
          ) {
            "\n${
              DecimalFormat(DecimalFormat2Digits).format(
                (current.dividend.cycle * 100.0 * current.dividend.amount / marketValue)
              )
            }% p. a."
          } else {
            ""
          }
        holder.binding.textViewDividendReceivedAmount.text =
          DecimalFormat(DecimalFormat2To4Digits).format(current.dividend.amount) + dividendYield

        val datetime: ZonedDateTime =
          ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(current.dividend.paydate),
            ZoneOffset.systemDefault()
          )
        holder.binding.textViewDividendReceivedDate.text =
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        holder.binding.textViewDividendReceivedCycle.text =
          dividendCycleStr(current.dividend.cycle, context)
        holder.binding.textViewDividendReceivedAccount.text = current.dividend.account
        holder.binding.textViewDividendReceivedNote.text = current.dividend.note

        holder.binding.textViewDividendReceivedDelete.visibility = View.VISIBLE
        holder.binding.dividendReceivedSummaryView.visibility = View.GONE
        holder.binding.dividendReceivedConstraintLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      }

      is SummaryViewHolder -> {

        // Summary line is always black on yellow
        holder.binding.textViewDividendReceivedAmount.text =
          SpannableStringBuilder()
            .color(Color.BLACK) {
              append(DecimalFormat(DecimalFormat2To4Digits).format(current.dividend.amount))
            }
        holder.binding.textViewDividendReceivedDate.text = ""
        holder.binding.textViewDividendReceivedCycle.text = ""
        holder.binding.textViewDividendReceivedAccount.text = current.dividend.account
        holder.binding.textViewDividendReceivedNote.text = ""

        // no delete icon for empty list, headline + summaryline = 2
        if (current.deleteAll && dividendList.size > 1) {
          holder.binding.textViewDividendReceivedDelete.visibility = View.VISIBLE
          // handler for delete all
          holder.bindDelete(current.dividend.symbol, null, dividendList, clickListenerDeleteLambda)
        } else {
          holder.binding.textViewDividendReceivedDelete.visibility = View.GONE
        }

        holder.binding.dividendReceivedSummaryText.text = current.summaryText
        holder.binding.dividendReceivedSummaryView.visibility = View.VISIBLE

        holder.binding.dividendReceivedConstraintLayout.setBackgroundColor(Color.YELLOW)
        val background = TypedValue()
        holder.binding.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      }

    }
  }

  override fun getItemViewType(position: Int): Int {
    val element: DividendListData = dividendList[position]
    return element.viewType
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
          DividendListData(
            viewType = dividend_headline_type,
            dividend = Dividend(symbol = "", amount = 0.0, cycle = 0, paydate = 0L)
          )
        )

      val receivedList = dividends!!.dividends.filter { dividend ->
        dividend.type == DividendType.Received.value
      }

      dividendList.addAll(receivedList.sortedBy { dividend ->
        dividend.paydate
      }.map { dividend ->
        DividendListData(
          viewType = dividend_item_type,
          dividend = dividend
        )
      }
      )

      val dividendTotal = receivedList.sumByDouble { dividend ->
        dividend.amount
      }

      // Summary
      if (receivedList.size > 1) {
        val symbol: String = receivedList.firstOrNull()?.symbol ?: ""
        dividendList.add(
          DividendListData(
            viewType = dividend_summary_type,
            deleteAll = true,
            summaryText = context.getString(R.string.dividend_received_summary_text),
            dividend = Dividend(
              symbol = symbol,
              amount = dividendTotal,
              type = 0,
              cycle = 0,
              paydate = 0L,
              exdate = 0L
            )
          )
        )

        // Add Summary for each Account.
        val map: java.util.HashSet<String> = hashSetOf()

        receivedList.forEach { dividend ->
          map.add(dividend.account)
        }

        val assetsAccounts =
          map.map { account ->
            account
          }

        if (assetsAccounts.size > 1) {
          assetsAccounts.sorted().forEach { account ->

            // Get the dividend for the account.
            val dividendTotalAccount = receivedList.filter { dividend ->
              dividend.account == account
            }.sumByDouble { dividend ->
              dividend.amount
            }

            val accountStr = if (account.isEmpty()) {
              context.getString(R.string.standard_account)
            } else {
              account
            }

            dividendList.add(
              DividendListData(
                viewType = dividend_summary_type,
                summaryText = context.getString(
                  R.string.dividend_received_summary_account_text,
                  accountStr
                ),
                dividend = Dividend(
                  symbol = symbol,
                  amount = dividendTotalAccount,
                  account = account,
                  type = 0,
                  cycle = 0,
                  paydate = 0L,
                  exdate = 0L
                )
              )
            )
          }
        }
      }
    }

    notifyDataSetChanged()
  }

  override fun getItemCount() = dividendList.size
}
