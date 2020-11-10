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
import androidx.annotation.Nullable;

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
      private int CLICK_ACTION_THRESHOLD = 200;
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
                String symbol = item.getLabel();
                onClickCallback.run(symbol);
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
        return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */
            || differenceY > CLICK_ACTION_THRESHOLD);
      }
    });

    mapLayout = new SquarifiedLayout();

    // Set up the Paint for the rectangle background
    Paint mRectBackgroundPaint = new Paint();
    mRectBackgroundPaint.setColor(Color.CYAN);
    mRectBackgroundPaint.setStyle(Paint.Style.FILL);

    // Set up the Paint for the rectangle border
    mRectBorderPaint = new Paint();
    mRectBorderPaint.setColor(Color.WHITE);
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

    // Draw all the rectangles and their labels
    for (Mappable mappableItem : mappableItems) {
      AndroidMapItem item = (AndroidMapItem) mappableItem;
      RectF rectF = item.getBoundsRectF();
      drawRectangle(canvas, rectF, item.getColor());
      drawText(canvas, item.getLabel(), item.getText(), rectF);
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

  private void drawText(Canvas canvas, String label, String text, RectF rectF) {
    // Don't draw text for small rectangles
    if (rectF.width() > 40) {
      float labelSize = Math.min(Math.max(rectF.width() / 7, 20), 100);
      float textSize = 0.75f * labelSize;
      mTextPaint.setTextSize((int) labelSize);

      float tym = labelSize / 2;
      float txm = mTextPaint.measureText(label) / 2;
      float xm = rectF.left + rectF.width() / 2 - txm;
      float ym = rectF.top + rectF.height() / 2 - tym;
      canvas.drawText(label, xm, ym, mTextPaint);
      ym += tym;

      mTextPaint.setTextSize((int) textSize);
      tym = textSize / 2;
      txm = mTextPaint.measureText(text) / 2;
      xm = rectF.left + rectF.width() / 2 - txm;
      ym += 2 * tym;
      canvas.drawText(text, xm, ym, mTextPaint);
    }
  }
}

