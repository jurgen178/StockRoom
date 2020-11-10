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
import android.view.View;

public class MapLayoutView extends View {

  private AbstractMapLayout mapLayout;
  private Mappable[] mappableItems;
  private Paint mRectBackgroundPaint;
  private Paint mRectBorderPaint;
  private Paint mTextPaint;
  private android.graphics.Rect drawingRect = new android.graphics.Rect(0, 0, 0, 0);

  public MapLayoutView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);

    mapLayout = new SquarifiedLayout();

    // Set up the Paint for the rectangle background
    mRectBackgroundPaint = new Paint();
    mRectBackgroundPaint.setColor(Color.CYAN);
    mRectBackgroundPaint.setStyle(Paint.Style.FILL);

    // Set up the Paint for the rectangle border
    mRectBorderPaint = new Paint();
    mRectBorderPaint.setColor(Color.BLACK);
    mRectBorderPaint.setStyle(Paint.Style.STROKE); // outline the rectangle
    mRectBorderPaint.setStrokeWidth(0); // single-pixel outline

    // Set up the Paint for the text label
    mTextPaint = new Paint();
    mTextPaint.setColor(Color.BLACK);
    mTextPaint.setTextSize(20);
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
      drawRectangle(canvas, item.getBoundsRectF(), item.getColor());
      drawText(canvas, item.getLabel(), item.getBoundsRectF());
    }
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

  private void map() {
    // Lay out the placement of the rectangles within the area available to this view
    mapLayout.layout(mappableItems, new Rect(
        drawingRect.left,
        drawingRect.top,
        drawingRect.width(),
        drawingRect.height()));
  }

  private void drawText(Canvas canvas, String text, RectF rectF) {
    // Don't draw text for small rectangles
    if (rectF.width() > 30) {
      float textSize = Math.max(rectF.width() / 7, 12);
      mTextPaint.setTextSize(textSize);
      canvas.drawText(text, rectF.left + rectF.width() / 2 + 2 - textSize / 2,
          rectF.top + textSize / 2 + rectF.height() / 2,
          mTextPaint);
    }
  }
}

