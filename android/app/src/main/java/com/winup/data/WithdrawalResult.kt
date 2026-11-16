package com.winup.data

sealed interface WithdrawalResult {
    data class Done(val message: String) : WithdrawalResult
    data class Failed(val message: String) : WithdrawalResult
}

fun WithdrawalDto.toResult(): WithdrawalResult {
    return when (status) {
        "PAID" -> WithdrawalResult.Done("Retiro pagado. Tu saldo ya está actualizado.")
        "PENDING_APPROVAL" -> WithdrawalResult.Done(message ?: "Tu retiro quedó en revisión.")
        "FAILED", "REJECTED" -> WithdrawalResult.Failed(
            message ?: "No se pudo procesar el retiro. El saldo fue liberado.",
        )
        else -> WithdrawalResult.Done(message ?: "Tu retiro está en proceso.")
    }
}
