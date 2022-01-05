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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.thecloudsite.stockroom.R;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapLayoutView extends View {

    private final AbstractMapLayout mapLayout;
    private Mappable[] mappableItems;
    private final Paint mRectBorderPaint;
    private final Paint mTextPaint;
    private final android.graphics.Rect drawingRect = new android.graphics.Rect(0, 0, 0, 0);
    private OnClickCallback onClickCallback;

    public interface OnClickCallback {
        void run(String symbol);
    }

    public void setOnClickCallback(OnClickCallback onClickCallback) {
        this.onClickCallback = onClickCallback;
    }

    public MapLayoutView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.setOnTouchListener(new OnTouchListener() {
            private float startX;
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        if (isAClick(startX, endX, startY, endY)) {
                            int index = getIndex(startX, startY);
                            if (index != -1) {
                                Mappable mappableItem = mappableItems[index];
                                AndroidMapItem item = (AndroidMapItem) mappableItem;

                                if (!item.getText().isEmpty()) {
                                    // color the clicked tile
                                    item.setBackgroundColor(Color.GRAY);
                                    v.invalidate();

                                    String symbol = item.getSymbol();
                                    onClickCallback.run(symbol);
                                }
                            }
                            //launchFullPhotoActivity(imageUrls);// WE HAVE A CLICK!!
                        }
                        break;
                }
                //v.getParent().requestDisallowInterceptTouchEvent(true); //specific to my project
                return true;
            }

            private boolean isAClick(float startX, float endX, float startY, float endY) {
                float differenceX = Math.abs(startX - endX);
                float differenceY = Math.abs(startY - endY);
                int CLICK_ACTION_THRESHOLD = 200;
                return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */
                        || differenceY > CLICK_ACTION_THRESHOLD);
            }
        });

        mapLayout = new SquarifiedLayout();

        // Set up the Paint for the rectangle border
        mRectBorderPaint = new Paint();
        mRectBorderPaint.setColor(context.getColor(R.color.white));
        mRectBorderPaint.setStyle(Paint.Style.STROKE); // outline the rectangle
        mRectBorderPaint.setStrokeWidth(8);

        // Set up the Paint for the text label
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(20);
        mTextPaint.setAntiAlias(true);
    }

    public void setTreeModel(TreeModel model) {
        mappableItems = model.getTreeItems();

        map();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawingRect.right = w;
        drawingRect.bottom = h;

        map();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Check if all displayable items have the same background color,
        // and use Reg/Green instead.
        boolean sameBackgroundColor = false;

        // Don't use small rectangles.
        List<Mappable> items = Arrays.stream(mappableItems).filter(mappableItem -> ((AndroidMapItem) mappableItem).getBoundsRectF().width() > 40).collect(Collectors.toList());

        if (items.size() > 1) {
            Integer firstBackgroundColor = ((AndroidMapItem) items.get(0)).getBackgroundColor();
            // Color must be different than the -1 value for Red/green colors.
            if (firstBackgroundColor != -1) {
                sameBackgroundColor = true;
                for (Mappable displayableMappableItem : items) {

                    AndroidMapItem item = (AndroidMapItem) displayableMappableItem;

                    Integer color = item.getBackgroundColor();
                    if (!color.equals(firstBackgroundColor)) {
                        sameBackgroundColor = false;
                        break;
                    }
                }
            }
        }

        // Draw all the rectangles and their labels
        for (Mappable mappableItem : items) {

            AndroidMapItem item = (AndroidMapItem) mappableItem;
            RectF rectF = item.getBoundsRectF();

            Integer backgroundColor = item.getBackgroundColor();

            // Color in Red/Green when all background colors are the same or backgroundColor is -1
            if (sameBackgroundColor || backgroundColor == -1) {
                if (item.getValue() >= 0.0) {
                    backgroundColor = item.getColorGreen();
                } else {
                    backgroundColor = item.getColorRed();
                }
            }

            Integer textColor = item.getTextColor();
            drawRectangle(canvas, rectF, backgroundColor);
            drawText(canvas, item.getLabel(), item.getText(), item.getChange(), rectF, backgroundColor, textColor);
        }
    }

    private int getIndex(float x, float y) {
        for (int i = 0; i < mappableItems.length; i++) {
            Mappable mappableItem = mappableItems[i];
            AndroidMapItem item = (AndroidMapItem) mappableItem;
            RectF rectF = item.getBoundsRectF();
            if (rectF.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private void map() {
        // Lay out the placement of the rectangles within the area available to this view
        mapLayout.layout(mappableItems, new Rect(
                drawingRect.left,
                drawingRect.top,
                drawingRect.width(),
                drawingRect.height()));
    }

    private void drawRectangle(Canvas canvas, RectF rectF, Integer color) {

        Paint rectBackgroundPaint = new Paint();
        rectBackgroundPaint.setColor(color);
        rectBackgroundPaint.setStyle(Paint.Style.FILL);

        // Draw the rectangle's background
        canvas.drawRect(rectF, rectBackgroundPaint);

        // Draw the rectangle's border
        canvas.drawRect(rectF, mRectBorderPaint);
    }

    private boolean isDarkColor(Integer color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        return r + g + b < 350;
    }

    private void drawText(
            Canvas canvas,
            String label,
            String text,
            String change,
            RectF rectF,
            Integer backgroundColor,
            Integer textColor) {
        if (textColor != 0) {
            mTextPaint.setColor(textColor);
        } else {
            if (backgroundColor != 0 && isDarkColor(backgroundColor)) {
                mTextPaint.setColor(Color.WHITE);
            } else {
                mTextPaint.setColor(Color.BLACK);
            }
        }

        // Print label-only items in smaller font, for example the root item when no assets are present.
        boolean labelOnly = text.isEmpty() && change.isEmpty();
        float labelSize = Math.min(Math.max(rectF.width() / 6, 20), labelOnly ? 48 : 100);
        mTextPaint.setTextSize((int) labelSize);

        float labelX = -1;
        float labelY = -1;
        float textX = -1;
        float textY = -1;
        float changeX = -1;
        float changeY = -1;

        android.graphics.Rect labelRect = new android.graphics.Rect(0, 0, 0, 0);
        mTextPaint.getTextBounds(label, 0, label.length(), labelRect);
        float tym = labelRect.height();
        float txm = labelRect.width();
        float xm = rectF.left + rectF.width() / 2 - txm / 2;
        float ym = rectF.top + tym + 8; // Border=8
        if (txm > 0 && txm + 8 < rectF.width() && ym + tym < rectF.bottom) {
            labelX = xm;
            labelY = ym;
            ym += tym / 4;
        } else {
            // rect to small for initial text, resize text
            mTextPaint.setTextSize((int) (0.6 * (rectF.height() - 8)));
            mTextPaint.getTextBounds(label, 0, label.length(), labelRect);
            tym = labelRect.height();
            txm = labelRect.width();
            xm = rectF.left + rectF.width() / 2 - txm / 2;
            ym = rectF.top + tym + 8; // Border=8
            if (txm > 0 && txm + 8 < rectF.width() && ym + tym < rectF.bottom) {
                labelX = xm;
                labelY = ym;
                ym += tym / 4;
            } else {
                // rect still to small, shorten the label text and set the text size to match width
                if (label.length() > 8) {
                    label = label.substring(0, 7) + "…";
                    mTextPaint.setTextSize((int) (0.18 * (rectF.width() - 8)));
                    mTextPaint.getTextBounds(label, 0, label.length(), labelRect);
                    tym = labelRect.height();
                    txm = labelRect.width();
                    xm = rectF.left + rectF.width() / 2 - txm / 2;
                    ym = rectF.top + tym + 8; // Border=8
                    if (txm > 0 && txm + 8 < rectF.width() && ym + tym < rectF.bottom) {
                        labelX = xm;
                        labelY = ym;
                        ym += tym / 4;
                    }
                }
            }
        }

        Paint textPaint = new Paint(mTextPaint);
        float textSize = 0.5f * labelSize;
        textPaint.setTextSize((int) textSize);
        android.graphics.Rect textRect = new android.graphics.Rect(0, 0, 0, 0);
        textPaint.getTextBounds(text, 0, text.length(), textRect);
        tym = textRect.height();
        txm = textRect.width();
        xm = rectF.left + rectF.width() / 2 - txm / 2;
        ym += tym;
        if (txm > 0 && labelY > 0 && txm + 8 < rectF.width() && ym + tym < rectF.bottom) {
            textX = xm;
            textY = ym;
            ym += tym / 4;
        }

        float changeSize = 0.5f * labelSize;
        Paint changePaint = new Paint(mTextPaint);
        changePaint.setTextSize((int) changeSize);
        android.graphics.Rect changeRect = new android.graphics.Rect(0, 0, 0, 0);
        changePaint.getTextBounds(change, 0, change.length(), changeRect);
        tym = changeRect.height();
        txm = changeRect.width();
        xm = rectF.left + rectF.width() / 2 - txm / 2;
        ym += tym;
        if (txm > 0 && textY > 0 && txm + 8 < rectF.width() && ym + tym < rectF.bottom) {
            changeX = xm;
            changeY = ym;
        }

        // Center the text vertically.
        float Y = Math.max(labelY, Math.max(textY, changeY));
        float offsetY = 0;
        if (Y < rectF.bottom) {
            offsetY = (rectF.bottom - Y) / 2;
        }
        if (labelY > 0) {
            canvas.drawText(label, labelX, labelY + offsetY, mTextPaint);
        }
        if (textY > 0) {
            canvas.drawText(text, textX, textY + offsetY, textPaint);
        }
        if (changeY > 0) {
            canvas.drawText(change, changeX, changeY + offsetY, changePaint);
        }
    }
}

