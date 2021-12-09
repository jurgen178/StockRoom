/*
 * Copyright 2013 Robert Theis
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
package com.thecloudsite.stockroom.treemap;

import android.graphics.RectF;

public class AndroidMapItem extends MapItem implements AndroidMappable {
    private final String symbol;
    private final String label;
    private final String text;
    private final String change;
    private Integer backgroundColor;
    private final Integer textColor;

    public AndroidMapItem(double weight, String symbol, String label, String text, String change, Integer backgroundColor, Integer textColor) {
        setSize(weight);
        this.symbol = symbol;
        this.label = label;
        this.text = text;
        this.change = change;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public String getChange() {
        return change;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public Integer getTextColor() {
        return textColor;
    }

    public void setBackgroundColor(Integer backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /* Return an Android RectF that is the size of the bounds rectangle */
    public RectF getBoundsRectF() {
        Rect bounds = getBounds();
        return new RectF(Double.valueOf(bounds.x).floatValue(),
                Double.valueOf(bounds.y).floatValue(),
                Double.valueOf(bounds.x).floatValue() + Double.valueOf(bounds.w).floatValue(),
                Double.valueOf(bounds.y).floatValue() + Double.valueOf(bounds.h).floatValue());
    }
}