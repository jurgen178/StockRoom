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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.thecloudsite.stockroom.FilterFactory
import com.thecloudsite.stockroom.FilterModeTypeEnum
import com.thecloudsite.stockroom.FilterSet
import com.thecloudsite.stockroom.FilterTypeJson
import com.thecloudsite.stockroom.Filters
import com.thecloudsite.stockroom.IFilterType
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.SharedRepository
import com.thecloudsite.stockroom.databinding.DialogCalcBinding
import com.thecloudsite.stockroom.databinding.FragmentCalcProgBinding

data class CodeType
  (
  val code: String,
  val desc: String,
)

data class CodeTypeJson
  (
  val name: String,
  val code: String,
  val desc: String,
)

class CalcProgFragment : CalcBaseFragment() {

  private var _binding: FragmentCalcProgBinding? = null
  private val codeMap: MutableMap<String, CodeType> = mutableMapOf()

  private var f1code = ""
  private var f1desc = ""

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  companion object {
    fun newInstance() = CalcProgFragment()
  }

  override fun updateUI() {
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

    if (codeMap.containsKey(name)) {
      dialogBinding.calcCode.setText(codeMap[name]!!.code)
      dialogBinding.calcDesc.setText(codeMap[name]!!.desc)
    }

    fun save() {
      val calcCodeText = (dialogBinding.calcCode.text).toString()
      val calcDescText = (dialogBinding.calcDesc.text).toString()

      codeMap[name] = CodeType(code = calcCodeText, desc = calcDescText)
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
        calcViewModel.function(codeMap[name]!!.code, codeMap[name]!!.desc)
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

    binding.calcZinsMonat.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcZinsMonat.setOnClickListener { calcViewModel.opTernary(TernaryArgument.ZinsMonat) }
    binding.calcSin.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcSin.setOnClickListener { calcViewModel.opUnary(UnaryArgument.SIN) }
    binding.calcCos.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcCos.setOnClickListener { calcViewModel.opUnary(UnaryArgument.COS) }
    binding.calcTan.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcTan.setOnClickListener { calcViewModel.opUnary(UnaryArgument.TAN) }
    binding.calcLn.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcLn.setOnClickListener { calcViewModel.opUnary(UnaryArgument.LN) }
    binding.calcPi.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcPi.setOnClickListener { calcViewModel.opZero(ZeroArgument.PI) }
    binding.calcE.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcE.setOnClickListener { calcViewModel.opZero(ZeroArgument.E) }
    binding.calcEx.setOnTouchListener { view, event -> touchHelper(view, event); false }
    binding.calcEx.setOnClickListener { calcViewModel.opUnary(UnaryArgument.E) }
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

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)

    val codeMapStr = sharedPreferences.getString("calcCodeMap", "").toString()
    if (codeMapStr.isEmpty()) {
      codeMap["F1"] = CodeType(code = "over - swap / 100 *", desc = "∆% ")
    } else {
      setSerializedStr(codeMapStr)
    }
  }

  private fun getSerializedStr(): String {

    var jsonString = ""
    try {
      val codeTypeJsonList: MutableList<CodeTypeJson> = mutableListOf()
      codeMap.forEach { (name, codeType) ->
        codeTypeJsonList.add(
          CodeTypeJson(
            name = name,
            code = codeType.code,
            desc = codeType.desc,
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
    try {

      val sType = object : TypeToken<List<CodeTypeJson>>() {}.type
      val gson = Gson()
      val codeList = gson.fromJson<List<CodeTypeJson>>(codeData, sType)

      codeMap.clear()
      codeList?.forEach { codeTypeJson ->
        // de-serialized JSON type can be null
        codeMap[codeTypeJson.name] = CodeType(code = codeTypeJson.code, desc = codeTypeJson.desc)
      }
    } catch (e: Exception) {
    }
  }
}
