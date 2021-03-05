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
import com.thecloudsite.stockroom.StockItem
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

enum class ZeroArgument {
  PI,
  E,
}

enum class UnaryArgument {
  SQRT,
  SQ,
  INV,
  ABS,
  SIGN,
  SIN,
  COS,
  TAN,
  ARCSIN,
  ARCCOS,
  ARCTAN,
  LN,
  E,
}

enum class BinaryArgument {
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

enum class TernaryArgument {
  ZinsMonat,
}

class CalcViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application
  private val calcRepository: CalcRepository = CalcRepository(application)
  var calcData: LiveData<CalcData> = calcRepository.calcLiveData
  var radian = 1.0
  var separatorChar = ','
  var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
  var aic: Int = 0
  var stockitemList: List<StockItem> = emptyList()

  init {
    calcRepository.updateData(calcRepository.getData())
  }

  fun function(code: String) {
    val calcData = submitEditline(calcData.value!!)

    // Split by spaces not followed by even amount of quotes so only spaces outside of quotes are replaced.
    val symbols = code
      .replace("/[*].*?[*]/".toRegex(), " ")
      .replace("//.*?(\n|$)".toRegex(), " ")
      .split("\\s+(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)".toRegex())

    var success = true
    var validArgs = true

    symbols.forEach { symbol ->
      when (symbol.toLowerCase(Locale.ROOT)) {

        // Math operations
        "sin" -> {
          validArgs = opUnary(calcData, UnaryArgument.SIN)
        }
        "cos" -> {
          validArgs = opUnary(calcData, UnaryArgument.COS)
        }
        "tan" -> {
          validArgs = opUnary(calcData, UnaryArgument.TAN)
        }
        "arcsin" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCSIN)
        }
        "arccos" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCCOS)
        }
        "arctan" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCTAN)
        }
        "ln" -> {
          validArgs = opUnary(calcData, UnaryArgument.LN)
        }
        "sqrt" -> {
          success = opUnary(calcData, UnaryArgument.SQRT)
        }
        "abs" -> {
          validArgs = opUnary(calcData, UnaryArgument.ABS)
        }

        // Stack operations
        "over" -> {
          validArgs = opBinary(calcData, BinaryArgument.OVER)
        }
        "swap" -> {
          validArgs = opBinary(calcData, BinaryArgument.SWAP)
        }
        "dup" -> {
          if (calcData.numberList.isNotEmpty()) {
            calcData.numberList.add(calcData.numberList.last())
          } else {
            // Error
            validArgs = false
          }
        }
        "rot" -> {
          if (calcData.numberList.size > 2) {
            val op3 = calcData.numberList.removeLast()
            val op2 = calcData.numberList.removeLast()
            val op1 = calcData.numberList.removeLast()
            calcData.numberList.add(op2)
            calcData.numberList.add(op3)
            calcData.numberList.add(op1)
          } else {
            // Error
            validArgs = false
          }
        }

        // Arithmetic operations
        "+" -> {
          validArgs = opBinary(calcData, BinaryArgument.ADD)
        }
        "-" -> {
          validArgs = opBinary(calcData, BinaryArgument.SUB)
        }
        "*" -> {
          validArgs = opBinary(calcData, BinaryArgument.MULT)
        }
        "/" -> {
          validArgs = opBinary(calcData, BinaryArgument.DIV)
        }
        "^" -> {
          validArgs = opBinary(calcData, BinaryArgument.POW)
        }

        // Formating and number operations
        "" -> {
          // Skip empty lines.
        }
        "pi" -> {
          opZero(ZeroArgument.PI)
        }
        "e" -> {
          opZero(ZeroArgument.E)
        }
        else -> {
          // If symbol is comment, add comment to the last entry.
          val match = "[\"'](.*?)[\"']".toRegex()
            .matchEntire(symbol)
          // is comment?
          if (match != null && match.groups.size == 2) {
            // first group (groups[0]) is entire src
            // captured comment is in groups[1]
            val desc = match.groups[1]?.value.toString()

            // Add line if list is empty to add the text to.
            if (calcData.numberList.isEmpty()) {
              calcData.numberList.add(CalcLine(desc = desc, value = Double.NaN))
            } else {
              val op = calcData.numberList.removeLast()
              op.desc = desc
              calcData.numberList.add(op)
            }
          } else
          // evaluate $$StockSymbol
            if (symbol.startsWith("$$")) {
              val stockSymbol = symbol.drop(2)
              val stockItem = stockitemList.find { stockItem ->
                stockItem.stockDBdata.symbol.equals(stockSymbol, true)
              }
              var marketPrice = Double.NaN
              if (stockItem != null) {
                marketPrice = stockItem.onlineMarketData.marketPrice
                if (marketPrice == 0.0) {
                  // Offline: use purchase price
                  val (quantity, price, commission) = com.thecloudsite.stockroom.utils.getAssets(
                    stockItem.assets
                  )
                  if (quantity != 0.0 && price != 0.0) {
                    marketPrice = price / quantity
                  }
                }
              }

              calcData.numberList.add(
                CalcLine(
                  desc = "$stockSymbol=",
                  value = marketPrice
                )
              )

            } else
            // Evaluate number
            {
              try {
                val value = numberFormat.parse(symbol)!!
                  .toDouble()

                calcData.numberList.add(CalcLine(desc = "", value = value))
              } catch (e: Exception) {
                // Error
                calcData.errorMsg = context.getString(R.string.calc_error_parsing_msg, symbol)
                success = false
              }
            }
        }
      }

      if (!success) {
        calcData.errorMsg = context.getString(R.string.calc_error_msg, symbol)
        calcRepository.updateData(calcData)

        return
      } else
        if (!validArgs) {
          calcData.errorMsg = context.getString(R.string.calc_invalid_args)
          calcRepository.updateData(calcData)

          return
        }
    }

    calcRepository.updateData(calcData)
  }

  // clipboard import/export
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
      calcData.errorMsg = ""

      calcRepository.updateData(submitEditline(calcData, desc))
    }
  }

  fun add(value: Double, desc: String) {
    val calcData = calcData.value!!

    endEdit(calcData)
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
    calcData.errorMsg = ""

    calcRepository.updateData(calcData)
  }

  fun opZero(op: ZeroArgument) {
    val calcData = submitEditline(calcData.value!!)

    endEdit(calcData)

    when (op) {
      ZeroArgument.PI -> {
        calcData.numberList.add(CalcLine(desc = "", value = Math.PI))
      }
      ZeroArgument.E -> {
        calcData.numberList.add(CalcLine(desc = "", value = Math.E))
      }
    }

    calcRepository.updateData(calcData)
  }

  fun opUnary(op: UnaryArgument): Boolean {
    val calcData = submitEditline(calcData.value!!)
    return opUnary(calcData, op)
  }

  private fun opUnary(calcData: CalcData, op: UnaryArgument): Boolean {
    val argsValid = calcData.numberList.size > 0

    if (argsValid) {
      endEdit(calcData)

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
        UnaryArgument.SQRT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.pow(0.5)))
        }
        UnaryArgument.SQ -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.pow(2)))
        }
        UnaryArgument.INV -> {
          calcData.numberList.add(CalcLine(desc = "", value = 1 / op1))
        }
        UnaryArgument.ABS -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.absoluteValue))
        }
        UnaryArgument.SIGN -> {
          calcData.numberList.add(CalcLine(desc = "", value = -op1))
        }
        UnaryArgument.SIN -> {
          calcData.numberList.add(CalcLine(desc = "", value = sin(op1 * radian)))
        }
        UnaryArgument.COS -> {
          calcData.numberList.add(CalcLine(desc = "", value = cos(op1 * radian)))
        }
        UnaryArgument.TAN -> {
          calcData.numberList.add(CalcLine(desc = "", value = tan(op1 * radian)))
        }
        UnaryArgument.ARCSIN -> {
          calcData.numberList.add(CalcLine(desc = "", value = asin(op1) / radian))
        }
        UnaryArgument.ARCCOS -> {
          calcData.numberList.add(CalcLine(desc = "", value = acos(op1) / radian))
        }
        UnaryArgument.ARCTAN -> {
          calcData.numberList.add(CalcLine(desc = "", value = atan(op1) / radian))
        }
        UnaryArgument.LN -> {
          calcData.numberList.add(CalcLine(desc = "", value = ln(op1)))
        }
        UnaryArgument.E -> {
          calcData.numberList.add(CalcLine(desc = "", value = Math.E.pow(op1)))
        }
      }
    } else {
      calcData.errorMsg = context.getString(R.string.calc_invalid_args)
    }

    calcRepository.updateData(calcData)

    return argsValid
  }

  fun opBinary(op: BinaryArgument): Boolean {
    val calcData = submitEditline(calcData.value!!)
    return opBinary(calcData, op)
  }

  private fun opBinary(calcData: CalcData, op: BinaryArgument): Boolean {

    val argsValid = calcData.numberList.size > 1
    if (argsValid) {
      endEdit(calcData)

//      // Validation
//      val d = calcData.numberList.last()
//      if (op == BinaryOperation.DIV && (d == 0.0 || d.isNaN())) {
//        return
//      }

      val op2 = calcData.numberList.removeLast()
      val op1 = calcData.numberList.removeLast()

      when (op) {
        BinaryArgument.ADD -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value + op2.value))
        }
        BinaryArgument.SUB -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value - op2.value))
        }
        BinaryArgument.MULT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value * op2.value))
        }
        BinaryArgument.DIV -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value / op2.value))
        }
        BinaryArgument.POW -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(op2.value)))
        }
        BinaryArgument.SWAP -> {
          calcData.numberList.add(CalcLine(desc = op2.desc, value = op2.value))
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
        }
        BinaryArgument.OVER -> {
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
          calcData.numberList.add(CalcLine(desc = op2.desc, value = op2.value))
          calcData.numberList.add(CalcLine(desc = op1.desc, value = op1.value))
        }
        // Percent
        BinaryArgument.PER -> {
          calcData.numberList.add(CalcLine(desc = "% ", value = op1.value * op2.value / 100))
        }
        // Percent change
        BinaryArgument.PERC -> {
          calcData.numberList.add(
            CalcLine(
              desc = "∆% ",
              value = (op2.value - op1.value) / op1.value * 100
            )
          )
        }
      }
    } else {
      calcData.errorMsg = context.getString(R.string.calc_invalid_args)
    }

    calcRepository.updateData(calcData)

    return argsValid
  }

  fun opTernary(op: TernaryArgument): Boolean {
    val calcData = submitEditline(calcData.value!!)
    return opTernary(calcData, op)
  }

  private fun opTernary(calcData: CalcData, op: TernaryArgument): Boolean {
    val argsValid = calcData.numberList.size > 2

    if (argsValid) {
      endEdit(calcData)

      val op3 = calcData.numberList.removeLast().value
      val op2 = calcData.numberList.removeLast().value
      val op1 = calcData.numberList.removeLast().value

      when (op) {
        TernaryArgument.ZinsMonat -> {
          val K = op1 // Kapital
          val P = op2 // Jahreszins
          val J = op3 // Laufzeit in Jahren
          val p = P / 12.0 / 100.0
          val M = p * K / (1.0 - (1.0 + p).pow(-J * 12.0))
          calcData.numberList.add(CalcLine(desc = "Monatsrate=", value = M))
        }
      }
    } else {
      calcData.errorMsg = context.getString(R.string.calc_invalid_args)
    }

    calcRepository.updateData(calcData)

    return argsValid
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

    calcData.errorMsg = ""
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

    endEdit(calcData)
    calcRepository.updateData(calcData)
  }

  fun submitEditline(calcData: CalcData, desc: String = ""): CalcData {
    if (calcData.editMode) {

      try {
        val value = numberFormat.parse(calcData.editline)!!
          .toDouble()

        endEdit(calcData)
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

  private fun endEdit(calcData: CalcData) {
    calcData.editMode = false
    calcData.editline = ""
    calcData.errorMsg = ""
  }

  fun updateData() {
    calcRepository.updateData()
  }

  fun updateData(data: CalcData) {

//    data.numberList.removeIf { calcLine ->
//      calcLine.desc.isEmpty() && calcLine.value.isNaN()
//    }

    calcRepository.updateData(data)
  }
}
