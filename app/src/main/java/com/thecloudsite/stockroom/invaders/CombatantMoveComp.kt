package com.thecloudsite.stockroom.invaders

import android.graphics.Point
import android.graphics.RectF
import com.thecloudsite.stockroom.invaders.GameObject.Companion
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class CombatantMoveComp(private val size: Point, private var act1Interval: Long, private var act2Interval: Long, private var speed: Int, var allyList: ArrayList<GameObject>) :
    IMoveComponent {

    override fun update(fps: Long, gameObject: GameObject) {
        // Move as long as it doesn't try and leave the screen

        val width = gameObject.position.right - gameObject.position.left
        if (gameObject.moving == Companion.left && gameObject.position.left > 0) {
            gameObject.position.left -= speed / fps
        }

        else if (gameObject.moving == Companion.right && gameObject.position.right < size.x) {
            gameObject.position.left += speed / fps
        }

        gameObject.position.right = gameObject.position.left + width

        if(gameObject.action1) {
            val bulletWidth : Float = 2.0f
            val bulletHeight : Float = size.y / 40f
            var rectBullet = RectF(gameObject.position.left + gameObject.position.width() / 2f,
                gameObject.position.top,
                gameObject.position.left + gameObject.position.width() / 2f + bulletWidth,
                gameObject.position.top + bulletHeight)

            var bullet = GameObject(GeneralMoveComp(size, 350), RectGraphicsComp(64, 255, 64), rectBullet)
            bullet.moving = Companion.forward
            var gObjIt = allyList.listIterator()
            gObjIt.add(bullet)

            gameObject.canAction1 = false
            Timer("Action1 delay", false).schedule(act1Interval) {
                gameObject.canAction1 = true
            }
        }

        if(gameObject.action2) {
            val bulletWidth : Float = 2.0f
            val bulletHeight : Float = size.y / 40f
            var rectBullet = RectF(gameObject.position.left + gameObject.position.width() / 2f,
                gameObject.position.top,
                gameObject.position.left + gameObject.position.width() / 2f + bulletWidth,
                gameObject.position.top + bulletHeight)

            var bullet = GameObject(GeneralMoveComp(size, 350), RectGraphicsComp(255, 64, 64), rectBullet)
            bullet.moving = Companion.backward
            var gObjIt = allyList.listIterator()
            gObjIt.add(bullet)

            gameObject.canAction2 = false
            Timer("Action2 delay", false).schedule(act2Interval) {
                gameObject.canAction2 = true
            }
        }

        gameObject.action1 = false
        gameObject.action2 = false
    }
}
