package com.thecloudsite.stockroom.invaders

import android.graphics.Point
import com.thecloudsite.stockroom.invaders.GameObject.Companion
import kotlin.random.Random

class EnemyAI(private val size: Point, private val allies: ArrayList<GameObject>, private val player: GameObject) :
    IEnemyController {
    override fun update(wave: Int) {
        var reverse = false
        for(ally in allies) {
            if(ally.moving == Companion.right && ally.position.right >= size.x ||
                ally.moving == Companion.left && ally.position.left <= 0) {
                reverse = true
                break
            }
        }

        if(Random.nextInt(0, 1000) > 990) {
            val randomShooter = Random.nextInt(0, allies.size)
            allies[randomShooter].action2 = true
        }

        if(reverse) {
            for(ally in allies) {
                if(ally.moving == Companion.left)
                    ally.moving = Companion.right
                else if(ally.moving == Companion.right)
                    ally.moving = Companion.left

                val height = ally.position.height()
                ally.position.top += height
                ally.position.bottom += height
            }
        }
    }
}