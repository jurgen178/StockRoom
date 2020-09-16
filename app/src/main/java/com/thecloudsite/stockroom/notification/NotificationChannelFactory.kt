/*
 * Copyright (C) 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.stockroom.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class NotificationChannelFactory(val context: Context) {
  companion object {
    const val NotificationChannelId = "NotificationChannelId"
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
