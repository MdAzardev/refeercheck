package com.example.myapplication

// ─── Existing local models (do NOT remove) ───────────────────────────────────

data class ContainerData(
    val position: String,
    val containerNumber: String,
    val setTemp: String,
    val humidity: String,
    val vent: String,
    val pol: String,
    val pod: String,
    val opr: String,
    val reeferType: String = "",
)

data class Verification(
    val containerNumber: String,
    val actualTemp: String,
    val actualHumidity: String,
    val remark: String,
    val status: String,
    val alarmCode: String = "",
    val alarmDescription: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null
)

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList()
)

// ─── API / Backend models ─────────────────────────────────────────────────────

/** Generic API envelope returned by all backend endpoints */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

/** Backend user record (from /api/user/me and auth responses) */
data class AuthUser(
    val id: Int,
    val email: String,
    val phone: String?,
    val isVerified: Boolean,
    val isAdmin: Boolean,
    val createdAt: String
)

/** Subscription info returned by /api/user/subscription */
data class SubscriptionInfo(
    val status: String,          // "trial" | "active" | "expired"
    val trialStart: String?,
    val trialEnd: String?,
    val subscriptionStart: String?,
    val subscriptionEnd: String?,
    val daysRemaining: Int,
    val plan: String?
)

// ─── Auth request/response bodies ────────────────────────────────────────────

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val email: String, val phone: String, val password: String)

data class OtpRequest(val email: String, val otp: String)

data class ResendOtpRequest(val email: String)

data class RefreshRequest(val refreshToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser
)

// ─── Payment response bodies ──────────────────────────────────────────────────

data class StripeSessionResponse(val sessionId: String, val url: String)

data class PaypalOrderResponse(val orderId: String, val approveUrl: String? = null)

// ─── Admin models ─────────────────────────────────────────────────────────────

data class AdminUserItem(
    val id: Int,
    val email: String,
    val phone: String?,
    val isVerified: Boolean,
    val isAdmin: Boolean,
    val createdAt: String,
    val subscription: SubscriptionInfo?
)

data class AdminUsersResponse(
    val users: List<AdminUserItem>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class AdminStats(
    val totalUsers: Int,
    val activeSubscriptions: Int,
    val trialUsers: Int,
    val expiredUsers: Int,
    val unverifiedUsers: Int
)
