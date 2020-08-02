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
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL
import java.time.format.FormatStyle.MEDIUM

data class NewsData(
  val title: String,
  val text: String,
  val date: Long,
  val link: String
)

class NewsAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var newsDataList = listOf<NewsData>()

  inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val newsItemTitle: TextView = itemView.findViewById(R.id.newsItemTitle)
    val newsItemDate: TextView = itemView.findViewById(R.id.newsItemDate)
    val newsItemLink: TextView = itemView.findViewById(R.id.newsItemLink)
    val newsItemPreviewText: TextView = itemView.findViewById(R.id.newsItemPreviewText)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): NewsViewHolder {
    val itemView = inflater.inflate(R.layout.newsview_item, parent, false)
    return NewsViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: NewsViewHolder,
    position: Int
  ) {
    val current: NewsData = newsDataList[position]

    holder.newsItemTitle.text = current.title

    val localDateTime = LocalDateTime.ofEpochSecond(current.date, 0, ZoneOffset.UTC)
    val dateStr = localDateTime.format(DateTimeFormatter.ofLocalizedDate(FULL))
    val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
    holder.newsItemDate.text = context.getString(R.string.news_date_time, dateStr, timeStr)

    holder.newsItemLink.text = current.link
    holder.newsItemPreviewText.text =
      HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    // Make links clickable.
    holder.newsItemPreviewText.movementMethod = LinkMovementMethod.getInstance()
  }

  fun updateData(newsDataList: List<NewsData>) {
    this.newsDataList = newsDataList

    notifyDataSetChanged()
  }

  override fun getItemCount() = newsDataList.size
}
