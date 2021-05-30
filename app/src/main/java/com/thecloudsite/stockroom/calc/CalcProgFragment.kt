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
import com.thecloudsite.stockroom.SyntaxHighlightRule
import com.thecloudsite.stockroom.databinding.DialogCalcBinding
import com.thecloudsite.stockroom.databinding.FragmentCalcProgBinding

class CalcProgFragment(stockSymbol: String = "") : CalcBaseFragment(stockSymbol) {

  private var _binding: FragmentCalcProgBinding? = null

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

    dialogBinding.calcCode.codeMap = calcViewModel.codeMap
    dialogBinding.calcCode.syntaxHighlightRules = listOf(
      // "text"
      SyntaxHighlightRule("(?s)[\"'](.+?)[\"']", "#D89E00"),
      // 1.234,56
      SyntaxHighlightRule("((\\s|^)[+-]?[0-9]+?[,.]?[0-9]*?)+(\\s|$)", "#028900"),
      // $$stock[.property]
      SyntaxHighlightRule("((\\s|^)[$]{2}\\w+?([.]\\w+)?)+(\\s|$)", "#A700D6"),
      // + - * /
      SyntaxHighlightRule("((\\s|^)[+-/*^])+(\\s|$)", "#B50000"),
      // loop, variable and label op
      SyntaxHighlightRule("(?i)((\\s|^)(do|goto|rcl|sto)?[.].+?)+(\\s|$)", "#FF7F7F"),
      SyntaxHighlightRule("(?i)((\\s|^)while[.](eq|le|lt|ge|gt)[.].+?)+(\\s|$)", "#FF7F7F"),
      SyntaxHighlightRule("(?i)((\\s|^)if[.](eq|le|lt|ge|gt)([.].+?)?)+(\\s|$)", "#FF7F7F"),
      SyntaxHighlightRule("(?i)((\\s|^):loop|:radian|:degree)+(\\s|$)", "#FF6A00"),
      SyntaxHighlightRule("(?i)((\\s|^)rcl)+(\\s|$)", "#2C42C1"),
      // stack op
      SyntaxHighlightRule(
        "(?i)((\\s|^)(validate|clear|depth|drop|dup|over|swap|rot|pick|roll))+(\\s|$)",
        "#0094FF"
      ),
      // math op
      SyntaxHighlightRule(
        "(?i)((\\s|^)(sin|cos|tan|arcsin|arccos|arctan|sinh|cosh|tanh|arcsinh|arccosh|arctanh|ln|log|sq|sqrt|pow|per|perc|inv|abs|mod|int|round|round2|round4|frac|tostr|sum|var|pi|π|e))+(\\s|$)",
        "#B50000"
      ),
      // ()
      SyntaxHighlightRule(
        "(?i)((\\s|^)[(].*?[)])+(\\s|$)",
        "#B851E8"
      ),
      // // comment
      SyntaxHighlightRule("(?m)//.*?$", "#808080"),
      // /* comment */
      SyntaxHighlightRule("(?s)/[*].*?[*]/", "#808080"),
      SyntaxHighlightRule("(?i)\u0061\u006c\u0069\u0065\u006e", "#00FF21")
    )

    var displayName = ""
    if (calcViewModel.codeMap.containsKey(name)) {
      dialogBinding.calcCode.setText(calcViewModel.codeMap[name]!!.code)
      displayName = calcViewModel.codeMap[name]!!.name
    }

    if (displayName.isEmpty()) {
      displayName = name
    }
    dialogBinding.calcDisplayName.setText(displayName)

