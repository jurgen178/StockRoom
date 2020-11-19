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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Asset
import kotlinx.android.synthetic.main.filterview_item.view.filterDelete
import kotlinx.android.synthetic.main.filterview_item.view.filterText

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class FilterListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Asset) -> Unit,
  private val clickListenerDelete: (String?, Asset?) -> Unit
) : RecyclerView.Adapter<FilterListAdapter.FilterViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var assetList = mutableListOf<Asset>()

  class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      asset: Asset,
      clickListenerUpdate: (Asset) -> Unit
    ) {
      itemView.filterText.setOnClickListener { clickListenerUpdate(asset) }
    }

    fun bindDelete(
      symbol: String?,
      asset: Asset?,
      clickListenerDelete: (String?, Asset?) -> Unit
    ) {
      itemView.filterDelete.setOnClickListener { clickListenerDelete(symbol, asset) }
    }

    val filterText: TextView = itemView.findViewById(R.id.filterText)
    val filterDelete: TextView = itemView.findViewById(R.id.filterDelete)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): FilterViewHolder {
    val itemView = inflater.inflate(R.layout.filterview_item, parent, false)
    return FilterViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: FilterViewHolder,
    position: Int
  ) {
    val current: Asset = assetList[position]

    // Asset items
    holder.bindUpdate(current, clickListenerUpdate)
    holder.bindDelete(null, current, clickListenerDelete)

    holder.filterText.text = "test"
  }

  internal fun updateAssets(assets: List<Asset>) {
    assetList = assets as MutableList<Asset>

    notifyDataSetChanged()
  }

  override fun getItemCount() = assetList.size
}
