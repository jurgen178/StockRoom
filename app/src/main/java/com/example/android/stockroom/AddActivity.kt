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

package com.example.android.stockroom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_add.button_add

/**
 * Activity for entering a new symbol.
 */

class AddActivity : AppCompatActivity() {

    private lateinit var addView: EditText

    companion object {
        const val EXTRA_REPLY = "com.example.android.stockroom.ADDSYMBOL"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        addView = findViewById(R.id.edit_add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        button_add.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(addView.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                val symbol = addView.text.toString()
                replyIntent.putExtra(EXTRA_REPLY, symbol)
                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

