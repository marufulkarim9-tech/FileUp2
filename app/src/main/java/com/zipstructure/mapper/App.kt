kotlin
package com.zipstructure.mapper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    BUBBLE_CHANNEL_ID,
                    getString(R.string.bubble_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
        }
    }

    companion object {
        const val BUBBLE_CHANNEL_ID = "bubble_channel"
    }
}