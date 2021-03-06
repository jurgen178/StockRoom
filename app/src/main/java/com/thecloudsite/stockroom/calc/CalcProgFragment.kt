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
import com.thecloudsite.stockroom.databinding.DialogCalcBinding
import com.thecloudsite.stockroom.databinding.FragmentCalcProgBinding

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
    // scroll to always show last element at the bottom of the list
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
      val calcCodeText = (dialogBinding.calcCode.text).toString()
      var calcDisplayNameText = (dialogBinding.calcDisplayName.text).toString().trim()

      // Default display name is the map key (name).
      if (calcDisplayNameText.isEmpty()) {
        calcDisplayNameText = name
      }

      codeMap[name] = CodeType(code = calcCodeText, name = calcDisplayNameText)
      updateFKeys()
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
    binding.calcF1.setOnClickListener { runCodeDialog("F1") }
    binding.calcF2.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF2.setOnClickListener { runCodeDialog("F2") }
    binding.calcF3.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF3.setOnClickListener { runCodeDialog("F3") }
    binding.calcF4.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF4.setOnClickListener { runCodeDialog("F4") }
    binding.calcF5.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF5.setOnClickListener { runCodeDialog("F5") }
    binding.calcF6.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF6.setOnClickListener { runCodeDialog("F6") }
    binding.calcF7.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF7.setOnClickListener { runCodeDialog("F7") }
    binding.calcF8.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcF8.setOnClickListener { runCodeDialog("F8") }

//    binding.calcZinsMonat.setOnTouchListener { view, event -> touchHelper(view, event); false }
//    binding.calcZinsMonat.setOnClickListener { calcViewModel.opTernary(TernaryArgument.ZinsMonat) }
    binding.calcSin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSin.setOnClickListener { calcViewModel.opUnary(UnaryArgument.SIN) }
    binding.calcCos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcCos.setOnClickListener { calcViewModel.opUnary(UnaryArgument.COS) }
    binding.calcTan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcTan.setOnClickListener { calcViewModel.opUnary(UnaryArgument.TAN) }
    binding.calcArcsin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArcsin.setOnClickListener { calcViewModel.opUnary(UnaryArgument.ARCSIN) }
    binding.calcArccos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArccos.setOnClickListener { calcViewModel.opUnary(UnaryArgument.ARCCOS) }
    binding.calcArctan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcArctan.setOnClickListener { calcViewModel.opUnary(UnaryArgument.ARCTAN) }

    binding.calcLn.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcLn.setOnClickListener { calcViewModel.opUnary(UnaryArgument.LN) }
    binding.calcEx.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcEx.setOnClickListener { calcViewModel.opUnary(UnaryArgument.EX) }
    binding.calcEx.text = SpannableStringBuilder()
      .append("e")
      .superscript { superscript { scale(0.65f) { bold { append("x") } } } }
    binding.calcLog.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcLog.setOnClickListener { calcViewModel.opUnary(UnaryArgument.LOG) }
    binding.calcZx.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcZx.setOnClickListener { calcViewModel.opUnary(UnaryArgument.ZX) }
    binding.calcZx.text = SpannableStringBuilder()
      .append("10")
      .superscript { superscript { scale(0.7f) { bold { append("x") } } } }

    binding.calcPi.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPi.setOnClickListener { calcViewModel.opZero(ZeroArgument.PI) }
    binding.calcE.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcE.setOnClickListener { calcViewModel.opZero(ZeroArgument.E) }
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
      codeMap["F1"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F1_code),
          name = requireContext().getString(R.string.calc_F1_desc)
        )
      codeMap["F2"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F2_code),
          name = requireContext().getString(R.string.calc_F2_desc)
        )
      codeMap["F3"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F3_code),
          name = requireContext().getString(R.string.calc_F3_desc)
        )
      codeMap["F4"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F4_code),
          name = requireContext().getString(R.string.calc_F4_desc)
        )
      codeMap["F5"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F5_code),
          name = requireContext().getString(R.string.calc_F5_desc)
        )
      codeMap["F6"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F6_code),
          name = requireContext().getString(R.string.calc_F6_desc)
        )
      codeMap["F7"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F7_code),
          name = requireContext().getString(R.string.calc_F7_desc)
        )
      codeMap["F8"] =
        CodeType(
          code = requireContext().getString(R.string.calc_F8_code),
          name = requireContext().getString(R.string.calc_F8_desc)
        )
    }

    updateFKeys()
  }

  private fun updateFKeys() {
    val F1 = codeMap["F1"]?.name
    binding.calcF1.text = if (F1.isNullOrEmpty()) "F1" else F1

    val F2 = codeMap["F2"]?.name
    binding.calcF2.text = if (F2.isNullOrEmpty()) "F2" else F2

    val F3 = codeMap["F3"]?.name
    binding.calcF3.text = if (F3.isNullOrEmpty()) "F3" else F3

    val F4 = codeMap["F4"]?.name
    binding.calcF4.text = if (F4.isNullOrEmpty()) "F4" else F4

    val F5 = codeMap["F5"]?.name
    binding.calcF5.text = if (F5.isNullOrEmpty()) "F5" else F5

    val F6 = codeMap["F6"]?.name
    binding.calcF6.text = if (F6.isNullOrEmpty()) "F6" else F6

    val F7 = codeMap["F7"]?.name
    binding.calcF7.text = if (F7.isNullOrEmpty()) "F7" else F7

    val F8 = codeMap["F8"]?.name
    binding.calcF8.text = if (F8.isNullOrEmpty()) "F8" else F8
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
