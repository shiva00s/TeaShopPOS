package com.teashop.pos.api

import com.teashop.pos.data.entity.Order
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.data.entity.Shop
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // --- SYNC PUSH ---
    @POST("sync/shops")
    suspend fun pushShops(@Body shops: List<Shop>): Response<Unit>

    @POST("sync/orders")
    suspend fun pushOrders(@Body orders: List<Order>): Response<Unit>

    @POST("sync/cashbook")
    suspend fun pushCashbook(@Body entries: List<Cashbook>): Response<Unit>

    // --- SYNC PULL (For Owner Dashboard) ---
    @GET("sync/all-shops-summary")
    suspend fun getGlobalSummary(): Response<GlobalSummaryResponse>
}

data class LoginRequest(val username: String, val password: String, val deviceId: String)
data class AuthResponse(val token: String, val ownerId: String)
data class GlobalSummaryResponse(
    val totalSalesToday: Double,
    val cashInHand: Double,
    val pendingSalaries: Double,
    val shopSummaries: List<ShopSummary>
)
data class ShopSummary(val shopId: String, val name: String, val todaySales: Double)
