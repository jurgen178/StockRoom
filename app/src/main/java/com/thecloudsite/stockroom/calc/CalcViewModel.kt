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

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.thecloudsite.stockroom.R
import java.text.NumberFormat
import kotlin.math.pow

enum class BinaryOperation {
  ADD,
  SUB,
  MULT,
  DIV,
  POW,
  SWAP,
  OVER,
  PER,  // Percent
  PERC, // Percent change
}

enum class UnaryOperation {
  SQR,
  SQ,
  INV,
  SIGN,
}

class CalcViewModel(application: Application) : AndroidViewModel(application) {

  private val calcRepository: CalcRepository = CalcRepository(application)
  var calcData: LiveData<CalcData> = calcRepository.calcLiveData
  var separatorChar = ','
  var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
  var aic: Int = 0

  init {
    calcRepository.updateData(calcRepository.getData())
  }

  // clipboard export/import text is using the . decimal point
  fun getText(): String {
    val calcData = calcData.value!!
    return if (calcData.numberList.isNotEmpty()) {
      // calcData.numberList.last().toString() converts to E-notation
      // calcData.numberList.last().toBigDecimal().toPlainString()
      numberFormat.format(calcData.numberList.last().value)
    } else {
      ""
    }
  }

  fun setText(text: String?, desc: String) {
    if (text != null && text.isNotEmpty()) {
      val calcData = calcData.value!!

      calcData.editMode = true
      calcData.editline = text

      calcRepository.updateData(submitEditline(calcData, desc))
    }
  }

  fun add(value: Double, desc: String) {
    val calcData = calcData.value!!

    calcData.editMode = false
    calcData.editline = ""
    calcData.numberList.add(CalcLine(desc = desc, value = value))

    calcRepository.updateData(calcData)
  }

  fun addNum(char: Char, context: Context? = null) {
    val calcData = calcData.value!!

    if (char == separatorChar) {
      aic++

      if (context != null && aic == 7) {
        AlertDialog.Builder(context)
          // https://convertcodes.com/unicode-converter-encode-decode-utf/
          .setTitle(
            "\u0041\u0049\u0020\u003d\u0020\u0041\u006c\u0069\u0065\u006e\u0020\u0049\u006e\u0074\u0065\u006c\u006c\u0069\u0067\u0065\u006e\u0063\u0065"
          )
          .setMessage(
            "\u0057\u0061\u0074\u0063\u0068\u0020\u006f\u0075\u0074\u002e"
          )
          .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
          .show()
      }

      // Validation:
      // Only one separator char allowed.
      if (calcData.editline.contains(char)) {
        return
      }
    } else {
      aic = 0
    }

    calcData.editMode = true
    calcData.editline += char

    calcRepository.updateData(calcData)
  }

  fun opUnary(op: UnaryOperation) {
    val calcData = submitEditline(calcData.value!!)

    if (calcData.numberList.size > 0) {
      calcData.editMode = false

//      // Validation
//      val d = calcData.numberList.last()
//      if (op == UnaryOperation.INV && (d == 0.0 || d.isNaN())) {
//        return
//      }
//      if (op == UnaryOperation.SQR && d < 0.0) {
//        return
//      }

      val op1 = calcData.numberList.removeLast().value

      when (op) {
        UnaryOperation.SQR -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.pow(0.5)))
        }
        UnaryOperation.SQ -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.pow(2)))
        }
        UnaryOperation.INV -> {
          calcData.numberList.add(CalcLine(desc = "", value = 1 / op1))
        }
        UnaryOperation.SIGN -> {
          calcData.numberList.add(CalcLine(desc = "", value = -op1))
        }
      }

      calcRepository.updateData(calcData)
    }
  }

  fun opBinary(op: BinaryOperation) {
    val calcData = submitEditline(calcData.value!!)

    if (calcData.numberList.size > 1) {
      calcData.editMode = false

//      // Validation
//      val d = calcData.numberList.last()
//      if (op == BinaryOperation.DIV && (d == 0.0 || d.isNaN())) {
//        return
//      }

      val op2 = calcData.numberList.removeLast()
      val op1 = calcData.numberList.removeLast()

      when (op) {
        BinaryOperation.ADD -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value + op2.value))
        }
        BinaryOperation.SUB -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value - op2.value))
        }
        BinaryOperation.MULT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value * op2.value))
        }
        BinaryOperation.DIV -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value / op2.value))
        }
        BinaryOperation.POW -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(op2.value)))
        }
        BinaryOperation.SWAP -> {
          calcData.numberList.add(CalcLine(desc = op2.desc, value = op2.value))
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
        }
        BinaryOperation.OVER -> {
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
          calcData.numberList.add(CalcLine(desc = op2.desc, value = op2.value))
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
        }
        // Percent
        BinaryOperation.PER -> {
          calcData.numberList.add(CalcLine(desc = "% ", value = op1.value * op2.value / 100))
        }
        // Percent change
        BinaryOperation.PERC -> {
          calcData.numberList.add(
            CalcLine(
              desc = "∆% ",
              value = (op2.value - op1.value) / op1.value * 100
            )
          )
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

  fun submitEditline(calcData: CalcData, desc: String = ""): CalcData {
    if (calcData.editMode) {

      try {
        val value = numberFormat.parse(calcData.editline)!!
          .toDouble()

        calcData.editMode = false
        calcData.editline = ""
        calcData.numberList.add(CalcLine(desc = desc, value = value))

        return calcData
      } catch (e: Exception) {
      }
    }

//    // Remove all descriptions.
//    calcData.numberList.forEach { calcLine ->
//      calcLine.desc = ""
//    }

    aic = 0
    return calcData
  }

  fun updateData() {
    calcRepository.updateData()
  }

  fun updateData(data: CalcData) {
    calcRepository.updateData(data)
  }
}
