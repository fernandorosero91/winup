package com.winup.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json

class TokenStore(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "winup_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var token: String?
        get() = preferences.getString("token", null)
        set(value) {
            preferences.edit().putString("token", value).apply()
        }

    var user: UserDto?
        get() = preferences.getString("user", null)?.let {
            runCatching { json.decodeFromString<UserDto>(it) }.getOrNull()
        }
        set(value) {
            preferences.edit().putString("user", value?.let { json.encodeToString(it) }).apply()
        }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
