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
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

// Levenshtein score between candidate string and reference string
fun getLevenshteinDistance(
  candidateString: String,
  referenceString: String
): Double {

  // if strings are identical (no distance) return 0
  if (candidateString.equals(referenceString, ignoreCase = true)) {
    return 0.0
  }

  val n = candidateString.length
  val m = referenceString.length

  // If one string is empty, return the max distance 1.0
  if (n == 0 || m == 0) {
    return 1.0
  }

  // Use only two rows for the previous and current result.
  val distance = Array(2) { Array(m + 1) { 0 } }
  //int[,] distance = new int[2, m + 1];

  // Initialize the distance matrix.
  for (j in 1..m) {
    distance[0][j] = j
  }

  var currentRow = 0
  for (i in 1..n) {
    currentRow = i and 1
    distance[currentRow][0] = i
    val previousRow = currentRow xor 1

    for (j in 1..m) {
      // Check if different.
      val cr = referenceString[j - 1]
      val cc = candidateString[i - 1]
      val isEqual = cr.equals(cc, true)
      val different = if (isEqual) 0 else 1

      // Calculate the distances.
      val deleted = distance[previousRow][j] + 1
      val added = distance[currentRow][j - 1] + 1
      val replaced = distance[previousRow][j - 1] + different

      // Update with the minimal distance.
      distance[currentRow][j] = min(min(deleted, added), replaced)
    }
  }

  return distance[currentRow][m] / max(n, m).toDouble()
}

fun initSubTypeList(context: Context) {
  FilterSubTypeEnum.GreaterThanType.value = context.getString(R.string.filter_GreaterThanType)
  FilterSubTypeEnum.LessThanType.value = context.getString(R.string.filter_LessThanType)
  FilterSubTypeEnum.EqualType.value = context.getString(R.string.filter_EqualType)
  FilterSubTypeEnum.BeforeDateType.value = context.getString(R.string.filter_BeforeDateType)
  FilterSubTypeEnum.AfterDateType.value = context.getString(R.string.filter_AfterDateType)
  FilterSubTypeEnum.ContainsTextType.value = context.getString(R.string.filter_ContainsTextType)
  FilterSubTypeEnum.NotContainsTextType.value =
    context.getString(R.string.filter_NotContainsTextType)
  FilterSubTypeEnum.SimilarTextType.value = context.getString(R.string.filter_SimilarTextType)
  FilterSubTypeEnum.NotSimilarTextType.value =
    context.getString(R.string.filter_NotSimilarTextType)
  FilterSubTypeEnum.IsEmptyTextType.value = context.getString(R.string.filter_IsEmptyTextType)
  FilterSubTypeEnum.IsNotEmptyTextType.value = context.getString(R.string.filter_IsNotEmptyTextType)
  FilterSubTypeEnum.StartsWithTextType.value = context.getString(R.string.filter_StartsWithTextType)
  FilterSubTypeEnum.EndsWithTextType.value = context.getString(R.string.filter_EndsWithTextType)
  FilterSubTypeEnum.IsTextType.value = context.getString(R.string.filter_IsTextType)
  FilterSubTypeEnum.IsNotTextType.value = context.getString(R.string.filter_IsNotTextType)
  FilterSubTypeEnum.IsType.value = context.getString(R.string.filter_IsType)
  FilterSubTypeEnum.IsNotType.value = context.getString(R.string.filter_IsNotType)
  FilterSubTypeEnum.MatchRegexTextType.value = context.getString(R.string.filter_MatchRegexTextType)
  FilterSubTypeEnum.NotMatchRegexTextType.value =
    context.getString(R.string.filter_NotMatchRegexTextType)
  FilterSubTypeEnum.IsPresentType.value = context.getString(R.string.filter_IsPresentType)
  FilterSubTypeEnum.IsOnePresentType.value = context.getString(R.string.filter_IsOnePresentType)
  FilterSubTypeEnum.IsAllPresentType.value = context.getString(R.string.filter_IsAllPresentType)
  FilterSubTypeEnum.IsNotPresentType.value = context.getString(R.string.filter_IsNotPresentType)
  FilterSubTypeEnum.IsUsedType.value = context.getString(R.string.filter_IsUsedType)
  FilterSubTypeEnum.IsNotUsedType.value = context.getString(R.string.filter_IsNotUsedType)
}

fun getFilterTypeList(context: Context): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
//      .filter { type ->
//        type != FilterTypeEnum.FilterNullType
//      }
    .forEach { filter ->
      filterList.add(FilterFactory.create(filter, context).displayName)
    }

  return filterList
}

fun strToDouble(str: String): Double {
  var value: Double = 0.0
  try {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    value = numberFormat.parse(str)!!
      .toDouble()
  } catch (e: Exception) {
  }

  return value
}

fun strToInt(str: String): Int {
  var value: Int = 0
  try {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    value = numberFormat.parse(str)!!
      .toInt()
  } catch (e: Exception) {
  }

  return value
}
