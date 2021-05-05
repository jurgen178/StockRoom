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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.scale
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
const val transaction_headline_type: Int = 1
const val transaction_data_type: Int = 2

data class TransactionData
  (
  val viewType: Int,
  var date: Long,
  var symbol: String,
  var type: TransactionType,
  var account: String = "",
  var value: Double = 0.0,
  var amountStr: SpannableStringBuilder = SpannableStringBuilder(),
  var assetBoughtMap: HashMap<String, Int> = hashMapOf(),
  var assetBought: Int = 0,
  var assetSoldMap: HashMap<String, Int> = hashMapOf(),
  var assetSold: Int = 0,
  var dividendReceivedMap: HashMap<String, Int> = hashMapOf(),
  var dividendReceived: Int = 0,
)

enum class TransactionType {
  AssetBoughtType,
  AssetSoldType,
  DividendReceivedType,
  StatsType,
}

enum class TransactionSortMode {
  ByDateUp,
  ByDateDown,
  BySymbolUp,
  BySymbolDown,
  ByTypeUp,
  ByTypeDown,
  ByAccountUp,
  ByAccountDown,
  ByAmountUp,
  ByAmountDown,
}

class StockRoomTransactionsAdapter internal constructor(
  val context: Context,
  private val clickListenerSymbolLambda: (TransactionData) -> Unit
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var statsTransactionData: TransactionData = TransactionData(
    viewType = -1,
    date = -1,
    symbol = "",
    type = TransactionType.StatsType
  )
  private var transactionDataList: MutableList<TransactionData> = mutableListOf()
  private var transactionDataListCopy: List<TransactionData> = listOf()
  private var transactionSortmode = TransactionSortMode.ByDateUp

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

  class HeadlineViewHolder(
    val binding: StockroomTransactionItemBinding
  ) : BaseViewHolder<TransactionData>(binding.root) {

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

      transaction_headline_type -> {
        val binding = StockroomTransactionItemBinding.inflate(inflater, parent, false)
        HeadlineViewHolder(binding)
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

        //   %1$d x Bought%2$s\n%3$d x Sold%4$s\n%5$d x Received Dividend%6$s
        val transactionStats = SpannableStringBuilder()
          .append(current.assetBought.toString())
          .append(" x ")
          .append(context.getString(R.string.transaction_bought))
          .scale(0.8f) { append(getAccounts(current.assetBoughtMap)) }
          .append("\n")

          .append(current.assetSold.toString())
          .append(" x ")
          .append(context.getString(R.string.transaction_sold))
          .scale(0.8f) { append(getAccounts(current.assetSoldMap)) }
          .append("\n")

          .append(current.dividendReceived.toString())
          .append(" x ")
          .append(context.getString(R.string.transaction_dividendReceived))
          .scale(0.8f) { append(getAccounts(current.dividendReceivedMap)) }

        holder.binding.transactionStats.text = transactionStats
      }

      is HeadlineViewHolder -> {

        holder.binding.transactionLayout.setBackgroundColor(context.getColor(R.color.tableHeaderBackground))

        holder.binding.transactionDate.setOnClickListener {
          update(TransactionSortMode.ByDateUp, TransactionSortMode.ByDateDown)
        }
        holder.binding.transactionDate.text =
          getHeaderStr(context.getString(R.string.transaction_column_date))

        holder.binding.transactionSymbol.setOnClickListener {
          update(TransactionSortMode.BySymbolUp, TransactionSortMode.BySymbolDown)
        }
        holder.binding.transactionSymbol.text =
          getHeaderStr(context.getString(R.string.transaction_column_symbol))

        holder.binding.transactionType.setOnClickListener {
          update(TransactionSortMode.ByTypeUp, TransactionSortMode.ByTypeDown)
        }
        holder.binding.transactionType.text =
          getHeaderStr(context.getString(R.string.transaction_column_type))

        holder.binding.transactionAccount.setOnClickListener {
          update(TransactionSortMode.ByAccountUp, TransactionSortMode.ByAccountDown)
        }
        holder.binding.transactionAccount.text =
          getHeaderStr(context.getString(R.string.transaction_column_account))

        holder.binding.transactionAmount.setOnClickListener {
          update(TransactionSortMode.ByAmountUp, TransactionSortMode.ByAmountDown)
        }
        holder.binding.transactionAmount.text =
          getHeaderStr(context.getString(R.string.transaction_column_amount))

        when (transactionSortmode) {
          TransactionSortMode.ByDateUp -> updateTextviewUp(holder.binding.transactionDate)
          TransactionSortMode.ByDateDown -> updateTextviewDown(holder.binding.transactionDate)

          TransactionSortMode.BySymbolUp -> updateTextviewUp(holder.binding.transactionSymbol)
          TransactionSortMode.BySymbolDown -> updateTextviewDown(holder.binding.transactionSymbol)

          TransactionSortMode.ByTypeUp -> updateTextviewUp(holder.binding.transactionType)
          TransactionSortMode.ByTypeDown -> updateTextviewDown(holder.binding.transactionType)

          TransactionSortMode.ByAccountUp -> updateTextviewUp(holder.binding.transactionAccount)
          TransactionSortMode.ByAccountDown -> updateTextviewDown(holder.binding.transactionAccount)

          TransactionSortMode.ByAmountUp -> updateTextviewUp(holder.binding.transactionAmount)
          TransactionSortMode.ByAmountDown -> updateTextviewDown(holder.binding.transactionAmount)
        }
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

        holder.binding.transactionAccount.text = current.account

        holder.binding.transactionAmount.text = current.amountStr
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    val element: TransactionData = transactionDataList[position]
    return element.viewType
  }

  override fun getItemCount() = transactionDataList.size

  private fun getHeaderStr(text: String): SpannableStringBuilder =
    SpannableStringBuilder()
      .color(Color.WHITE) {
        bold { append(text) }
      }

  private fun updateTextviewUp(textView: TextView) {
    textView.text = textView.text.toString() + " ▲"
  }

  private fun updateTextviewDown(textView: TextView) {
    textView.text = textView.text.toString() + " ▼"
  }

  private fun getAccounts(assetMap: HashMap<String, Int>): String {
    if (assetMap.size > 1) {
      val accounts: MutableList<String> = mutableListOf()
      assetMap.toSortedMap().forEach { (account, n) ->
        val accountName = if (account.isEmpty()) {
          context.getString(R.string.standard_account)
        } else {
          account
        }

        accounts.add("$accountName (${n})")
      }

      return accounts.joinToString(
        prefix = " [",
        separator = ", ",
        postfix = "]"
      )
    }

    return ""
  }

  fun updateData(
    transactionDataList: List<TransactionData>,
    statsTransactionData: TransactionData
  ) {
    this.transactionDataListCopy = transactionDataList
    this.statsTransactionData = statsTransactionData

    update(transactionSortmode, transactionSortmode)
  }

  internal fun update(
    transactionSortmodeUp: TransactionSortMode,
    TransactionSortmodeDown: TransactionSortMode
  ) {
    this.transactionSortmode = if (this.transactionSortmode == transactionSortmodeUp) {
      TransactionSortmodeDown
    } else {
      transactionSortmodeUp
    }

    this.transactionDataList = mutableListOf(
      statsTransactionData,
      TransactionData(
        viewType = transaction_headline_type,
        date = 0L,
        symbol = "",
        type = TransactionType.StatsType
      )
    )

    this.transactionDataList.addAll(when (transactionSortmode) {
      TransactionSortMode.ByDateUp -> this.transactionDataListCopy.sortedBy { transactionData ->
        transactionData.date
      }
      TransactionSortMode.ByDateDown -> this.transactionDataListCopy.sortedByDescending { transactionData ->
        transactionData.date
      }

      TransactionSortMode.BySymbolUp -> this.transactionDataListCopy.sortedBy { transactionData ->
        transactionData.symbol
      }
      TransactionSortMode.BySymbolDown -> this.transactionDataListCopy.sortedByDescending { transactionData ->
        transactionData.symbol
      }

      TransactionSortMode.ByTypeUp -> this.transactionDataListCopy.sortedBy { transactionData ->
        transactionData.type
      }
      TransactionSortMode.ByTypeDown -> this.transactionDataListCopy.sortedByDescending { transactionData ->
        transactionData.type
      }

      TransactionSortMode.ByAccountUp -> this.transactionDataListCopy.sortedBy { transactionData ->
        transactionData.account
      }
      TransactionSortMode.ByAccountDown -> this.transactionDataListCopy.sortedByDescending { transactionData ->
        transactionData.account
      }

      TransactionSortMode.ByAmountUp -> this.transactionDataListCopy.sortedBy { transactionData ->
        transactionData.value
      }
      TransactionSortMode.ByAmountDown -> this.transactionDataListCopy.sortedByDescending { transactionData ->
        transactionData.value
      }
    }
    )

    notifyDataSetChanged()
  }
}
