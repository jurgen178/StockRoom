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

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.core.text.scale
import androidx.core.text.superscript
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.codeeditor.SyntaxHighlightRule
import com.thecloudsite.stockroom.databinding.DialogCalcBinding
import com.thecloudsite.stockroom.databinding.FragmentCalcProgBinding
import com.thecloudsite.stockroom.setBackgroundColor

data class CodeType
  (
  val code: String,
  val name: String,
)

data class CodeTypeJson
  (
  val key: String,
  val code: String,
  val name: String,
)

class CalcProgFragment(stockSymbol: String = "") : CalcBaseFragment(stockSymbol) {

  private var _binding: FragmentCalcProgBinding? = null
  private val codeMap: MutableMap<String, CodeType> = mutableMapOf()

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  companion object {
    fun newInstance(symbol: String) = CalcProgFragment(symbol)
  }

  override fun updateCalcAdapter() {
    val lines = calcViewModel.getLines()
    binding.calcIndicatorDepth.text = if (lines > 3) "$lines" else ""

    // scroll to always show last element at the bottom of the list
    // itemcount is numberlist + editline
    binding.calclines.adapter?.itemCount?.minus(1)
      ?.let { binding.calclines.scrollToPosition(it) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    _binding = FragmentCalcProgBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun runCodeDialog(name: String) {

    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogBinding = DialogCalcBinding.inflate(inflater)

    dialogBinding.calcCode.setTextSize(13f)
    dialogBinding.calcCode.setTypeface(Typeface.MONOSPACE)
    dialogBinding.calcCode.setSyntaxHighlightRules(
      // 0..9
      SyntaxHighlightRule("[+-]?[0-9]+", "#028900"),
      // + - * /
      SyntaxHighlightRule("((\\s|^)[+-/*])+(\\s|\$)", "#FF0000"),
      // loop, variable and label op
      SyntaxHighlightRule("(?i)((\\s|^)(while|do|goto|if|rcl|sto)?[.].+?)+(\\s|\$)", "#2C42C1"),
      SyntaxHighlightRule("(?i)((\\s|^)(rcl))+(\\s|$)", "#2C42C1"),
      // stack op
      SyntaxHighlightRule(
        "(?i)((\\s|^)(validate|clear|depth|drop|dup|over|swap|rot|pick|roll))+(\\s|$)",
        "#0094FF"
      ),
      // math op
      SyntaxHighlightRule(
        "(?i)((\\s|^)(sin|cos|tan|arcsin|arccos|arctan|ln|sqrt|abs|int|round|tostr|sum|var|pi|e))+(\\s|\$)",
        "#B50000"
      ),
      // "text"
      SyntaxHighlightRule("(?s)[\"'](.+?)[\"']", "#A09936"),
      // // comment
      SyntaxHighlightRule("(?m)//.*?$", "#808080"),
      // /* comment */
      SyntaxHighlightRule("(?s)/[*].*?[*]/", "#808080")
    )

    var displayName = ""
    if (codeMap.containsKey(name)) {
      dialogBinding.calcCode.setText(codeMap[name]!!.code)
      displayName = codeMap[name]!!.name
    }

    if (displayName.isEmpty()) {
      displayName = name
    }
    dialogBinding.calcDisplayName.setText(displayName)

    fun save() {
      val calcCodeText = (dialogBinding.calcCode.getEditText().text).toString()
      var calcDisplayNameText = (dialogBinding.calcDisplayName.text).toString().trim()

      // Default display name is the map key (name).
      if (calcDisplayNameText.isEmpty()) {
        calcDisplayNameText = name
      }

      codeMap[name] = CodeType(code = calcCodeText, name = calcDisplayNameText)
      updateKeys()
    }

    builder.setView(dialogBinding.root)
      .setTitle(R.string.calc_code)
      // Add action buttons
      .setNeutralButton(
        R.string.menu_save_filter_set
      ) { _, _ ->
        save()
      }
      .setPositiveButton(
        R.string.execute
      ) { _, _ ->
        save()
        // codeMap[name] gets added by the save function and is always available.
        calcViewModel.function(codeMap[name]!!.code)
      }
      .setNegativeButton(
        R.string.cancel
      ) { _, _ ->
      }
    builder
      .create()
      .show()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(requireActivity())

    binding.calcF1.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF1.setOnClickListener { runCodeDialog(mapKey("F1")) }
    binding.calcF2.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF2.setOnClickListener { runCodeDialog(mapKey("F2")) }
    binding.calcF3.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF3.setOnClickListener { runCodeDialog(mapKey("F3")) }
    binding.calcF4.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF4.setOnClickListener { runCodeDialog(mapKey("F4")) }
    binding.calcF5.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF5.setOnClickListener { runCodeDialog(mapKey("F5")) }
    binding.calcF6.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF6.setOnClickListener { runCodeDialog(mapKey("F6")) }
    binding.calcF7.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF7.setOnClickListener { runCodeDialog(mapKey("F7")) }
    binding.calcF8.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF8.setOnClickListener { runCodeDialog(mapKey("F8")) }
    binding.calcF9.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF9.setOnClickListener { runCodeDialog(mapKey("F9")) }
    binding.calcF10.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF10.setOnClickListener { runCodeDialog(mapKey("F10")) }
    binding.calcF11.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF11.setOnClickListener { runCodeDialog(mapKey("F11")) }
    binding.calcF12.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF12.setOnClickListener { runCodeDialog(mapKey("F12")) }

//    binding.calcZinsMonat.setOnTouchListener { view, event -> touchHelper(view, event); false }
//    binding.calcZinsMonat.setOnClickListener { calcViewModel.opTernary(TernaryArgument.ZinsMonat) }
    binding.calcSin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSin.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.SINH)
      } else {
        calcViewModel.opUnary(UnaryArgument.SIN)
      }
    }
    binding.calcCos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcCos.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.COSH)
      } else {
        calcViewModel.opUnary(UnaryArgument.COS)
      }
    }
    binding.calcTan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcTan.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.TANH)
      } else {
        calcViewModel.opUnary(UnaryArgument.TAN)
      }
    }
    binding.calcArcsin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArcsin.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.ARCSINH)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCSIN)
      }
    }
    binding.calcArccos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArccos.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.ARCCOSH)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCCOS)
      }
    }
    binding.calcArctan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArctan.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.ARCTANH)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCTAN)
      }
    }

    binding.calcLog.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcLog.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.LN)
      } else {
        calcViewModel.opUnary(UnaryArgument.LOG)
      }
    }
    binding.calcZx.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcZx.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opUnary(UnaryArgument.EX)
      } else {
        calcViewModel.opUnary(UnaryArgument.ZX)
      }
    }
    binding.calcConst.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcConst.setOnClickListener {
      if (calcViewModel.shift) {
        calcViewModel.opZero(ZeroArgument.E)
      } else {
        calcViewModel.opZero(ZeroArgument.PI)
      }
    }
    binding.calcSum.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSum.setOnClickListener { calcViewModel.opVarArg(VariableArguments.SUM) }

    binding.calcShift.setOnTouchListener { view, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        setBackgroundColor(view, requireContext().getColor(R.color.calcShiftPressed))
      } else
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
          setBackgroundColor(view, requireContext().getColor(R.color.calcShift))
        }
      false
    }
    binding.calcShift.setOnClickListener {
      calcViewModel.shift = !calcViewModel.shift
      updateShift()
    }

    updateShift()
  }

  override fun onPause() {
    super.onPause()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)

    val codeMapStr = getSerializedStr()
    sharedPreferences
      .edit()
      .putString("calcCodeMap", codeMapStr)
      .apply()
  }

  override fun onResume() {
    super.onResume()

    // Get the latest market value for the stock.
    // This fragment uses the online data only for script code $$symbol evaluation.
    // CalcFragment uses the online data for the spinner data and runs runOnlineTaskNow()
    // in the touch handler for each update.
    stockRoomViewModel.runOnlineTaskNow()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)

    val codeMapStr = sharedPreferences.getString("calcCodeMap", "").toString()
    setSerializedStr(codeMapStr)

    // Set default code if all codes are empty.
    val codes = codeMap.map { code ->
      code.value
    }.filter { codeType ->
      codeType.code.isNotEmpty()
    }

    if (codes.isEmpty()) {

      // Default only for the first 12 entries.
      val resList = listOf(
        Triple("F1", R.string.calc_F1_code, R.string.calc_F1_desc),
        Triple("F2", R.string.calc_F2_code, R.string.calc_F2_desc),
        Triple("F3", R.string.calc_F3_code, R.string.calc_F3_desc),
        Triple("F4", R.string.calc_F4_code, R.string.calc_F4_desc),
        Triple("F5", R.string.calc_F5_code, R.string.calc_F5_desc),
        Triple("F6", R.string.calc_F6_code, R.string.calc_F6_desc),
        Triple("F7", R.string.calc_F7_code, R.string.calc_F7_desc),
        Triple("F8", R.string.calc_F8_code, R.string.calc_F8_desc),
        Triple("F9", R.string.calc_F9_code, R.string.calc_F9_desc),
        Triple("F10", R.string.calc_F10_code, R.string.calc_F10_desc),
        Triple("F11", R.string.calc_F11_code, R.string.calc_F11_desc),
        Triple("F12", R.string.calc_F12_code, R.string.calc_F12_desc),
      )

      resList.forEach { entry ->

//        codeMap["F1"] =
//          CodeType(
//            code = requireContext().getString(R.string.calc_F1_code),
//            name = requireContext().getString(R.string.calc_F1_desc)
//          )

        codeMap[entry.first] =
          CodeType(
            code = requireContext().getString(entry.second),
            name = requireContext().getString(entry.third)
          )
      }
    }

    updateKeys()
  }

  private fun updateShift() {
    if (calcViewModel.shift) {
      binding.calcIndicatorShift.text = "↱"

      // set e^x
      binding.calcZx.text = SpannableStringBuilder()
        .append("e")
        .superscript { superscript { scale(0.65f) { bold { append("x") } } } }
    } else {
      binding.calcIndicatorShift.text = ""

      // set 10^x
      binding.calcZx.text = SpannableStringBuilder()
        .append("10")
        .superscript { superscript { scale(0.7f) { bold { append("x") } } } }
    }

    updateKeys()
  }

  private fun mapKey(key: String): String {
    if (calcViewModel.shift) {
      if (key == "F1") {
        return "F13"
      }
      if (key == "F2") {
        return "F14"
      }
      if (key == "F3") {
        return "F15"
      }
      if (key == "F4") {
        return "F16"
      }
      if (key == "F5") {
        return "F17"
      }
      if (key == "F6") {
        return "F18"
      }
      if (key == "F7") {
        return "F19"
      }
      if (key == "F8") {
        return "F20"
      }
      if (key == "F9") {
        return "F21"
      }
      if (key == "F10") {
        return "F22"
      }
      if (key == "F11") {
        return "F23"
      }
      if (key == "F12") {
        return "F24"
      }

      if (key == "sin") {
        return "sinh"
      }
      if (key == "cos") {
        return "cosh"
      }
      if (key == "tan") {
        return "tanh"
      }
      if (key == "sin⁻¹") {
        return "sinh⁻¹"
      }
      if (key == "cos⁻¹") {
        return "cosh⁻¹"
      }
      if (key == "tan⁻¹") {
        return "tanh⁻¹"
      }
      if (key == "log") {
        return "ln"
      }
      // 10^x/e^x is set directly
      if (key == "π") {
        return "e"
      }
    }
    return key
  }

  private fun updateKeys() {

    val textViewList = listOf(
      Pair(binding.calcF1, mapKey("F1")),
      Pair(binding.calcF2, mapKey("F2")),
      Pair(binding.calcF3, mapKey("F3")),
      Pair(binding.calcF4, mapKey("F4")),
      Pair(binding.calcF5, mapKey("F5")),
      Pair(binding.calcF6, mapKey("F6")),
      Pair(binding.calcF7, mapKey("F7")),
      Pair(binding.calcF8, mapKey("F8")),
      Pair(binding.calcF9, mapKey("F9")),
      Pair(binding.calcF10, mapKey("F10")),
      Pair(binding.calcF11, mapKey("F11")),
      Pair(binding.calcF12, mapKey("F12")),

      Pair(binding.calcSin, mapKey("sin")),
      Pair(binding.calcCos, mapKey("cos")),
      Pair(binding.calcTan, mapKey("tan")),
      Pair(binding.calcArcsin, mapKey("sin⁻¹")),
      Pair(binding.calcArccos, mapKey("cos⁻¹")),
      Pair(binding.calcArctan, mapKey("tan⁻¹")),

      Pair(binding.calcLog, mapKey("log")),
      // 10^x/e^x is set directly
      Pair(binding.calcConst, mapKey("π")),
    )

    textViewList.forEach { pair ->

//      val F1 = codeMap["F1"]?.name
//      binding.calcF1.text = if (F1.isNullOrEmpty()) "F1" else F1

      val F = codeMap[pair.second]?.name
      pair.first.text = if (F.isNullOrEmpty()) pair.second else F
    }
  }

  private fun getSerializedStr(): String {

    var jsonString = ""
    try {
      val codeTypeJsonList: MutableList<CodeTypeJson> = mutableListOf()
      codeMap.forEach { (key, codeType) ->
        codeTypeJsonList.add(
          CodeTypeJson(
            key = key,
            code = codeType.code,
            name = if (codeType.name.isNotEmpty()) {
              codeType.name
            } else {
              key
            },
          )
        )
      }

      // Convert to a json string.
      val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

      jsonString = gson.toJson(codeTypeJsonList)
    } catch (e: Exception) {
    }

    return jsonString
  }

  private fun setSerializedStr(
    codeData: String
  ) {
    codeMap.clear()

    try {

      val sType = object : TypeToken<List<CodeTypeJson>>() {}.type
      val gson = Gson()
      val codeList = gson.fromJson<List<CodeTypeJson>>(codeData, sType)

      codeList?.forEach { codeTypeJson ->
        // de-serialized JSON type can be null
        codeMap[codeTypeJson.key] =
          CodeType(
            code = codeTypeJson.code,
            name = if (codeTypeJson.name.isNotEmpty()) {
              codeTypeJson.name
            } else {
              codeTypeJson.key
            }
          )
      }
    } catch (e: Exception) {
    }
  }
}
