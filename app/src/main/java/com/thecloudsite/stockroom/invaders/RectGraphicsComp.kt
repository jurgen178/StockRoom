package com.thecloudsite.stockroom.invaders

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class RectGraphicsComp(red: Int, green: Int, blue: Int) : IGraphicsComponent {
    private val paint: Paint = Paint()

    init {
        paint.color = Color.argb(255, red, green, blue)
    }

    override fun draw(canvas: Canvas, gameObject: GameObject){
        canvas.drawRect(gameObject.position, paint)
    }
}
