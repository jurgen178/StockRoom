/*
 * Copyright (C) 2017 Google Inc.
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

package com.thecloudsite.stockroom

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.SVBar
import kotlinx.android.synthetic.main.activity_updategroup.addGroupButton
import kotlinx.android.synthetic.main.activity_updategroup.addPredefinedGroupsButton
import kotlinx.android.synthetic.main.activity_updategroup.deleteAllGroupButton
import kotlinx.android.synthetic.main.activity_updategroup.groupView

class UpdateGroupActivity : AppCompatActivity() {
  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var stockDBdataList: List<StockDBdata> = emptyList()
  private var groupList: List<Group> = emptyList()

  private fun groupItemUpdateClicked(group: GroupData) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_group, null)

    val addUpdateGroupsHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateGroupsHeadline)
    addUpdateGroupsHeadlineView.text = getString(R.string.update_groups_dialog_headline, group.name)

    val colorView = dialogView.findViewById<ColorPicker>(R.id.colorPicker)
    val svbarView = dialogView.findViewById<SVBar>(R.id.colorPickerSV)
//      val saturationbarView = dialogView.findViewById<SaturationBar>(R.id.colorPickerSaturationbar)
//      val valuebarView = dialogView.findViewById<ValueBar>(R.id.colorPickerValuebar)
    colorView.addSVBar(svbarView)

    val clr = group.color
    colorView.color = clr
    colorView.oldCenterColor = clr
    colorView.setNewCenterColor(clr)
//      colorView.addSaturationBar(saturationbarView)
//      colorView.addValueBar(valuebarView)

    val addNameView = dialogView.findViewById<TextView>(R.id.addName)
    addNameView.text = group.name

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          val color = colorView.color
          if (clr != color) {
            // Change color of all stocks from the old color 'clr' to the new color.
            stockRoomViewModel.updateStockGroupColors(clr, color)
            // Delete the old color.
            stockRoomViewModel.deleteGroup(clr)
            // Add the new color.
            stockRoomViewModel.setGroup(color = color, name = group.name)
          }
          // Add () to avoid cast exception.
          val nameText = (addNameView.text).toString()
              .trim()
          val nameUsed = groupList.find { group ->
            group.color != clr && group.name == nameText
          } != null
          if (nameUsed) {
            Toast.makeText(this, getString(R.string.group_name_in_use, nameText), Toast.LENGTH_LONG)
                .show()
            return@setPositiveButton
          }

          if (group.name != nameText && nameText.isNotEmpty()) {
            // Add or update the group with color/name.
            stockRoomViewModel.setGroup(color = color, name = nameText)
          }
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
        }
    builder
        .create()
        .show()
  }

  private fun groupItemDeleteClicked(group: GroupData) {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.delete_group_title, group.name))
        .setMessage(
            if (group.stats == 0) {
              getString(R.string.delete_group_confirm, group.name)
            } else {
              getString(R.string.delete_group_confirm2, group.name, group.stats)
            }
        )
        .setPositiveButton(R.string.delete) { _, _ ->
          stockDBdataList.forEach { data ->
            if (data.groupColor == group.color) {
              // reset color
              stockRoomViewModel.setStockGroupColor(data.symbol, 0)
            }
          }
          stockRoomViewModel.deleteGroup(group.color)

          Toast.makeText(
              this, getString(R.string.delete_group_msg, group.name), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_updategroup)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    val groupItemClickListenerUpdate =
      { group: GroupData -> groupItemUpdateClicked(group) }
    val groupItemClickListenerDelete =
      { group: GroupData -> groupItemDeleteClicked(group) }
    val updateGroupAdapter =
      UpdateGroupAdapter(this, groupItemClickListenerUpdate, groupItemClickListenerDelete)
    groupView.adapter = updateGroupAdapter
    groupView.layoutManager = LinearLayoutManager(this)

    stockRoomViewModel.allGroupTable.observe(this, Observer { groups ->
      updateGroupAdapter.addGroups(groups)
      groupList = groups
    })

    stockRoomViewModel.allProperties.observe(this, Observer { items ->
      updateGroupAdapter.updateData(items)
      stockDBdataList = items
    })

    //val groups: List<Group> = stockRoomViewModel.getGroupsSync()
    //updateGroupAdapter.addGroups(groups)

    addGroupButton.setOnClickListener {
      val builder = AlertDialog.Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.add_group, null)
      val addUpdateGroupsHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateGroupsHeadline)
      addUpdateGroupsHeadlineView.text = getString(R.string.add_group)

      val colorView = dialogView.findViewById<ColorPicker>(R.id.colorPicker)
      val svbarView = dialogView.findViewById<SVBar>(R.id.colorPickerSV)
//      val saturationbarView = dialogView.findViewById<SaturationBar>(R.id.colorPickerSaturationbar)
//      val valuebarView = dialogView.findViewById<ValueBar>(R.id.colorPickerValuebar)
      colorView.addSVBar(svbarView)

      val clr = Color.BLUE
      colorView.color = clr
      colorView.oldCenterColor = clr
      colorView.setNewCenterColor(clr)
//      colorView.addSaturationBar(saturationbarView)
//      colorView.addValueBar(valuebarView)

      val addNameView = dialogView.findViewById<TextView>(R.id.addName)
      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val nameText = (addNameView.text).toString()
                .trim()
            if (nameText.isEmpty()) {
              Toast.makeText(
                  this, getString(R.string.group_name_not_empty), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }

            val color = colorView.color
            // Check if color is used
            val colorUsed = groupList.find { group ->
              group.color == color
            } != null
            if (colorUsed) {
              Toast.makeText(this, getString(R.string.group_color_in_use), Toast.LENGTH_LONG)
                  .show()
              return@setPositiveButton
            }

            val nameUsed = groupList.find { group ->
              group.name == nameText
            } != null
            if (nameUsed) {
              Toast.makeText(
                  this, getString(R.string.group_name_in_use, nameText), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }

            // Add the group with color/name.
            stockRoomViewModel.setGroup(color = color, name = nameText)
          }
          .setNegativeButton(
              R.string.cancel
          ) { _, _ ->
          }
      builder
          .create()
          .show()
    }

    addPredefinedGroupsButton.setOnClickListener {
      AlertDialog.Builder(this)
          .setTitle(R.string.add_predef_groups_title)
          .setMessage(getString(R.string.add_predef_groups_confirm))
          .setPositiveButton(R.string.add) { _, _ ->
            stockRoomViewModel.setPredefinedGroups()
            Toast.makeText(this, getString(R.string.add_predef_groups_msg), Toast.LENGTH_LONG)
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }

    deleteAllGroupButton.setOnClickListener {
      AlertDialog.Builder(this)
          .setTitle(R.string.delete_allgroups_title)
          .setMessage(getString(R.string.delete_all_groups_confirm))
          .setPositiveButton(R.string.delete) { _, _ ->
            // Remove color form all stocks.
            stockDBdataList.forEach { data ->
              if (data.groupColor != 0) {
                // reset color
                stockRoomViewModel.setStockGroupColor(data.symbol, 0)
              }
            }
            stockRoomViewModel.deleteAllGroups()
            Toast.makeText(this, getString(R.string.delete_all_groups_msg), Toast.LENGTH_LONG)
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}
