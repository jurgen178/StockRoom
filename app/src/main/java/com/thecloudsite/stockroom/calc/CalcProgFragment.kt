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
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.databinding.DialogCalcBinding
import com.thecloudsite.stockroom.databinding.FragmentCalcProgBinding

class CalcProgFragment : CalcBaseFragment() {

  private var _binding: FragmentCalcProgBinding? = null

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

  private fun runCodeDialog(code: String, desc: String) {

    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogBinding = DialogCalcBinding.inflate(inflater)

    dialogBinding.calcCode.setText(code)
    dialogBinding.calcDesc.setText(desc)

    builder.setView(dialogBinding.root)
      .setTitle(R.string.calc_code)
      // Add action buttons
      .setPositiveButton(
        R.string.execute
      ) { _, _ ->
        // Add () to avoid cast exception.
        val calcCodeText = (dialogBinding.calcCode.text).toString()
          .trim()

        val calcDescText = (dialogBinding.calcDesc.text).toString()
          .trim()

        //calcViewModel.function("dup 1.0 +", "test")
        calcViewModel.function(calcCodeText, calcDescText)
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
    binding.calcF1.setOnClickListener {
      runCodeDialog("dup 1.0 +", "test")
    }

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
}
