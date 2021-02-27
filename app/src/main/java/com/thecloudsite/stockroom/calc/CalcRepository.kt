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

package com.thecloudsite.stockroom.calc

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class CalcData
  (
  var numberList: MutableList<Double> = mutableListOf(),
  var editMode: Boolean = false,
)

class CalcRepository(val context: Context) {

  val calcMutableLiveData = MutableLiveData<CalcData>()
  val calcLiveData: LiveData<CalcData>
    get() = calcMutableLiveData

  fun updateData(data: CalcData) {
    calcMutableLiveData.postValue(data)
  }
}
