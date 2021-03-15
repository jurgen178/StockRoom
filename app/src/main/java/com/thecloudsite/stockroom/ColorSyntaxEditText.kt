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

package com.thecloudsite.stockroom

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.thecloudsite.stockroom.calc.CodeType
import com.thecloudsite.stockroom.calc.wordDefinitionRegex
import com.thecloudsite.stockroom.calc.wordListRegex
import java.util.regex.Pattern

// https://github.com/testica/codeeditor

data class SyntaxHighlightRule(
  var regex: String,
  var color: String
)

fun afterTextChanged(predicate: (() -> Unit)): TextWatcher {

  return object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
      predicate.invoke()
    }
  }
}

fun Editable.removeAllSpans() {
  val spans = this.getSpans(0, this.length, ForegroundColorSpan::class.java)
  for (span in spans) this.removeSpan(span)
}

class ColorSyntaxEditText(context: Context, attrs: AttributeSet) :
  AppCompatEditText(context, attrs) {

  var syntaxHighlightRules: List<SyntaxHighlightRule> = emptyList()
  var codeMap: MutableMap<String, CodeType> = mutableMapOf()

  init {
    addTextChangedListener(afterTextChanged { applySyntaxHighlight() })
  }

  private fun getDefinitions(text: String): List<SyntaxHighlightRule> {
    val rules: MutableList<SyntaxHighlightRule> = mutableListOf()

    var codePreprocessed = text

    // resolve imports
    val regexImport = Regex("(?i)(?:\\s|^)import[.]\"(.+?)\"")
    val matchesImport = regexImport.findAll(text)

    // process each import statement
    matchesImport.forEach { matchResult ->
      val import = matchResult.groupValues[1]

      // Search all code entries for a matching name.
      val codeKey = codeMap.filterValues { codeValue ->
        codeValue.name.equals(import, true)
      }

      if (codeKey.size == 1) {
        // Get all definitions in the import.
        val importedCode = codeKey.values.first().code
        val matchesDefinition = wordDefinitionRegex.findAll(importedCode)
        matchesDefinition.forEach { matchResultDefinition ->
          val definition = matchResultDefinition.groupValues[1]
          codePreprocessed += " $definition "
        }
      }
    }

    val matches = wordDefinitionRegex.findAll(codePreprocessed)
    matches.forEach { matchResult ->
      val name = matchResult.groupValues[1]
      if (!wordListRegex.matches(name)) {
        val escapedName = name
          .replace("[", "\\[")
          .replace("]", "\\]")
          .replace("(", "[(]")
          .replace(")", "[)]")
          .replace("{", "[{]")
          .replace("}", "[}]")
          .replace("|", "[|]")
          .replace("+", "[+]")
          .replace("^", "[^]")
          .replace("*", "[*]")
          .replace(".", "[.]")
          .replace("/", "[/]")
          .replace("$", "[$]")
          .replace("?", "[?]")
        rules.add(SyntaxHighlightRule("(?i)((\\s|^)$escapedName)+(\\s|$)", "#FF6A00"))
      }
    }

    return rules
  }

  private fun applySyntaxHighlight() {
    if (!syntaxHighlightRules.isNullOrEmpty()) {

      // first remove all spans
      text?.removeAllSpans()

      val textStr = text.toString()

      // set span for proper matching according to a rule
      for (syntaxHighlightRule in getDefinitions(textStr)) {
        setSyntax(text, textStr, syntaxHighlightRule)
      }
      for (syntaxHighlightRule in syntaxHighlightRules) {
        setSyntax(text, textStr, syntaxHighlightRule)
      }
    }
  }

  private fun setSyntax(
    text: Editable?,
    textStr: String,
    syntaxHighlightRule: SyntaxHighlightRule
  ) {
    try {
      val color = Color.parseColor(syntaxHighlightRule.color)
      val matcher = Pattern.compile(syntaxHighlightRule.regex).matcher(textStr)

      while (matcher.find()) text?.setSpan(
        ForegroundColorSpan(color),
        matcher.start(),
        matcher.end(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    } catch (e: Exception) {
      // skip exception caused by the custom definition entries
      Log.d("invalid regex in setSyntax", "Exception=$e")
    }
  }
}