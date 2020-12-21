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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.GroupviewItemBinding
import java.util.Locale

data class GroupData(
  var color: Int,
  val name: String,
  val stats: Int
)

class UpdateGroupAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (GroupData) -> Unit,
  private val clickListenerDelete: (GroupData) -> Unit
) : RecyclerView.Adapter<UpdateGroupAdapter.UpdateGroupViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var groupList: List<Group> = emptyList()
  private var stockDBdataList: List<StockDBdata> = emptyList()
  private var groupDatalist: List<GroupData> = emptyList()

  class UpdateGroupViewHolder(
    val binding: GroupviewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      group: GroupData,
      clickListenerUpdate: (GroupData) -> Unit
    ) {
      binding.groupItemName.setOnClickListener { clickListenerUpdate(group) }
    }

    fun bindDelete(
      group: GroupData,
      clickListenerDelete: (GroupData) -> Unit
    ) {
      binding.textViewGroupDelete.setOnClickListener { clickListenerDelete(group) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): UpdateGroupViewHolder {

    val binding = GroupviewItemBinding.inflate(inflater, parent, false)
    return UpdateGroupViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: UpdateGroupViewHolder,
    position: Int
  ) {
    val current: GroupData = groupDatalist[position]

    holder.bindUpdate(current, clickListenerUpdate)
    holder.bindDelete(current, clickListenerDelete)

    holder.binding.groupItemName.text = current.name
    holder.binding.groupItemStats.text = "[${current.stats}]"

    var color = current.color
    if (color == 0) {
      color = context.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(holder.binding.groupItemGroup, color)

    val background = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
    holder.binding.groupItemName.setBackgroundResource(background.resourceId)
  }

  private fun mergeData() {
    groupDatalist = groupList.map { group ->
      val stats = stockDBdataList.count { item ->
        item.groupColor == group.color
      }
      GroupData(color = group.color, name = group.name, stats = stats)
    }
  }

  internal fun addGroups(groups: List<Group>) {
    groupList = groups.sortedBy { group ->
      group.name.toLowerCase(Locale.ROOT)
    }

    mergeData()
    notifyDataSetChanged()
  }

  fun updateData(items: List<StockDBdata>) {
    stockDBdataList = items

    mergeData()
    notifyDataSetChanged()
  }

  override fun getItemCount() = groupList.size
}
