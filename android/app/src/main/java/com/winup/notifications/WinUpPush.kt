package com.winup.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.winup.data.DeviceTokenRequest
import com.winup.data.WinUpApiService
import com.winup.data.WinUpSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WinUpPush {
    private const val PREFERENCES = "winup_push"
    private const val TOKEN_KEY = "fcm_token"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = mutableSetOf<String>()

    fun register(context: Context, api: WinUpApiService) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            storeAndSend(context, task.result, api)
        }
    }

    fun onNewToken(context: Context, token: String) {
        if (token.isBlank() || !WinUpSession.isReady() || WinUpSession.token.value == null) {
            return
        }
        storeAndSend(context, token, WinUpSession.api)
    }

    private fun storeAndSend(context: Context, token: String, api: WinUpApiService) {
        if (token.isBlank()) {
            return
        }
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
        if (!WinUpSession.isReady() || WinUpSession.token.value == null) {
            return
        }
        synchronized(inFlight) {
            if (!inFlight.add(token)) {
                return
            }
        }
        scope.launch {
            try {
                runCatching { api.registerDevice(DeviceTokenRequest(token = token)) }
            } finally {
                synchronized(inFlight) {
                    inFlight.remove(token)
                }
            }
        }
    }
}
