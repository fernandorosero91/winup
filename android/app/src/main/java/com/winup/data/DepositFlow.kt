package com.winup.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.winup.BuildConfig

sealed interface DepositResult {
    data class Checkout(val intentId: String, val url: String) : DepositResult
    data object Completed : DepositResult
    data class Failed(val message: String) : DepositResult
}

object DepositFlow {
    var pendingIntentId by mutableStateOf<String?>(null)
        private set

    fun start(intentId: String) {
        pendingIntentId = intentId
    }

    fun clear() {
        pendingIntentId = null
    }

    fun returnUrl(): String =
        BuildConfig.API_BASE_URL.removeSuffix("/") + "/api/v1/payments/return"
}
