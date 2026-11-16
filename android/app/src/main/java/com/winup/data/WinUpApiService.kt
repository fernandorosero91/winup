package com.winup.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WinUpApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("api/v1/matches")
    suspend fun matches(
        @Query("status") status: String,
        @Query("sport") sport: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): MatchPageDto

    @GET("api/v1/bets")
    suspend fun bets(): List<BetDto>

    @GET("api/v1/notifications")
    suspend fun notifications(): List<NotificationDto>

    @GET("api/v1/notifications/read")
    suspend fun readNotifications(): List<String>

    @POST("api/v1/notifications/read")
    suspend fun markNotificationsRead(@Body request: NotificationReadRequest)

    @DELETE("api/v1/notifications")
    suspend fun clearNotifications()

    @GET("api/v1/wallet/summary")
    suspend fun wallet(): WalletDto

    @GET("api/v1/wallet/movements")
    suspend fun movements(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): MovementPageDto

    @POST("api/v1/bets")
    suspend fun placeBet(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: PlaceBetRequest,
    ): BetDto

    @POST("api/v1/payments/deposits")
    suspend fun deposit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: DepositRequest,
    ): DepositDto

    @POST("api/v1/payments/withdrawals")
    suspend fun withdraw(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: WithdrawalRequest,
    ): WithdrawalDto

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: DeviceTokenRequest)

    @GET("api/v1/payments/intents/{id}")
    suspend fun depositIntent(@Path("id") id: String): DepositDto

    @POST("api/v1/dev/payments/{id}/simulate")
    suspend fun simulateDeposit(
        @retrofit2.http.Path("id") id: String,
        @Query("outcome") outcome: String,
    ): DepositDto
}
