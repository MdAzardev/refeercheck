package com.mrrawthereltech.reefercheck.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrrawthereltech.reefercheck.*
import com.mrrawthereltech.reefercheck.network.ApiClient
import com.mrrawthereltech.reefercheck.network.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AuthViewModel(private val context: Context) : ViewModel() {

    private val authManager = AuthManager.getInstance(context)
    private val api = ApiClient.api
    private val gson = Gson()

    // ── Exposed flows ────────────────────────────────────────────────────────
    val isLoggedIn        = authManager.isLoggedIn
    val isAdmin           = authManager.isAdmin
    val subscriptionStatus = authManager.subscriptionStatus
    val daysRemaining     = authManager.daysRemaining
    val userEmail         = authManager.userEmail
    val userPhone         = authManager.userPhone

    // ── State flows ──────────────────────────────────────────────────────────
    private val _registerState = MutableStateFlow<NetworkResult<String>?>(null)
    val registerState: StateFlow<NetworkResult<String>?> = _registerState

    private val _otpState = MutableStateFlow<NetworkResult<String>?>(null)
    val otpState: StateFlow<NetworkResult<String>?> = _otpState

    private val _authState = MutableStateFlow<NetworkResult<AuthResponse>?>(null)
    val authState: StateFlow<NetworkResult<AuthResponse>?> = _authState

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading

    private val _subscriptionInfo = MutableStateFlow<SubscriptionInfo?>(null)
    val subscriptionInfo: StateFlow<SubscriptionInfo?> = _subscriptionInfo

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseErrorMessage(response: retrofit2.Response<*>): String {
        return try {
            val errJson = response.errorBody()?.string()
            if (!errJson.isNullOrEmpty()) {
                val parsed = gson.fromJson(errJson, ApiResponse::class.java)
                parsed?.message ?: "Request failed (${response.code()})"
            } else {
                "Request failed (${response.code()})"
            }
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }

    private fun parseDateMs(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {}
        }
        return 0L
    }

    // ── Auth actions ─────────────────────────────────────────────────────────

    fun register(email: String, phone: String, password: String) {
        viewModelScope.launch {
            _registerState.value = NetworkResult.Loading
            try {
                val response = api.register(RegisterRequest(email, phone, password))
                _registerState.value = if (response.isSuccessful && response.body()?.success == true) {
                    NetworkResult.Success(response.body()?.message ?: "OTP sent to $email")
                } else {
                    NetworkResult.Error(response.body()?.message ?: parseErrorMessage(response))
                }
            } catch (e: Exception) {
                _registerState.value = NetworkResult.Error(e.message ?: "Network error. Check your connection.")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _otpState.value = NetworkResult.Loading
            try {
                val response = api.verifyOtp(OtpRequest(email, otp))
                if (response.isSuccessful) {
                    val body = response.body()
                    val authData = body?.data
                    if (body?.success == true && authData != null) {
                        authManager.saveAuthData(
                            authData.accessToken, authData.refreshToken,
                            authData.user.id, authData.user.email,
                            authData.user.phone ?: "", authData.user.isAdmin
                        )
                        // Save trial subscription (30 days from now)
                        val trialEndMs = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                        authManager.saveSubscriptionStatus("trial", 30, trialEndMs = trialEndMs)
                        _otpState.value = NetworkResult.Success("Verified! Your 30-day trial has started.")
                    } else {
                        _otpState.value = NetworkResult.Error(body?.message ?: "Invalid OTP")
                    }
                } else {
                    _otpState.value = NetworkResult.Error(parseErrorMessage(response))
                }
            } catch (e: Exception) {
                _otpState.value = NetworkResult.Error(e.message ?: "Network error. Check your connection.")
            }
        }
    }

    fun resendOtp(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.resendOtp(ResendOtpRequest(email))
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "OTP resent successfully")
                } else {
                    onResult(false, parseErrorMessage(response))
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Network error")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoginLoading.value = true
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val authData = body?.data
                    if (body?.success == true && authData != null) {
                        authManager.saveAuthData(
                            authData.accessToken, authData.refreshToken,
                            authData.user.id, authData.user.email,
                            authData.user.phone ?: "", authData.user.isAdmin
                        )
                        fetchSubscription()
                        _authState.value = NetworkResult.Success(authData)
                    } else {
                        _authState.value = NetworkResult.Error(body?.message ?: "Login failed")
                    }
                } else {
                    _authState.value = NetworkResult.Error(parseErrorMessage(response))
                }
            } catch (e: Exception) {
                _authState.value = NetworkResult.Error(e.message ?: "Network error. Check your connection.")
            } finally {
                _isLoginLoading.value = false
            }
        }
    }

    fun fetchSubscription() {
        viewModelScope.launch {
            try {
                val response = api.getSubscription()
                if (response.isSuccessful) {
                    val sub = response.body()?.data
                    if (sub != null) {
                        _subscriptionInfo.value = sub
                        authManager.saveSubscriptionStatus(
                            sub.status, sub.daysRemaining,
                            parseDateMs(sub.trialEnd),
                            parseDateMs(sub.subscriptionEnd)
                        )
                    }
                }
            } catch (_: Exception) {
                // Silently fail — offline mode uses cached values
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            authManager.logout()
        }
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteMe()
                if (response.isSuccessful && response.body()?.success == true) {
                    authManager.logout()
                    onResult(true, "Account deleted successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to delete account")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Network error")
            }
        }
    }

    // ── State resets ─────────────────────────────────────────────────────────

    fun resetRegisterState() { _registerState.value = null }
    fun resetOtpState()      { _otpState.value = null }
    fun resetAuthState()     { _authState.value = null }
}
