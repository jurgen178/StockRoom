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
import kotlinx.android.synthetic.main.filterview_item.view.filterDelete
import kotlinx.android.synthetic.main.filterview_item.view.filterText

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class FilterListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (IFilterType, Int) -> Unit,
  private val clickListenerDelete: (IFilterType, Int) -> Unit
) : RecyclerView.Adapter<FilterListAdapter.FilterViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var filterList = mutableListOf<IFilterType>()

  class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      filterType: IFilterType,
      index: Int,
      clickListenerUpdate: (IFilterType, Int) -> Unit
    ) {
      itemView.filterText.setOnClickListener { clickListenerUpdate(filterType, index) }
    }

    fun bindDelete(
      filterType: IFilterType,
      index: Int,
      clickListenerDelete: (IFilterType, Int) -> Unit
    ) {
      itemView.filterDelete.setOnClickListener { clickListenerDelete(filterType, index) }
    }

    val filterText: TextView = itemView.findViewById(R.id.filterText)
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
    val current: IFilterType = filterList[position]

    holder.bindUpdate(current, position, clickListenerUpdate)
    holder.bindDelete(current, position, clickListenerDelete)

    holder.filterText.text = "${current.desc} ${current.data}"
  }

  internal fun setFilter(filterList: List<IFilterType>) {
    this.filterList.clear()
    this.filterList.addAll(filterList)

    notifyDataSetChanged()
  }

  internal fun addFilter(filter: IFilterType) {
    this.filterList.add(filter)

    notifyDataSetChanged()
  }

  override fun getItemCount() = filterList.size
}
