package com.thecloudsite.stockroom.invaders

import android.graphics.Point

class GeneralMoveComp(private val size: Point, private var speed: Int) : IMoveComponent {
    override fun update(fps: Long, gameObject: GameObject) {
        val width = gameObject.position.right - gameObject.position.left
        val height = gameObject.position.bottom - gameObject.position.top

        if (gameObject.moving == GameObject.left)
            gameObject.position.left -= speed / fps
        else if (gameObject.moving == GameObject.right)
            gameObject.position.left += speed / fps

        if (gameObject.moving == GameObject.forward)
            gameObject.position.top -= speed / fps
        else if (gameObject.moving == GameObject.backward)
            gameObject.position.top += speed / fps

        gameObject.position.right = gameObject.position.left + width
        gameObject.position.bottom = gameObject.position.top + height

        if(gameObject.position.top <= 0 ||
                gameObject.position.bottom >= size.y ||
                gameObject.position.left <= 0 ||
                gameObject.position.right >= size.x)
            gameObject.removeMe = true
    }
}
