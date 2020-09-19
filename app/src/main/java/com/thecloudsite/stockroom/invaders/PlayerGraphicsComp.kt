package com.thecloudsite.stockroom.invaders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

class PlayerGraphicsComp(var bitmap: Bitmap) : IGraphicsComponent {
    private val paint: Paint = Paint()

    override fun draw(canvas: Canvas, gameObject: GameObject){
        canvas.drawBitmap(bitmap, gameObject.position.left, gameObject.position.top, paint)
    }
}
