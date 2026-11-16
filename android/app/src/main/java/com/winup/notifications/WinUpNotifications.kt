package com.winup.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.winup.MainActivity
import com.winup.R
import com.winup.data.NotificationItem

object WinUpNotifications {
    private const val CHANNEL_ID = "winup_notifications"
    private val recent = mutableMapOf<String, Long>()

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Resultados de apuestas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos de apuestas ganadas, perdidas y promociones"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build(),
            )
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context, key: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val dedupeKey = "$title|$body"
        val now = System.currentTimeMillis()
        val duplicated = synchronized(recent) {
            val previous = recent[dedupeKey]
            recent[dedupeKey] = now
            recent.entries.removeAll { now - it.value > 60_000 }
            previous != null && now - previous < 8_000
        }
        if (duplicated) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_winup)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
    }
}

object NotificationAnnouncer {
    private var lastId: String? = null

    fun announce(context: Context, items: List<NotificationItem>) {
        val latest = items.firstOrNull() ?: return
        if (lastId == null) {
            lastId = latest.id
            return
        }
        if (latest.id == lastId) {
            return
        }
        lastId = latest.id
        WinUpNotifications.show(context, latest.id, latest.title, latest.detail)
    }
}
