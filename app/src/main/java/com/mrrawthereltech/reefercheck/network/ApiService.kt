package com.mrrawthereltech.reefercheck.network

import com.mrrawthereltech.reefercheck.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<Any>>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<ApiResponse<Any>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<ApiResponse<Map<String, String>>>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    // ── User ─────────────────────────────────────────────────────────────────
    @GET("api/user/me")
    suspend fun getMe(): Response<ApiResponse<AuthUser>>

    @GET("api/user/subscription")
    suspend fun getSubscription(): Response<ApiResponse<SubscriptionInfo>>

    @DELETE("api/user/me")
    suspend fun deleteMe(): Response<ApiResponse<Any>>

    // ── Payment ───────────────────────────────────────────────────────────────
    @POST("api/payment/stripe/create-session")
    suspend fun createStripeSession(): Response<ApiResponse<StripeSessionResponse>>

    @POST("api/payment/paypal/create-order")
    suspend fun createPaypalOrder(): Response<ApiResponse<PaypalOrderResponse>>

    @POST("api/payment/paypal/capture/{orderId}")
    suspend fun capturePaypalOrder(@Path("orderId") orderId: String): Response<ApiResponse<Any>>

    // ── Admin ─────────────────────────────────────────────────────────────────
    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiResponse<AdminUsersResponse>>

    @GET("api/admin/stats")
    suspend fun getAdminStats(): Response<ApiResponse<AdminStats>>

    @PATCH("api/admin/users/{id}/subscription")
    suspend fun updateAdminSubscription(
        @Path("id") userId: Int,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Any>>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteUser(@Path("id") userId: Int): Response<ApiResponse<Any>>
}
