package com.winup.data

import android.content.Context
import kotlinx.serialization.json.Json

object WinUpCache {
    private const val PREFERENCES = "winup_cache"
    private val json = Json { ignoreUnknownKeys = true }
    private var context: Context? = null

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    fun <T> read(key: String, serializer: kotlinx.serialization.DeserializationStrategy<T>): T? {
        val raw = preferences()?.getString(key, null) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    fun write(key: String, value: String) {
        preferences()?.edit()?.putString(key, value)?.apply()
    }

    fun clear() {
        preferences()?.edit()?.clear()?.apply()
    }

    private fun preferences() = context?.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
