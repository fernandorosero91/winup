package com.winup.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object WinUpSession {
    private lateinit var store: TokenStore
    lateinit var api: WinUpApiService

    val token = mutableStateOf<String?>(null)
    val user = mutableStateOf<UserDto?>(null)

    fun init(context: Context) {
        if (::store.isInitialized) {
            return
        }
        store = TokenStore(context.applicationContext)
        api = ApiClient.service()
        token.value = store.token
        user.value = store.user
    }

    fun save(response: AuthResponse) {
        store.token = response.token
        store.user = response.user
        token.value = response.token
        user.value = response.user
    }

    fun clear() {
        if (::store.isInitialized) {
            store.clear()
        }
        token.value = null
        user.value = null
        WinUpData.clear()
        RealtimeClient.stop()
    }
}
