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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

data class OnlineData(
  val desc: String,
  val text: String
)

class OnlineDataAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<OnlineDataAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<OnlineData>()

  class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val itemViewOnlineDataDesc: TextView = itemView.findViewById(R.id.textViewOnlineDataDesc)
    val itemViewOnlineData: TextView = itemView.findViewById(R.id.textViewOnlineData)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.onlinedataview_item, parent, false)
    return OnlineDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current: OnlineData = data[position]

    holder.itemViewOnlineDataDesc.text = current.desc
    holder.itemViewOnlineData.text = current.text
  }

  private fun formatInt(value: Long): String {
    return when {
      value >= 1000000000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000000000.0)}${context.getString(
            R.string.trillion_abbr
        )}"
      }
      value >= 1000000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000000.0)}${context.getString(
            R.string.billion_abbr
        )}"
      }
      value >= 1000000L -> {
        "${DecimalFormat("0.##").format(value / 1000000.0)}${context.getString(
            R.string.million_abbr
        )}"
      }
      else -> {
        DecimalFormat("0.##").format(value)
      }
    }
  }

  fun updateData(onlineMarketData: OnlineMarketData) {
    data.clear()

    val separatorChar: Char = DecimalFormatSymbols.getInstance()
        .decimalSeparator

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketPreviousClose),
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketPreviousClose)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketOpen),
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketOpen)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fiftyDayAverage),
            text = DecimalFormat("0.00").format(onlineMarketData.fiftyDayAverage)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fiftyTwoWeekRange),
            text = onlineMarketData.fiftyTwoWeekRange.replace('.', separatorChar)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketDayRange),
            text = onlineMarketData.regularMarketDayRange.replace('.', separatorChar)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_regularMarketVolume),
            text = formatInt(onlineMarketData.regularMarketVolume)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_marketCap),
            text = formatInt(onlineMarketData.marketCap)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_forwardPE),
            text = DecimalFormat("0.00").format(onlineMarketData.forwardPE)
        )
    )

/*
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendRate),
            text = DecimalFormat("0.00##").format(onlineMarketData.annualDividendRate)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_annualDividendYield),
            text = "${DecimalFormat("0.00##").format(
                onlineMarketData.annualDividendYield * 100.0
            )}%"
        )
    )
 */

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_epsTrailingTwelveMonths),
            text = DecimalFormat("0.00").format(onlineMarketData.epsTrailingTwelveMonths)
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_epsForward),
            text = DecimalFormat("0.00").format(onlineMarketData.epsForward)
        )
    )

    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_region),
            text = onlineMarketData.region
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_language),
            text = onlineMarketData.language
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_fullExchangeName),
            text = onlineMarketData.fullExchangeName
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_messageBoardId),
            text = onlineMarketData.messageBoardId
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_financialCurrency),
            text = onlineMarketData.financialCurrency
        )
    )
    data.add(
        OnlineData(
            desc = context.getString(R.string.onlinedata_marketState),
            text = onlineMarketData.marketState
        )
    )

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size
}
