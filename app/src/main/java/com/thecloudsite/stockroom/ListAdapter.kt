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

data class DebugData(
  val timeStamp: String,
  val data: String
)

class DebugListAdapter internal constructor(
  context: Context
) : RecyclerView.Adapter<DebugListAdapter.DebugDataViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = listOf<DebugData>()

  inner class DebugDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val debugItemTimeStamp: TextView = itemView.findViewById(R.id.debugItemTimeStamp)
    val debugItemData: TextView = itemView.findViewById(R.id.debugItemData)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DebugDataViewHolder {
    val itemView = inflater.inflate(R.layout.debug_item, parent, false)
    return DebugDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: DebugDataViewHolder,
    position: Int
  ) {
    val current: DebugData = data[position]

    holder.debugItemTimeStamp.text = current.timeStamp
    holder.debugItemData.text = current.data
  }

  fun updateData(debugDataList: List<DebugData>) {
    data = debugDataList

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size
}
