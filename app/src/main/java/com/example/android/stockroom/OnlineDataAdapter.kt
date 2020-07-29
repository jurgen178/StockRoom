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

package com.example.android.stockroom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

data class OnlineData(
  val desc: String,
  val text: String
)

class OnlineDataAdapter internal constructor(
  context: Context
) : RecyclerView.Adapter<OnlineDataAdapter.OnlineDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<OnlineData>()

  inner class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

  fun updateData(onlineMarketData: OnlineMarketData) {
    data.clear()

    data.add(
        OnlineData(
            desc = "regularMarketPreviousClose",
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketPreviousClose)
        )
    )
    data.add(
        OnlineData(
            desc = "regularMarketOpen",
            text = DecimalFormat("0.00##").format(onlineMarketData.regularMarketOpen)
        )
    )
    data.add(
        OnlineData(
            desc = "fiftyDayAverage",
            text = DecimalFormat("0.00##").format(onlineMarketData.fiftyDayAverage)
        )
    )
    data.add(OnlineData(desc = "fiftyTwoWeekRange", text = onlineMarketData.fiftyTwoWeekRange))
    data.add(
        OnlineData(desc = "regularMarketDayRange", text = onlineMarketData.regularMarketDayRange)
    )
    data.add(
        OnlineData(
            desc = "regularMarketVolume", text = onlineMarketData.regularMarketVolume.toString()
        )
    )
    data.add(OnlineData(desc = "marketCap", text = onlineMarketData.marketCap.toString()))
    data.add(
        OnlineData(
            desc = "forwardPE", text = DecimalFormat("0.00##").format(onlineMarketData.forwardPE)
        )
    )
    data.add(
        OnlineData(
            desc = "annualDividendRate",
            text = DecimalFormat("0.00##").format(onlineMarketData.annualDividendRate)
        )
    )
    data.add(
        OnlineData(
            desc = "annualDividendYield",
            text = "${DecimalFormat("0.00##").format(
                onlineMarketData.annualDividendYield * 100
            )}%"
        )
    )
    data.add(
        OnlineData(
            desc = "epsTrailingTwelveMonths",
            text = DecimalFormat("0.00##").format(onlineMarketData.epsTrailingTwelveMonths)
        )
    )
    data.add(
        OnlineData(
            desc = "epsForward", text = DecimalFormat("0.00##").format(onlineMarketData.epsForward)
        )
    )

    data.add(OnlineData(desc = "region", text = onlineMarketData.region))
    data.add(OnlineData(desc = "language", text = onlineMarketData.language))
    data.add(OnlineData(desc = "fullExchangeName", text = onlineMarketData.fullExchangeName))
    data.add(OnlineData(desc = "messageBoardId", text = onlineMarketData.messageBoardId))
    data.add(OnlineData(desc = "financialCurrency", text = onlineMarketData.financialCurrency))
    data.add(OnlineData(desc = "marketState", text = onlineMarketData.marketState))

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size
}
