package com.familyguard.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.familyguard.app.MainActivity
import com.familyguard.app.R
import android.app.PendingIntent
import android.content.Intent

object NotificationHelper {

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val sharingChannel = NotificationChannel(
            Constants.CHANNEL_LOCATION_SHARING,
            context.getString(R.string.notif_channel_location_sharing_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_location_sharing_desc)
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            Constants.CHANNEL_ALERTS,
            context.getString(R.string.notif_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_alerts_desc)
        }

        manager.createNotificationChannel(sharingChannel)
        manager.createNotificationChannel(alertsChannel)
    }

    fun buildLocationSharingNotification(context: Context) = NotificationCompat.Builder(
        context,
        Constants.CHANNEL_LOCATION_SHARING
    )
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle(context.getString(R.string.notif_sharing_title))
        .setContentText(context.getString(R.string.notif_sharing_text))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(mainActivityPendingIntent(context))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    fun buildUnpairedNotification(context: Context) = NotificationCompat.Builder(
        context,
        Constants.CHANNEL_ALERTS
    )
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle(context.getString(R.string.notif_unpaired_title))
        .setContentText(context.getString(R.string.notif_unpaired_text))
        .setAutoCancel(true)
        .setContentIntent(mainActivityPendingIntent(context))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    private fun mainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }
}
