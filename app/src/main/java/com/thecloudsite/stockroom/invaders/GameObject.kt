package com.thecloudsite.stockroom.invaders

import android.graphics.Canvas
import android.graphics.RectF

class GameObject(val moveComp: IMoveComponent,
                    val graphicsComp: IGraphicsComponent,
                    var position: RectF) {
    // This data is accessible using ClassName.propertyName
    companion object {
        // Which ways can the game object is moving
        const val stopped = 0
        const val left = 1
        const val right = 2
        const val forward = 4
        const val backward = 8
    }
    var moving: Int = stopped

    var action1: Boolean = false
    var action2: Boolean = false

    var canAction1: Boolean = true
    var canAction2: Boolean = true

    var removeMe: Boolean = false

    fun draw(canvas: Canvas) {
        graphicsComp.draw(canvas, this)
    }

    fun update(fps: Long) {
        moveComp.update(fps,this)
    }
}
