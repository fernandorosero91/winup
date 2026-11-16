package com.winup.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.winup.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = WinUpSession.token.value
            val request = if (token == null) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            val response = chain.proceed(request)
            if (response.code == 401 && token != null) {
                WinUpSession.clear()
            }
            response
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun service(): WinUpApiService = retrofit.create(WinUpApiService::class.java)

    fun websocketUrl(): String {
        val base = BuildConfig.API_BASE_URL.removeSuffix("/")
        return base.replaceFirst("https", "wss").replaceFirst("http", "ws") + "/ws/events"
    }

    fun decodeEvent(text: String): RealtimeEvent = json.decodeFromString(text)
}
