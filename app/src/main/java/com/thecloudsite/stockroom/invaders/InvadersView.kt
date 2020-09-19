package com.thecloudsite.stockroom.invaders

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.SurfaceView
import android.util.Log
import android.view.MotionEvent
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.invaders.SoundPlayer.Companion

class InvadersView(context: Context, private val size: Point)
    : SurfaceView(context), // SurfaceView is a View, therefore setContentView can be called on this
    Runnable { // Implements the Runnable interface

    // This variable tracks the game frame rate
    var fps: Long = 0

    // For making a noise
    private val soundPlayer = SoundPlayer(context)

    // This is our thread
    private val gameThread = Thread(this) // OS will call run

    // A boolean which we will set and unset
    private var playing = false

    // Game is paused at the start
    private var paused = true

    // A Canvas and a Paint object
    private var canvas: Canvas = Canvas()
    private val paint: Paint = Paint()

    private var friendlyGameObjs = arrayListOf<GameObject>()
    private var neutralGameObjs = arrayListOf<GameObject>()
    private var enemyGameObjs = arrayListOf<GameObject>()

    // Add to this temporary list to avoid concurrent access to enemyGameObjs in update()
    private var kludgeEnemyBullets = arrayListOf<GameObject>()

    // The players ship
    private val playerWidth = size.x / 20f
    private val playerHeight = size.y / 20f
    var bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.playership)
    var bitmapScaled = Bitmap.createScaledBitmap(bitmap, playerWidth.toInt(), playerHeight.toInt(),false)
    val position = RectF(size.x / 2f,size.y.toFloat() - playerHeight,size.x / 2f + playerWidth, size.y.toFloat())
    private var playerShip: GameObject = GameObject(
        CombatantMoveComp(size, 500, 20, 450, friendlyGameObjs), PlayerGraphicsComp(bitmapScaled), position)

    private val ai : IEnemyController = EnemyAI(size, enemyGameObjs, playerShip)
    private val collision : ICollisionHandler = CollisionAABB(playerShip, friendlyGameObjs, neutralGameObjs, enemyGameObjs)

    var gameStatus = GameStatus(0, 3, 1)

    // To remember the high score
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "Invaders",
        Context.MODE_PRIVATE)

    private var highScore =  prefs.getInt("highScore", 0)

    // Called every new wave
    private fun prepareLevel() {

        // Add invaders
        val width = size.x / 25f
        val height = size.y / 25f
        val padding = size.x / 60f
        val topBuffer = size.y / 6f

        var bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.invader1)
        var bitmapScaled = Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(),false)

        // Here we will initialize the game objects
        for (column in 0..7) {
            for (row in 0..6) {
                var position = RectF(
                    column * (width + padding),
                    topBuffer + row * (height + padding),
                    column * (width + padding) + width,
                    topBuffer + row * (height + padding) + height
                )
                var invader = GameObject(
                    CombatantMoveComp(size, 8000, 1000, 150 + gameStatus.wave * 10, kludgeEnemyBullets), PlayerGraphicsComp(bitmapScaled), position)
                invader.moving = GameObject.right // Invaders start moving to the right
                enemyGameObjs.add(invader)
            }
        }

        // Add shelters
        val shelterWidth = size.x / 180f
        val shelterHeight = size.y / 90f
        val topOffset = size.y * 13 / 16f
        val leftOffset = size.x / 4f

        for(shelterNumber in 1..3) {
            for (column in 0..20) {
                for (row in 0..10) {
                    var position = RectF(
                        shelterNumber * leftOffset + column * shelterWidth,
                        topOffset + row * shelterHeight,
                        shelterNumber * leftOffset + column * shelterWidth + shelterWidth,
                        topOffset + row * shelterHeight + shelterHeight
                    )
                    var shelterPiece = GameObject(
                        StationaryMoveComp(), RectGraphicsComp(32, 128, 255), position)
                    neutralGameObjs.add(shelterPiece)
                }
            }
        }

    }

    override fun run() {
        // The game loop
        while (playing) {

            // Capture the current time
            val startFrameTime = System.currentTimeMillis()

            if(enemyGameObjs.size == 0) {
                gameStatus.wave++
                prepareLevel()
            }

            if(gameStatus.lives <= 0) {
                paused = true
                gameStatus.score = 0
                gameStatus.lives = 3
                gameStatus.wave = 1
                friendlyGameObjs.clear()
                enemyGameObjs.clear()
                prepareLevel()
            }

            // Update the frame
            if (!paused) {
                update(fps)
            }

            // Draw the frame
            draw()

            // Calculate the fps rate this frame
            val timeThisFrame = System.currentTimeMillis() - startFrameTime
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame
            }
        }
    }

    private fun update(fps: Long) {
        // Update the state of all the game objects

        // Check collisions
        collision.update(gameStatus)

        // Move the player's ship
        playerShip.update(fps)

        // AI decides what enemies do
        ai.update(gameStatus.wave)

        // Update friendlies
        with(friendlyGameObjs.iterator()) {
            forEach {
                if (it.removeMe) {
                    remove()
                }
                else {
                    it.update(fps)
                }
            }
        }

        // Update enemies
        with(enemyGameObjs.iterator()) {
            forEach {
                if (it.removeMe) {
                    remove()
                }
                else {
                    it.update(fps)
                }
            }
        }

        // Append any new bullets to enemyGameObjs
        enemyGameObjs.addAll(kludgeEnemyBullets)
        kludgeEnemyBullets.clear()
    }

    private fun draw() {
        // Make sure our drawing surface is valid or the game will crash
        if (holder.surface.isValid) { // able to write to memory that holds graphics
            // Lock the canvas ready to draw
            canvas = holder.lockCanvas()

            // Draw the background color
            canvas.drawColor(Color.argb(255, 0, 0, 0))

            // Choose the brush color for drawing
            paint.color = Color.argb(255, 0, 255, 0)

            playerShip.draw(canvas)

            for(friend in friendlyGameObjs)
                friend.draw(canvas)

            for(enemy in enemyGameObjs)
                enemy.draw(canvas)

            for(neutral in neutralGameObjs)
                neutral.draw(canvas)

            // Draw the score and remaining lives
            // Change the brush color
            paint.color = Color.argb(255, 255, 255, 255)
            paint.textSize = 70f
            val(score, lives, wave) = gameStatus
            canvas.drawText("Score:$score   Lives:$lives   Wave:$wave   HI:$highScore  FPS:$fps", 20f, 75f, paint)

            // Draw everything to the screen
            holder.unlockCanvasAndPost(canvas)
        }
    }

    // If SpaceInvadersActivity is paused/stopped
    // then shut down our thread.
    fun pause() {
        playing = false

        val prefs = context.getSharedPreferences("Invaders", Context.MODE_PRIVATE)

        val oldHighScore = prefs.getInt("highScore", 0)

        if(highScore > oldHighScore) {
            val editor = prefs.edit()

            editor.putInt("highScore", highScore)

            editor.apply()
        }

        try {
            gameThread.join()
        } catch (e: InterruptedException) {
            Log.e("Error:", "joining thread")
        }
    }

    // If SpaceInvadersActivity is started then
    // start our thread.
    fun resume() {
        playing = true
        prepareLevel()
        gameThread.start()
    }

    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action and MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            // Or moved their finger while touching screen
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE-> {
                paused = false

                if (motionEvent.y > size.y - size.y / 8) { // lower eighth of screen
                    if (motionEvent.x > playerShip.position.left) { // to the right of the left edge of player
                        playerShip.moving = GameObject.right
                    } else {
                        playerShip.moving = GameObject.left
                    }
//                    soundPlayer.loopSound(SoundPlayer.playerMoveID, true)
                }

                if (motionEvent.y < size.y - size.y / 8 && playerShip.canAction1) {
                    playerShip.action1 = true
                    soundPlayer.playSound(Companion.shootID)
                }
            }

            // Player has removed finger from screen
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP -> {
                if (motionEvent.y > size.y - size.y / 10) {
                    playerShip.moving = GameObject.stopped
//                    soundPlayer.loopSound(SoundPlayer.playerMoveID, false)
                }
            }

        }
        return true
    }
}
