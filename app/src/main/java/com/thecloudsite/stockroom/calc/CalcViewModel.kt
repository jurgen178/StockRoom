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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import java.text.NumberFormat
import kotlin.math.pow

enum class ArithmeticOperationBinary {
  ADD,
  SUB,
  MULT,
  DIV,
  POW,
}

enum class ArithmeticOperationUnary {
  SQR,
  INV,
}

class CalcViewModel(application: Application) : AndroidViewModel(application) {

  private val calcRepository: CalcRepository = CalcRepository(application)
  var calcData: LiveData<CalcData> = calcRepository.calcLiveData

  init {
    calcRepository.updateData(calcRepository.getData())
  }

  fun addNum(char: Char) {
    val calcData = calcData.value!!

    calcData.editMode = true
    calcData.editline += char

    calcRepository.updateData(calcData)
  }

  fun opUnary(op: ArithmeticOperationUnary) {
    val calcData = submitEditline(calcData.value!!)

    if (calcData.numberList.size > 0) {
      calcData.editMode = false
      if (op == ArithmeticOperationUnary.INV && calcData.numberList.last() == 0.0) {
        return
      }
      val op1 = calcData.numberList.removeLast()

      when (op) {
        ArithmeticOperationUnary.SQR -> {
          calcData.numberList.add(op1.pow(0.5))
        }
        ArithmeticOperationUnary.INV -> {
          calcData.numberList.add(1 / op1)
        }
      }

      calcRepository.updateData(calcData)
    }
  }

  fun opBinary(op: ArithmeticOperationBinary) {
    val calcData = submitEditline(calcData.value!!)

    if (calcData.numberList.size > 1) {
      calcData.editMode = false
      if (op == ArithmeticOperationBinary.DIV && calcData.numberList.last() == 0.0) {
        return
      }
      val op2 = calcData.numberList.removeLast()
      val op1 = calcData.numberList.removeLast()

      when (op) {
        ArithmeticOperationBinary.ADD -> {
          calcData.numberList.add(op1 + op2)
        }
        ArithmeticOperationBinary.SUB -> {
          calcData.numberList.add(op1 - op2)
        }
        ArithmeticOperationBinary.MULT -> {
          calcData.numberList.add(op1 * op2)
        }
        ArithmeticOperationBinary.DIV -> {
          calcData.numberList.add(op1 / op2)
        }
        ArithmeticOperationBinary.POW -> {
          calcData.numberList.add(op1.pow(op2))
        }
      }

      calcRepository.updateData(calcData)
    }
  }

  fun drop() {
    val calcData = calcData.value!!

    if (calcData.editMode) {
      calcData.editline = calcData.editline.dropLast(1)
    } else {
      if (calcData.numberList.size > 0) {
        calcData.numberList.removeLast()
      }
    }

    calcRepository.updateData(calcData)
  }

  fun enter() {
    val calcData1 = calcData.value!!

    val calcData = if (calcData1.editMode) {
      submitEditline(calcData.value!!)
    } else {
      if (calcData1.numberList.size > 0) {
        val op = calcData1.numberList.last()
        calcData1.numberList.add(op)
      }
      calcData1
    }

    calcRepository.updateData(calcData)
  }

  fun submitEditline(calcData: CalcData): CalcData {
    if (calcData.editMode) {
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        val value = numberFormat.parse(calcData.editline)!!
          .toDouble()

        calcData.editMode = false
        calcData.editline = ""
        calcData.numberList.add(value)

        return calcData
      } catch (e: Exception) {
      }
    }

    return calcData
  }

  fun updateData(data: CalcData) {
    calcRepository.updateData(data)
  }
}
