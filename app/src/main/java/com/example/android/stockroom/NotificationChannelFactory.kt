package com.example.android.stockroom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationChannelFactory(val context: Context) {
  companion object {
    val NotificationChannelId = "NotificationChannelId"
  }

  init {
    val name = "Alerts"
    val descriptionText = "StockRoom Alerts"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(NotificationChannelId, name, importance).apply {
      description = descriptionText
      setShowBadge(true)
      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }

    // Register the channel with the system
    val notificationManager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }
}
