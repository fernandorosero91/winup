package com.winup.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class WinUpFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        WinUpPush.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "WINUP"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val key = message.messageId ?: UUID.randomUUID().toString()
        WinUpNotifications.show(applicationContext, key, title, body)
    }
}
