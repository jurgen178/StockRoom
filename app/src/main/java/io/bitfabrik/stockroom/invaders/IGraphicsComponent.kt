package io.bitfabrik.stockroom.invaders

import android.graphics.Canvas

interface IGraphicsComponent {
    fun draw(canvas: Canvas, gameObject: GameObject) {}
}