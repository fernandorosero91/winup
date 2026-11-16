package com.winup.data

import android.os.Handler
import android.os.Looper
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object RealtimeClient {
    private val handler = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null
    private var active = false

    var onNotificationsUpdated: (() -> Unit)? = null

    fun start() {
        if (active) {
            return
        }
        active = true
        connect()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        socket?.close(1000, "logout")
        socket = null
    }

    private fun connect() {
        if (!active) {
            return
        }
        val request = Request.Builder().url(ApiClient.websocketUrl()).build()
        socket = ApiClient.client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text.contains("NOTIFICATIONS_UPDATED")) {
                handler.post { onNotificationsUpdated?.invoke() }
                return
            }
            val event = runCatching { ApiClient.decodeEvent(text) }.getOrNull() ?: return
            handler.post { WinUpData.applyRealtime(event) }
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            retry()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            retry()
        }

        private fun retry() {
            if (active) {
                handler.postDelayed({ connect() }, 5_000)
            }
        }
    }
}
