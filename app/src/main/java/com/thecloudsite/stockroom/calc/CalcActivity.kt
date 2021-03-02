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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.databinding.ActivityCalcBinding

class CalcActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCalcBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityCalcBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    binding.calcViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            val instance = CalcFragment.newInstance()
            instance
          }
          1 -> {
            val instance = CalcProgFragment.newInstance()
            instance
          }
          else -> {
            val instance = CalcFragment.newInstance()
            instance
          }
        }
      }

      override fun getItemCount(): Int {
        return 2
      }
    }

    binding.calcViewpager.setCurrentItem(0, false)

    TabLayoutMediator(binding.tabLayout, binding.calcViewpager) { tab, position ->
      tab.text = when (position) {
        0 -> getString(R.string.calc_headline)
        1 -> getString(R.string.calc_prog_headline)
        else -> ""
      }
    }.attach()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.calc_menu, menu)
    return true
  }

  fun onSettings(item: MenuItem) {
    val intent = Intent(this@CalcActivity, CalcSettingsActivity::class.java)
    startActivity(intent)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}