    fun save() {
      val calcCodeText = (dialogBinding.calcCode.text).toString()
      var calcDisplayNameText = (dialogBinding.calcDisplayName.text).toString().trim()

      // Default display name is the map key (name).
      if (calcDisplayNameText.isEmpty()) {
        calcDisplayNameText = name
      }

      calcViewModel.codeMap[name] = CodeType(code = calcCodeText, name = calcDisplayNameText)
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
        calcViewModel.function(calcViewModel.codeMap[name]!!.code, name)
      }
      .setNegativeButton(
        R.string.cancel
      ) { _, _ ->
      }
    builder
      .create()
      .show()
  }

  private fun touchHelperFunction(
    view: View,
    event: MotionEvent
  ) {
    return touchHelper(
      view,
      event,
      requireContext().getColor(R.color.calcFunctionPressed),
      requireContext().getColor(R.color.calcFunction)
    )
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    binding.calclines.adapter = calcAdapter
    binding.calclines.layoutManager = LinearLayoutManager(requireActivity())

    binding.calcF1.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF1.setOnClickListener { runCodeDialog(mapKey("F1")) }
    binding.calcF2.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF2.setOnClickListener { runCodeDialog(mapKey("F2")) }
    binding.calcF3.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF3.setOnClickListener { runCodeDialog(mapKey("F3")) }
    binding.calcF4.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF4.setOnClickListener { runCodeDialog(mapKey("F4")) }
    binding.calcF5.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF5.setOnClickListener { runCodeDialog(mapKey("F5")) }
    binding.calcF6.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF6.setOnClickListener { runCodeDialog(mapKey("F6")) }
    binding.calcF7.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF7.setOnClickListener { runCodeDialog(mapKey("F7")) }
    binding.calcF8.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF8.setOnClickListener { runCodeDialog(mapKey("F8")) }
    binding.calcF9.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF9.setOnClickListener { runCodeDialog(mapKey("F9")) }
    binding.calcF10.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF10.setOnClickListener { runCodeDialog(mapKey("F10")) }
    binding.calcF11.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF11.setOnClickListener { runCodeDialog(mapKey("F11")) }
    binding.calcF12.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF12.setOnClickListener { runCodeDialog(mapKey("F12")) }
    binding.calcF13.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF13.setOnClickListener { runCodeDialog(mapKey("F13")) }
    binding.calcF14.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF14.setOnClickListener { runCodeDialog(mapKey("F14")) }
    binding.calcF15.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF15.setOnClickListener { runCodeDialog(mapKey("F15")) }
    binding.calcF16.setOnTouchListener { view, event -> touchHelperFunction(view, event); false }
    binding.calcF16.setOnClickListener { runCodeDialog(mapKey("F16")) }

//    binding.calcZinsMonat.setOnTouchListener { view, event -> touchHelper(view, event); false }
//    binding.calcZinsMonat.setOnClickListener { calcViewModel.opTernary(TernaryArgument.ZinsMonat) }
    binding.calcSin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSin.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.SIN)
      } else {
        calcViewModel.opUnary(UnaryArgument.SINH)
      }
    }
    binding.calcCos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcCos.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.COS)
      } else {
        calcViewModel.opUnary(UnaryArgument.COSH)
      }
    }
    binding.calcTan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcTan.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.TAN)
      } else {
        calcViewModel.opUnary(UnaryArgument.TANH)
      }
    }
    binding.calcArcsin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArcsin.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.ARCSIN)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCSINH)
      }
    }
    binding.calcArccos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArccos.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.ARCCOS)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCCOSH)
      }
    }
    binding.calcArctan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArctan.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.ARCTAN)
      } else {
        calcViewModel.opUnary(UnaryArgument.ARCTANH)
      }
    }

    binding.calcLog.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcLog.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.LOG)
      } else {
        calcViewModel.opUnary(UnaryArgument.LN)
      }
    }
    binding.calcZx.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcZx.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opUnary(UnaryArgument.ZX)
      } else {
        calcViewModel.opUnary(UnaryArgument.EX)
      }
    }
    binding.calcConst.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcConst.setOnClickListener {
      if (calcViewModel.shiftLevel == 0) {
        calcViewModel.opZero(ZeroArgument.PI)
      } else {
        calcViewModel.opZero(ZeroArgument.E)
      }
    }
    binding.calcSum.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSum.setOnClickListener { calcViewModel.opVarArg(VariableArguments.SUM) }

    binding.calcShift.setOnTouchListener { view, event ->
      touchHelper(
        view,
        event,
        requireContext().getColor(R.color.calcShiftPressed),
        requireContext().getColor(R.color.calcShift)
      )
      false
    }
    binding.calcFrac.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcFrac.setOnClickListener { calcViewModel.opUnary(UnaryArgument.FRAC) }
    binding.calcShift.setOnClickListener {
      calcViewModel.shiftLevel = (calcViewModel.shiftLevel + 1).rem(3)
      updateShift()
    }

    binding.calcIndicatorRadian.setOnClickListener {
      radian = if (radian == 1.0) {
        binding.calcIndicatorRadian.text = "360°"
        Math.PI / 180.0
      } else {
        binding.calcIndicatorRadian.text = "2π"
        1.0
      }

      calcViewModel.radian = radian
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

    sharedPreferences
      .edit()
      .putBoolean("calc_format_radian", radian == 1.0)
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

    if (codeMapStr.isEmpty()) {

      // Default only for the first 16 entries.
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
        Triple("F13", R.string.calc_F13_code, R.string.calc_F13_desc),
        Triple("F14", R.string.calc_F14_code, R.string.calc_F14_desc),
        Triple("F15", R.string.calc_F15_code, R.string.calc_F15_desc),
        Triple("F16", R.string.calc_F16_code, R.string.calc_F16_desc),
      )

      resList.forEach { entry ->

//        codeMap["F1"] =
//          CodeType(
//            code = requireContext().getString(R.string.calc_F1_code),
//            name = requireContext().getString(R.string.calc_F1_desc)
//          )

        calcViewModel.codeMap[entry.first] =
          CodeType(
            code = requireContext().getString(entry.second),
            name = requireContext().getString(entry.third)
          )
      }
    } else {
      setSerializedStr(codeMapStr)
    }

    if (radian == 1.0) {
      binding.calcIndicatorRadian.text = "2π"
    } else {
      binding.calcIndicatorRadian.text = "360°"
    }

    updateKeys()
  }

  private fun updateShift() {
    when (calcViewModel.shiftLevel) {
      0 -> {
        binding.calcShift.text = "↱"
        binding.calcIndicatorShift.text = ""
      }
      1 -> {
        binding.calcShift.text = "↱  ↱"
        binding.calcIndicatorShift.text = "↱"
      }
      2 -> {
        binding.calcShift.text = "↱  ↱↱"
        binding.calcIndicatorShift.text = "↱↱"
      }
    }

    if (calcViewModel.shiftLevel == 0) {
      // set 10^x
      binding.calcZx.text = SpannableStringBuilder()
        .append("10")
        .superscript { superscript { scale(0.7f) { bold { append("x") } } } }
    } else {
      // set e^x
      binding.calcZx.text = SpannableStringBuilder()
        .append("e")
        .superscript { superscript { scale(0.65f) { bold { append("x") } } } }
    }

    updateKeys()
  }

  private fun mapKey(key: String): String {
    if (calcViewModel.shiftLevel > 0) {

      // Fnn keys
      val match = "^F(\\d{1,2})$".toRegex().matchEntire(key)
      if (match != null
        && match.groups.size == 2
        && match.groups[1] != null
      ) {
        // first group (groups[0]) is entire text
        // first capture is in groups[1]
        var value = match.groups[1]!!.value.toInt()

        // 16 keys per shift level
        value += 16 * calcViewModel.shiftLevel
        return "F$value"
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
      Pair(binding.calcF13, mapKey("F13")),
      Pair(binding.calcF14, mapKey("F14")),
      Pair(binding.calcF15, mapKey("F15")),
      Pair(binding.calcF16, mapKey("F16")),

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

      val F = calcViewModel.codeMap[pair.second]?.name
      pair.first.text = if (F.isNullOrEmpty()) pair.second else F
    }
  }

  private fun getSerializedStr(): String {

    if (calcViewModel.codeMap.isEmpty()) {
      return ""
    }

    var jsonString = ""
    try {
      val codeTypeJsonList: MutableList<CodeTypeJson> = mutableListOf()
      calcViewModel.codeMap.forEach { (key, codeType) ->
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
    calcViewModel.codeMap.clear()

    try {

      val sType = object : TypeToken<List<CodeTypeJson>>() {}.type
      val gson = Gson()
      val codeList = gson.fromJson<List<CodeTypeJson>>(codeData, sType)

      codeList?.forEach { codeTypeJson ->
        // de-serialized JSON type can be null
        calcViewModel.codeMap[codeTypeJson.key] =
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
