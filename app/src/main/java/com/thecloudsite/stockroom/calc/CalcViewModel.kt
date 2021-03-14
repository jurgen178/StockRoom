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
import com.thecloudsite.stockroom.getName
import com.thecloudsite.stockroom.utils.frac
import java.text.NumberFormat
import java.util.Locale
import java.util.Stack
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.text.RegexOption.IGNORE_CASE

enum class VariableArguments {
  PICK,
  ROLL,
  SUM,
  VAR,  // Varianz
  VALIDATE,
}

enum class ZeroArgument {
  DEPTH,
  CLEAR,
  PI,
  E,
}

enum class UnaryArgument {
  DROP,
  DUP,
  SQRT,
  SQ,
  INV,
  ABS,
  SIGN,
  SIN,
  SINH,
  COS,
  COSH,
  TAN,
  TANH,
  ARCSIN,
  ARCSINH,
  ARCCOS,
  ARCCOSH,
  ARCTAN,
  ARCTANH,
  INT,    // Integer part
  ROUND,  // Round to nearest int
  ROUND2, // Round to two digits
  ROUND4, // Round to four digits
  FRAC,
  TOSTR,  // toStr
  LN,
  EX,
  LOG,
  ZX,
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
  ROT,
  ZinsMonat,
}

enum class QuadArgument {
  IFEQ, // if equal
  IFGE, // if greater or equal than
  IFGT, // if greater than
  IFLE, // if less or equal then
  IFLT, // if less then
}

val wordListRegex =
  "do|goto|if|rcl|sto|while|validate|clear|depth|drop|dup|over|swap|rot|pick|roll|sin|cos|tan|arcsin|arccos|arctan|sinh|cosh|tanh|arcsinh|arccosh|arctanh|ln|log|sq|sqrt|pow|per|perc|inv|abs|int|round|round2|round4|frac|tostr|sum|var|pi|p|e".toRegex()

class CalcViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application
  private val calcRepository: CalcRepository = CalcRepository(application)
  private var aic: Int = 0
  private val varMap: MutableMap<String, CalcLine> = mutableMapOf()
  var symbol: String = ""
  var shift = false
  var calcData: LiveData<CalcData> = calcRepository.calcLiveData
  var radian = 1.0
  var separatorChar = ','
  var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
  var stockitemList: List<StockItem> = emptyList()

  init {
    calcRepository.updateData(calcRepository.getData())
  }

  private fun getRegexOneGroup(text: String, regex: Regex): String? {
    val match = regex.matchEntire(text)
    if (match != null
      && match.groups.size == 2
      && match.groups[1] != null
    ) {
      // first group (groups[0]) is entire src
      // first capture is in groups[1]
      return match.groups[1]!!.value
    }

    return null
  }

  private fun getRegexTwoGroups(text: String, regex: Regex): Pair<String, String>? {
    val match = regex.matchEntire(text)
    if (match != null
      && match.groups.size == 3
      && match.groups[1] != null
      && match.groups[2] != null
    ) {
      // first group (groups[0]) is entire src
      // first capture is in groups[1]
      // second capture is in groups[2]
      return Pair(match.groups[1]!!.value, match.groups[2]!!.value)
    }

    return null
  }

  fun function(code: String) {
    val calcData = submitEditline(calcData.value!!)

    endEdit(calcData)

    // Split by spaces not followed by even amount of quotes so only spaces outside of quotes are replaced.
    val words = code
      // [\s\S] = . + \n
      //.replace("/[*][\\s\\S]*?[*]/".toRegex(), " ")

      // (?s) = dotall = . + \n
      .replace("(?s)/[*].*?[*]/".toRegex(), " ")

      //.replace("(?s): [a-z]+ .*? ;".toRegex(IGNORE_CASE), " ")

      //.replace("//.*?(\n|$)".toRegex(), " ")

      // multiline (?m) accept the anchors ^ and $ to match at the start and end of each line
      // (otherwise they only match at the start/end of the entire string)
      .replace("(?m)//.*?$".toRegex(), " ")

      .split("\\s+(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)".toRegex())

    var success = true
    var validArgs = true
    var checkLoop = true

    // Label has optional 'do' (do.label1 instead of .label1) at the beginning
    // for better readability when used for while loop.
    // ?: = non capturing group
    // (?s) = dotall = . + \n
    val labelRegex = "^(?:do)?[.](.+?)$".toRegex(IGNORE_CASE)
    val whileRegex = "^while[.](\\w+)[.](.+?)$".toRegex(IGNORE_CASE)
    val gotoRegex = "^goto[.](.+?)$".toRegex(IGNORE_CASE)
    val stoRegex = "^sto[.](.+)$".toRegex(IGNORE_CASE)
    val rclRegex = "^rcl[.](.+)$".toRegex(IGNORE_CASE)
    val commentRegex = "(?s)^[\"'](.+?)[\"']$".toRegex()

    // Stores the index of the labels.
    val labelMap: MutableMap<String, Int> = mutableMapOf()

    // validate and store all .labels
    words.forEachIndexed { index, word ->
      val labelMatch = getRegexOneGroup(word, labelRegex)
      // is label?
      if (labelMatch != null) {
        val label = labelMatch.toLowerCase(Locale.ROOT)

        if (labelMap.containsKey(label)) {
          calcData.errorMsg = context.getString(R.string.calc_duplicate_label, labelMatch)
          calcRepository.updateData(calcData)

          // duplicate label, end loop
          return
        } else {
          // Store label
          labelMap[label] = index
        }
      } else {
        when (word.toLowerCase(Locale.ROOT)) {
          // disable loop check
          ":loop" -> {
            checkLoop = false
          }
        }
      }
    }

    // validate and store all while.labels
    val whileMap: MutableMap<String, Int> = mutableMapOf()
    words.forEachIndexed { index, word ->
      val whileMatch =
        getRegexTwoGroups(word, whileRegex)
      // is while?
      if (whileMatch != null) {
        // captured compare is in first
        // captured label is in second
        val compare = whileMatch.first
        val labelStr = whileMatch.second
        val label = labelStr.toLowerCase(Locale.ROOT)

        if (!compare.matches("eq|le|lt|ge|gt".toRegex())) {
          calcData.errorMsg = context.getString(R.string.calc_unknown_while_condition, compare)
          calcRepository.updateData(calcData)

          // label missing, end loop
          return
        }

        if (whileMap.containsKey(label)) {
          calcData.errorMsg = context.getString(R.string.calc_duplicate_while_label, labelStr)
          calcRepository.updateData(calcData)

          // duplicate label, end loop
          return
        } else {
          // Store label
          whileMap[label] = index
        }

        if (!labelMap.containsKey(label)) {
          calcData.errorMsg = context.getString(R.string.calc_missing_while_label, labelStr)
          calcRepository.updateData(calcData)

          // label missing, end loop
          return
        }
      }
    }

    // validate and store all definitions
    // : name ... ;
    val definitionMap: MutableMap<String, Int> = mutableMapOf()
    var k = 0
    while (k < words.size) {
      // Search begin of definition.
      if (words[k] == ":") {
        k++
        var endIndex = -1
        var name = ""
        if (k < words.size) {
          name = words[k]
          k++
        }
        val startIndex = k
        // Search end of definition.
        while (k < words.size) {
          if (words[k] == ";") {
            endIndex = k
            break
          }
          k++
        }

        // Add definition name and start index to definition
        if (endIndex > 0) {
          when {
            wordListRegex.matches(name) -> {
              calcData.errorMsg = context.getString(R.string.calc_definition_is_keyword, name)
              calcRepository.updateData(calcData)

              // already exists, end loop
              return
            }
            definitionMap.containsKey(name) -> {
              calcData.errorMsg = context.getString(R.string.calc_definition_entry_exists, name)
              calcRepository.updateData(calcData)

              // already exists, end loop
              return
            }
            else -> {
              definitionMap[name] = startIndex
            }
          }
        } else {
          calcData.errorMsg = context.getString(R.string.calc_incomplete_definition, name)
          calcRepository.updateData(calcData)

          // label missing, end loop
          return
        }
      }

      k++
    }

    val returnStack = Stack<Int>()
    var loopCounter: Int = 0
    var i: Int = 0
    while (i < words.size) {

      val word = words[i++]

      // is label?
      if (getRegexOneGroup(word, labelRegex) != null) {
        // skip to next word
        continue
      }

      // Check for endless loop.
      if (checkLoop && (loopCounter > 10000 || calcData.numberList.size >= 1000)) {
        calcData.errorMsg = context.getString(R.string.calc_endless_loop)
        calcRepository.updateData(calcData)
        return
      }

      // While loop
      // while.[compare].[label]
      // while.gt.label1
      val whileMatch =
        getRegexTwoGroups(word, whileRegex)
      // is while?
      if (whileMatch != null) {
        loopCounter++

        // captured compare is in first
        // captured label is in second
        val compare = whileMatch.first
        val labelStr = whileMatch.second
        val label = labelStr.toLowerCase(Locale.ROOT)

        // already checked in validation, kept as runtime test-case
        if (!labelMap.containsKey(label)) {
          calcData.errorMsg = context.getString(R.string.calc_missing_while_label, labelStr)
          calcRepository.updateData(calcData)

          // label missing, end loop
          return
        }

        val argsValid = calcData.numberList.size >= 2
        if (argsValid) {

          // 2: op2
          // 1: op1
          val op1 = calcData.numberList.removeLast()
          val op2 = calcData.numberList.removeLast()

          when (compare.toLowerCase(Locale.ROOT)) {

            "ge" -> {
              if (op2.value >= op1.value) {
                // jump to label
                i = labelMap[label]!!
              }
            }
            "gt" -> {
              if (op2.value > op1.value) {
                // jump to label
                i = labelMap[label]!!
              }
            }
            "le" -> {
              if (op2.value <= op1.value) {
                // jump to label
                i = labelMap[label]!!
              }
            }
            "lt" -> {
              if (op2.value < op1.value) {
                // jump to label
                i = labelMap[label]!!
              }
            }
            "eq" -> {
              if (op2.value == op1.value) {
                // jump to label
                i = labelMap[label]!!
              }
            }
            else -> {
              // already checked in validation, kept as runtime test-case
              calcData.errorMsg = context.getString(R.string.calc_unknown_while_condition, compare)
              calcRepository.updateData(calcData)

              // label missing, end loop
              return
            }
          }

          continue

        } else {
          calcData.errorMsg = context.getString(R.string.calc_invalid_while_args)
          calcRepository.updateData(calcData)

          // invalid args, end instruction
          return
        }
      }

      // Goto
      // goto.[label]
      val gotoMatch =
        getRegexOneGroup(word, gotoRegex)
      if (gotoMatch != null) {
        loopCounter++
        val label = gotoMatch.toLowerCase(Locale.ROOT)

        if (!labelMap.containsKey(label)) {
          calcData.errorMsg = context.getString(R.string.calc_missing_label, gotoMatch)
          calcRepository.updateData(calcData)

          // label missing, end instruction
          return
        }

        // jump to label
        i = labelMap[label]!!
        continue
      }

      val wordLwr = word.toLowerCase(Locale.ROOT)
      if (definitionMap.containsKey(wordLwr)) {
        loopCounter++

        returnStack.push(i)
        i = definitionMap[wordLwr]!!

        continue
      }

      // skip definitions
      if (word == ":") {
        loopCounter++

        do {
          i++
        } while (i < words.size && words[i] != ";")
        // skip ;
        i++

        continue
      }

      // Definition function was called, return to the calling word.
      if (word == ";") {
        loopCounter++

        if (returnStack.isNotEmpty()) {
          i = returnStack.pop()
        }

        continue
      }

      // process words
      when (wordLwr) {

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
        "sinh" -> {
          validArgs = opUnary(calcData, UnaryArgument.SINH)
        }
        "cosh" -> {
          validArgs = opUnary(calcData, UnaryArgument.COSH)
        }
        "tanh" -> {
          validArgs = opUnary(calcData, UnaryArgument.TANH)
        }
        "arcsinh" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCSINH)
        }
        "arccosh" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCCOSH)
        }
        "arctanh" -> {
          validArgs = opUnary(calcData, UnaryArgument.ARCTANH)
        }
        "ln" -> {
          validArgs = opUnary(calcData, UnaryArgument.LN)
        }
        "log" -> {
          validArgs = opUnary(calcData, UnaryArgument.LOG)
        }
        "sq" -> {
          success = opUnary(calcData, UnaryArgument.SQ)
        }
        "sqrt" -> {
          success = opUnary(calcData, UnaryArgument.SQRT)
        }
        "inv" -> {
          validArgs = opUnary(calcData, UnaryArgument.INV)
        }
        "abs" -> {
          validArgs = opUnary(calcData, UnaryArgument.ABS)
        }
        "int" -> {
          validArgs = opUnary(calcData, UnaryArgument.INT)
        }
        "round" -> {
          validArgs = opUnary(calcData, UnaryArgument.ROUND)
        }
        "round2" -> {
          validArgs = opUnary(calcData, UnaryArgument.ROUND2)
        }
        "round4" -> {
          validArgs = opUnary(calcData, UnaryArgument.ROUND4)
        }
        "frac" -> {
          validArgs = opUnary(calcData, UnaryArgument.FRAC)
        }
        "tostr" -> {
          validArgs = opUnary(calcData, UnaryArgument.TOSTR)
        }
        "sum" -> {
          validArgs = opVarArg(calcData, VariableArguments.SUM)
        }
        "var" -> {
          validArgs = opVarArg(calcData, VariableArguments.VAR)
        }
        "per" -> {
          validArgs = opBinary(calcData, BinaryArgument.PER)
        }
        "perc" -> {
          validArgs = opBinary(calcData, BinaryArgument.PERC)
        }

        // Stack operations
        "clear" -> {
          opZero(calcData, ZeroArgument.CLEAR)
        }
        "depth" -> {
          opZero(calcData, ZeroArgument.DEPTH)
        }
        "drop" -> {
          validArgs = opUnary(calcData, UnaryArgument.DROP)
        }
        "dup" -> {
          validArgs = opUnary(calcData, UnaryArgument.DUP)
        }
        "over" -> {
          validArgs = opBinary(calcData, BinaryArgument.OVER)
        }
        "swap" -> {
          validArgs = opBinary(calcData, BinaryArgument.SWAP)
        }
        "rot" -> {
          validArgs = opTernary(calcData, TernaryArgument.ROT)
        }
        "pick" -> {
          validArgs = opVarArg(calcData, VariableArguments.PICK)
        }
        "roll" -> {
          validArgs = opVarArg(calcData, VariableArguments.ROLL)
        }

        // Conditional operations
        "if.eq" -> {
          validArgs = opQuad(calcData, QuadArgument.IFEQ)
        }
        "if.ge" -> {
          validArgs = opQuad(calcData, QuadArgument.IFGE)
        }
        "if.gt" -> {
          validArgs = opQuad(calcData, QuadArgument.IFGT)
        }
        "if.le" -> {
          validArgs = opQuad(calcData, QuadArgument.IFLE)
        }
        "if.lt" -> {
          validArgs = opQuad(calcData, QuadArgument.IFLT)
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
        "^", "pow" -> {
          validArgs = opBinary(calcData, BinaryArgument.POW)
        }

        // Formatting and number operations
        "", ":loop" -> {
          // Skip empty lines and instructions.
        }
        "pi", "π" -> {
          opZero(calcData, ZeroArgument.PI)
        }
        "e" -> {
          opZero(calcData, ZeroArgument.E)
        }
        "\u0061\u006c\u0069\u0065\u006e" -> {
          val a = "\uD83D\uDEF8\uD83D\uDC7D\uD83D\uDC7E" +
              " ".repeat((1..17).shuffled().first())
          if (calcData.numberList.isEmpty()) {
            calcData.numberList.add(
              CalcLine(
                desc = a,
                value = Double.NaN
              )
            )
          } else {
            val op = calcData.numberList.removeLast()
            calcData.numberList.add(
              CalcLine(
                desc = "$a ${op.desc}",
                value = op.value
              )
            )
          }
        }
        "validate" -> {
          validArgs = opVarArg(calcData, VariableArguments.VALIDATE)
          // Stop here when validate fails.
          if (!validArgs) {
            return
          }
        }

        // Variable operation
        "rcl" -> {
          recallAllVariables(calcData)
        }
        else -> {

          // Comment
          // If word is comment, add comment to the last entry.
          // (?s) = dotall = . + \n
          val comment = getRegexOneGroup(word, commentRegex)
          // is comment?
          if (comment != null) {

//            // Add line if list is empty to add the text to.
//            if (calcData.numberList.isEmpty()) {
//              calcData.numberList.add(CalcLine(desc = desc, value = Double.NaN))
//            } else {
//              val op = calcData.numberList.removeLast()
//              if (op.value.isNaN()) {
//                // add comment
//                op.desc += desc
//              } else {
//                op.desc = desc
//              }
//              calcData.numberList.add(op)
//            }

            calcData.numberList.add(CalcLine(desc = comment, value = Double.NaN))

          } else {

            // sto[.name]
            val variableName = getRegexOneGroup(word, stoRegex)
            if (variableName != null) {
              validArgs = storeVariable(calcData, variableName)

            } else {

              // rcl[.name]
              val recallVariable = getRegexOneGroup(word, rclRegex)
              if (recallVariable != null) {
                recallVariable(calcData, recallVariable)

              } else {

                // evaluate $$StockSymbol
                if (word.startsWith("$$")) {
                  val stockSymbol = word.drop(2)
                  evaluate(calcData, stockSymbol)

                } else {

                  // Evaluate number
                  try {
                    val value = numberFormat.parse(word)!!
                      .toDouble()

                    calcData.numberList.add(CalcLine(desc = "", value = value))
                  } catch (e: Exception) {
                    // Error
                    calcData.errorMsg = context.getString(R.string.calc_error_parsing_msg, word)
                    success = false
                  }

                }
              }
            }
          }
        }
      }

      if (!success) {
        calcData.errorMsg = context.getString(R.string.calc_error_msg, word)
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

  private fun storeVariable(calcData: CalcData, name: String): Boolean {
    val argsValid = calcData.numberList.size > 0

    if (argsValid) {
      endEdit(calcData)

      // 1: op1
      val op1 = calcData.numberList.removeLast()
      varMap[name] = op1

    } else {
      calcData.errorMsg = context.getString(R.string.calc_invalid_args)
    }

    calcRepository.updateData(calcData)

    return argsValid
  }

  private fun recallVariable(calcData: CalcData, name: String) {
    endEdit(calcData)

    if (varMap.containsKey(name)) {
      calcData.numberList.add(varMap[name]!!)

      calcRepository.updateData(calcData)
    }
  }

  private fun recallAllVariables(calcData: CalcData) {
    endEdit(calcData)

    varMap.forEach { (name, line) ->
      calcData.numberList.add(CalcLine(desc = "Variable '$name'", value = Double.NaN))
      calcData.numberList.add(line)
    }
    //calcData.numberList.add(CalcLine(desc = "${varMap.size}", value = Double.NaN))

    calcRepository.updateData(calcData)
  }

  // $$tsla
  // $$tsla.marketprice
  // $$tsla.purchaseprice
  // $$tsla.quantity
  fun evaluate(calcData: CalcData, expression: String) {

    // word[.property]
    val match = getRegexTwoGroups(expression, "(.+?)([.].+?)?$".toRegex())

    var word = expression.toUpperCase(Locale.ROOT)
    var property = ""
    if (match != null) {
      // first group (groups[0]) is entire src
      // captured word is in groups[1]
      // captured property is in groups[2]
      word = match.first.toUpperCase(Locale.ROOT)
      property = match.second.toLowerCase(Locale.ROOT)
    }

    val stockItem = stockitemList.find { stockItem ->
      stockItem.stockDBdata.symbol.equals(word, true)
    }

    var desc = "$word$property="
    var value = 0.0
    if (stockItem != null) {

      when {
        property == ".marketprice" -> {
          value = stockItem.onlineMarketData.marketPrice
        }
        property == ".purchaseprice" -> {
          val (quantity, price, commission) = com.thecloudsite.stockroom.utils.getAssets(
            stockItem.assets
          )
          value = price / quantity
        }
        property == ".quantity" -> {
          val (quantity, price, commission) = com.thecloudsite.stockroom.utils.getAssets(
            stockItem.assets
          )
          value = quantity
        }
        property == ".marketchange" -> {
          value = stockItem.onlineMarketData.marketChange
        }
        property == ".marketchangepercent" -> {
          value = stockItem.onlineMarketData.marketChangePercent
        }
        property == ".name" -> {
          desc = getName(stockItem.onlineMarketData)
          value = Double.NaN
        }
        property == ".currency" -> {
          desc = stockItem.onlineMarketData.currency
          value = Double.NaN
        }
        property == ".annualdividendrate" -> {
          value = if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
            stockItem.stockDBdata.annualDividendRate
          } else {
            stockItem.onlineMarketData.annualDividendRate
          }
        }
        property.isEmpty() -> {
          value = stockItem.onlineMarketData.marketPrice
          if (value == 0.0) {
            // Offline: use purchase price
            val (quantity, price, commission) = com.thecloudsite.stockroom.utils.getAssets(
              stockItem.assets
            )
            if (quantity != 0.0) {
              value = price / quantity
            }
          }
        }
        else -> {
          // unknown property
          value = Double.NaN
        }
      }
    }

    calcData.numberList.add(
      CalcLine(
        desc = desc,
        value = value
      )
    )
  }

  // clipboard import/export
  fun getText(): String {
    val calcData = submitEditline(calcData.value!!)
    calcRepository.updateData(calcData)

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

      // Use function(...) to interpret the content instead of parsing as a number.
      // This allows pasting code scripts.
      function(text)

//      val calcData = calcData.value!!
//
//      calcData.editMode = true
//      calcData.editline = text
//      calcData.errorMsg = ""
//
//      calcRepository.updateData(submitEditline(calcData, desc))

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

  fun opVarArg(op: VariableArguments): Boolean {
    val calcData = submitEditline(calcData.value!!)
    return opVarArg(calcData, op)
  }

  private fun opVarArg(calcData: CalcData, op: VariableArguments): Boolean {
    var argsValid = false
    endEdit(calcData)

    when (op) {
      VariableArguments.PICK -> {
        val size = calcData.numberList.size
        if (size > 1) {

          val n = calcData.numberList.removeLast().value.toInt()
          if (size > n) {
            argsValid = true
            // copy level n to level 1
            val nLevelOp = calcData.numberList[size - n - 1]
            // Clone nLevelOp
            calcData.numberList.add(CalcLine(desc = nLevelOp.desc, value = nLevelOp.value))
          }
        }
      }
      VariableArguments.ROLL -> {
        val size = calcData.numberList.size
        if (size > 1) {

          val n = calcData.numberList.removeLast().value.toInt()
          if (size > n) {
            argsValid = true
            // move level n to level 1
            val nLevelOp = calcData.numberList.removeAt(size - n - 1)
            // Copy nLevelOp
            calcData.numberList.add(nLevelOp)
          }
        }
      }
      VariableArguments.SUM -> {
        var n = 0
        var sum = 0.0
        calcData.numberList.forEach { calcLine ->
          if (calcLine.value.isFinite()) {
            n++
            sum += calcLine.value
          }
        }
        if (n > 1) {
          argsValid = true
          calcData.numberList.clear()
          calcData.numberList.add(CalcLine(desc = "Σ=", value = sum))
          calcData.numberList.add(CalcLine(desc = "n=", value = n.toDouble()))
        }
      }
      VariableArguments.VAR -> {
        val size = calcData.numberList.size
        if (size > 1) {
          var sum = 0.0
          var n = 0
          calcData.numberList.forEach { calcLine ->
            val x = calcLine.value
            if (x.isFinite()) {
              n++
              sum += x
            }
          }

          if (n > 1) {
            argsValid = true
            val mean = sum / n

            var variance = 0.0
            calcData.numberList.forEach { calcLine ->
              val x1 = calcLine.value
              if (x1.isFinite()) {
                val x = x1 - mean
                variance += x * x
              }
            }

            variance /= n

            calcData.numberList.clear()
            calcData.numberList.add(CalcLine(desc = "σ²=", value = variance))
          }
        }
      }
      VariableArguments.VALIDATE -> {
        val size = calcData.numberList.size
        if (size >= 2) {

          val n = calcData.numberList.removeLast().value.toInt()
          val errorOp = calcData.numberList.removeLast()
          if (n > 0 && n <= size - 2) {
            argsValid = true
            for (i in 0 until n) {
              if (!calcData.numberList[size - 3 - i].value.isFinite()) {
                argsValid = false
                break
              }
            }
          }
          if (!argsValid) {
            calcData.numberList.add(errorOp)
            calcRepository.updateData(calcData)
            return false
          }
        }
      }
    }

    if (!argsValid) {
      calcData.errorMsg = context.getString(R.string.calc_invalid_args)
    }

    calcRepository.updateData(calcData)

    return argsValid
  }

  fun opZero(op: ZeroArgument) {
    val calcData = submitEditline(calcData.value!!)
    opZero(calcData, op)
  }

  private fun opZero(calcData: CalcData, op: ZeroArgument) {
    endEdit(calcData)

    when (op) {
      ZeroArgument.CLEAR -> {
        calcData.numberList.clear()
      }
      ZeroArgument.DEPTH -> {
        val size = calcData.numberList.size.toDouble()
        calcData.numberList.add(CalcLine(desc = "", value = size))
      }
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

      val op1 = calcData.numberList.removeLast()

      when (op) {
        UnaryArgument.DROP -> {
        }
        UnaryArgument.DUP -> {
          calcData.numberList.add(op1)
          calcData.numberList.add(op1)
        }
        UnaryArgument.SQRT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(0.5)))
        }
        UnaryArgument.SQ -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.pow(2)))
        }
        UnaryArgument.INV -> {
          calcData.numberList.add(CalcLine(desc = "", value = 1 / op1.value))
        }
        UnaryArgument.ABS -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.absoluteValue))
        }
        UnaryArgument.SIGN -> {
          calcData.numberList.add(CalcLine(desc = "", value = -op1.value))
        }
        UnaryArgument.INT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op1.value.toInt().toDouble()))
        }
        UnaryArgument.ROUND -> {
          calcData.numberList.add(
            CalcLine(
              desc = "",
              // roundToLong is not defined for Double.NaN
              value = if (op1.value.isFinite()) {
                op1.value.roundToLong().toDouble()
              } else {
                op1.value
              }
            )
          )
        }
        UnaryArgument.ROUND2 -> {
          calcData.numberList.add(
            CalcLine(
              desc = "",
              // roundToLong is not defined for Double.NaN
              value = if (op1.value.isFinite()) {
                op1.value.times(100.0).roundToLong().toDouble().div(100.0)
              } else {
                op1.value
              }
            )
          )
        }
        UnaryArgument.ROUND4 -> {
          calcData.numberList.add(
            CalcLine(
              desc = "",
              // roundToLong is not defined for Double.NaN
              value = if (op1.value.isFinite()) {
                op1.value.times(10000.0).roundToLong().toDouble().div(10000.0)
              } else {
                op1.value
              }
            )
          )
        }
        UnaryArgument.FRAC -> {
          val fraction = frac(op1.value)
          if (fraction.first != null) {
            calcData.numberList.add(CalcLine(desc = "", value = fraction.first!!.toDouble()))
            calcData.numberList.add(CalcLine(desc = "", value = fraction.second.toDouble()))
          } else {
            calcData.numberList.add(op1)
          }
        }
        UnaryArgument.TOSTR -> {
          // Convert the value to string.
          if (op1.value.isFinite()) {
            calcData.numberList.add(
              CalcLine(
                desc = numberFormat.format(op1.value),
                value = Double.NaN
              )
            )
          } else {
            calcData.numberList.add(op1)
          }
        }
        UnaryArgument.SIN -> {
          calcData.numberList.add(CalcLine(desc = "", value = sin(op1.value * radian)))
        }
        UnaryArgument.SINH -> {
          calcData.numberList.add(CalcLine(desc = "", value = sinh(op1.value)))
        }
        UnaryArgument.COS -> {
          calcData.numberList.add(CalcLine(desc = "", value = cos(op1.value * radian)))
        }
        UnaryArgument.COSH -> {
          calcData.numberList.add(CalcLine(desc = "", value = cosh(op1.value)))
        }
        UnaryArgument.TAN -> {
          calcData.numberList.add(CalcLine(desc = "", value = tan(op1.value * radian)))
        }
        UnaryArgument.TANH -> {
          calcData.numberList.add(CalcLine(desc = "", value = tanh(op1.value)))
        }
        UnaryArgument.ARCSIN -> {
          calcData.numberList.add(CalcLine(desc = "", value = asin(op1.value) / radian))
        }
        UnaryArgument.ARCSINH -> {
          calcData.numberList.add(CalcLine(desc = "", value = asinh(op1.value)))
        }
        UnaryArgument.ARCCOS -> {
          calcData.numberList.add(CalcLine(desc = "", value = acos(op1.value) / radian))
        }
        UnaryArgument.ARCCOSH -> {
          calcData.numberList.add(CalcLine(desc = "", value = acosh(op1.value)))
        }
        UnaryArgument.ARCTAN -> {
          calcData.numberList.add(CalcLine(desc = "", value = atan(op1.value) / radian))
        }
        UnaryArgument.ARCTANH -> {
          calcData.numberList.add(CalcLine(desc = "", value = atanh(op1.value)))
        }
        UnaryArgument.LN -> {
          calcData.numberList.add(CalcLine(desc = "", value = ln(op1.value)))
        }
        UnaryArgument.EX -> {
          calcData.numberList.add(CalcLine(desc = "", value = Math.E.pow(op1.value)))
        }
        UnaryArgument.LOG -> {
          calcData.numberList.add(CalcLine(desc = "", value = log10(op1.value)))
        }
        UnaryArgument.ZX -> {
          calcData.numberList.add(CalcLine(desc = "", value = 10.0.pow(op1.value)))
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

      // 2: op2
      // 1: op1
      val op1 = calcData.numberList.removeLast()
      val op2 = calcData.numberList.removeLast()

      when (op) {
        BinaryArgument.ADD -> {
          if (op1.value.isNaN() && op2.value.isNaN()) {
            // add comments if both NaN
            calcData.numberList.add(CalcLine(desc = op2.desc + op1.desc, value = op2.value))
          } else {
            if (op1.value.isNaN() && op1.desc.isNotEmpty()) {
              // set comment to op2 if exists, same as add comments if both NaN
              calcData.numberList.add(CalcLine(desc = op2.desc + op1.desc, value = op2.value))
            } else {
              // default op, add two numbers
              calcData.numberList.add(CalcLine(desc = "", value = op2.value + op1.value))
            }
          }
        }
        BinaryArgument.SUB -> {
          calcData.numberList.add(CalcLine(desc = "", value = op2.value - op1.value))
        }
        BinaryArgument.MULT -> {
          calcData.numberList.add(CalcLine(desc = "", value = op2.value * op1.value))
        }
        BinaryArgument.DIV -> {
          calcData.numberList.add(CalcLine(desc = "", value = op2.value / op1.value))
        }
        BinaryArgument.POW -> {
          calcData.numberList.add(CalcLine(desc = "", value = op2.value.pow(op1.value)))
        }
        BinaryArgument.SWAP -> {
          calcData.numberList.add(op1)
          calcData.numberList.add(op2)
        }
        BinaryArgument.OVER -> {
          calcData.numberList.add(op2)
          calcData.numberList.add(op1)
          // Clone element
          calcData.numberList.add(CalcLine(desc = op2.desc, value = op2.value))
        }
        // Percent
        BinaryArgument.PER -> {
          calcData.numberList.add(CalcLine(desc = "% ", value = op2.value * op1.value / 100))
        }
        // Percent change
        BinaryArgument.PERC -> {
          calcData.numberList.add(
            CalcLine(
              desc = "∆% ",
              value = (op1.value - op2.value) / op2.value * 100
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

      // 3: op3
      // 2: op2
      // 1: op1
      val op1 = calcData.numberList.removeLast()
      val op2 = calcData.numberList.removeLast()
      val op3 = calcData.numberList.removeLast()

      when (op) {
        TernaryArgument.ROT -> {
          calcData.numberList.add(op2)
          calcData.numberList.add(op1)
          calcData.numberList.add(op3)
        }
        TernaryArgument.ZinsMonat -> {
          val K = op3.value // Kapital
          val P = op2.value // Jahreszins
          val J = op1.value // Laufzeit in Jahren
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

  fun opQuad(op: QuadArgument): Boolean {
    val calcData = submitEditline(calcData.value!!)
    return opQuad(calcData, op)
  }

  private fun opQuad(calcData: CalcData, op: QuadArgument): Boolean {
    val argsValid = calcData.numberList.size > 3

    if (argsValid) {
      endEdit(calcData)

      // 4: op4 else part
      // 3: op3 then part
      // 2: op2 conditional op
      // 1: op1 conditional op
      val op1 = calcData.numberList.removeLast()
      val op2 = calcData.numberList.removeLast()
      val op3 = calcData.numberList.removeLast()
      val op4 = calcData.numberList.removeLast()

      when (op) {
        QuadArgument.IFEQ -> {
          val opResult = if (op2.value == op1.value) op3 else op4
          calcData.numberList.add(opResult)
        }
        QuadArgument.IFGE -> {
          val opResult = if (op2.value >= op1.value) op3 else op4
          calcData.numberList.add(opResult)
        }
        QuadArgument.IFGT -> {
          val opResult = if (op2.value > op1.value) op3 else op4
          calcData.numberList.add(opResult)
        }
        QuadArgument.IFLE -> {
          val opResult = if (op2.value <= op1.value) op3 else op4
          calcData.numberList.add(opResult)
        }
        QuadArgument.IFLT -> {
          val opResult = if (op2.value < op1.value) op3 else op4
          calcData.numberList.add(opResult)
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

  fun getLines(): Int {
    return calcData.value!!.numberList.size
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
