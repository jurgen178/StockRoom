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
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.StockRoomTransactionsAdapter.BaseViewHolder
import com.thecloudsite.stockroom.databinding.StockroomTransactionStatsItemBinding
import com.thecloudsite.stockroom.databinding.StockroomTransactionItemBinding
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

const val transaction_stats_type: Int = 0
const val transaction_data_type: Int = 1

data class TransactionData
  (
  val viewType: Int,
  var date: Long,
  var symbol: String,
  var type: TransactionType,
  var data: SpannableStringBuilder = SpannableStringBuilder(),
  var assetBought: Int = 0,
  var assetSold: Int = 0,
  var dividendReceived: Int = 0,
)

enum class TransactionType {
  AssetBoughtType,
  AssetSoldType,
  DividendReceivedType,
  StatsType,
}

class StockRoomTransactionsAdapter internal constructor(
  val context: Context,
  private val clickListenerSymbolLambda: (TransactionData) -> Unit
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var transactionDataList: List<TransactionData> = listOf()

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun bindOnClickListener(
      transactionData: TransactionData,
      clickListenerLambda: (TransactionData) -> Unit
    )
  }

  class StatsViewHolder(
    val binding: StockroomTransactionStatsItemBinding
  ) : BaseViewHolder<AssetListData>(binding.root) {

    override fun bindOnClickListener(
      transactionData: TransactionData,
      clickListenerLambda: (TransactionData) -> Unit
    ) {
    }
  }

  class TransactionsViewHolder(
    val binding: StockroomTransactionItemBinding
  ) : BaseViewHolder<TransactionData>(binding.root) {

    override fun bindOnClickListener(
      transactionData: TransactionData,
      clickListenerLambda: (TransactionData) -> Unit
    ) {
      binding.transactionLayout.setOnClickListener { clickListenerLambda(transactionData) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {

    return when (viewType) {
      transaction_stats_type -> {
        val binding = StockroomTransactionStatsItemBinding.inflate(inflater, parent, false)
        StatsViewHolder(binding)
      }

      transaction_data_type -> {
        val binding = StockroomTransactionItemBinding.inflate(inflater, parent, false)
        TransactionsViewHolder(binding)
      }

      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {
    val current: TransactionData = transactionDataList[position]

    when (holder) {

      is StatsViewHolder -> {
        holder.binding.transactionLayout.setBackgroundColor(
          context.getColor(R.color.backgroundListColor)
        )

        holder.binding.transactionStats.text = context.getString(
          R.string.transaction_stats,
          current.assetBought,
          current.assetSold,
          current.dividendReceived
        )
      }

      is TransactionsViewHolder -> {

        holder.bindOnClickListener(current, clickListenerSymbolLambda)

        holder.binding.transactionLayout.setBackgroundColor(
          context.getColor(
            when (current.type) {
              TransactionType.AssetBoughtType -> {
                R.color.transactionBackgroundBought
              }
              TransactionType.AssetSoldType -> {
                R.color.transactionBackgroundSold
              }
              TransactionType.DividendReceivedType -> {
                R.color.transactionBackgroundDividend
              }
              else -> {
                R.color.backgroundListColor
              }
            }
          )
        )

        val datetime: ZonedDateTime =
          ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(current.date),
            ZoneOffset.systemDefault()
          )

        holder.binding.transactionDate.text = "${
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        } ${
          datetime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
        }"

        holder.binding.transactionSymbol.text = current.symbol
        holder.binding.transactionType.text = when (current.type) {
          TransactionType.AssetBoughtType -> {
            context.getString(R.string.transaction_bought)
          }
          TransactionType.AssetSoldType -> {
            context.getString(R.string.transaction_sold)
          }
          TransactionType.DividendReceivedType -> {
            context.getString(R.string.table_column_Dividend)
          }
          else -> {
            ""
          }
        }
        holder.binding.transactionData.text = current.data
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    val element: TransactionData = transactionDataList[position]
    return element.viewType
  }

  override fun getItemCount() = transactionDataList.size

  fun updateData(transactionDataList: List<TransactionData>) {
    this.transactionDataList = transactionDataList.sortedBy { transactionData ->
      transactionData.date
    }

    notifyDataSetChanged()
  }
}
