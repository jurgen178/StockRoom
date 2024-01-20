/*
 * Copyright (C) 2021
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

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.thecloudsite.stockroom.EXTRA_SYMBOL
import com.thecloudsite.stockroom.R.drawable
import com.thecloudsite.stockroom.StockDataActivity
import java.util.concurrent.atomic.AtomicInteger

object NotificationID {
    private val c: AtomicInteger = AtomicInteger(1)
    val Id: Int
        get() = c.incrementAndGet()
}

class NotificationFactory(
    val context: Context,
    title: String,
    text: String,
    symbol: String
) {
    private val notificationId: Int = NotificationID.Id

    private val intent = Intent(context, StockDataActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(EXTRA_SYMBOL, symbol)
    }

    private val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            // FLAG_IMMUTABLE required by SDK 31
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    private val notificationBuilder: Builder =
        Builder(context, NotificationChannelFactory.NotificationChannelId)
            .setSmallIcon(drawable.ic_notification_chart)
            .setContentTitle(
                title
            )
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setDefaults(
                NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

    fun sendNotification(activity: Activity) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    val requestCode = 100

                    // Display dialog to get permissions.
                    ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)

                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.

//                override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//                    when (requestCode) {
//                        PERMISSIONS_CODE -> {
//                            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                                // Permission granted
//                            } else {
//                                // Permission denied
//                            }
//                            return
//                        }
//                    }
//                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//                }
                }
                return
            }

            // NotificationId is a unique int for each notification.
            notify(notificationId, notificationBuilder.build())
        }
    }
}
