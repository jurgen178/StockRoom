package com.thecloudsite.stockroom.invaders

import android.app.Activity
import android.graphics.Point
import android.os.Bundle

// Copied from https://github.com/badut129/KInvaders

class InvadersActivity : Activity() {

  // InvadersView will be the view of the game
  // It will also hold the logic of the game
  // and respond to screen touches as well
  private var invadersView: InvadersView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Get a Display object to access screen details
    val display = windowManager.defaultDisplay
    // Load the resolution into a Point object
    val size = Point()
    display.getSize(size)

    // Initialize gameView and set it as the view
    invadersView = InvadersView(this, size)
    setContentView(invadersView)
  }

  // This method executes when the player starts the game
  // Also called after onCreate
  override fun onResume() {
    try {
      super.onResume()

      // Tell the gameView resume method to execute
      invadersView?.resume()
    } catch (e: Exception) {
    }
  }

  // This method executes when the player quits the game
  override fun onPause() {
    super.onPause()

    // Tell the gameView pause method to execute
    invadersView?.pause()
  }
}