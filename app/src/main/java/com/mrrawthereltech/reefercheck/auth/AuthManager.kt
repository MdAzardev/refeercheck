package com.mrrawthereltech.reefercheck.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthManager private constructor(private val context: Context) {

    @Volatile private var cachedToken: String? = null

    init {
        // Keep an in-memory cache of the access token for synchronous use by the OkHttp interceptor
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collect { prefs ->
                cachedToken = prefs[ACCESS_TOKEN]
            }
        }
    }

    /** Synchronous read for OkHttp interceptor — backed by in-memory cache */
    fun getCachedAccessToken(): String? = cachedToken

    companion object {
        @Volatile private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager =
            instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }

        // DataStore keys
        private val ACCESS_TOKEN    = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN   = stringPreferencesKey("refresh_token")
        private val USER_ID         = intPreferencesKey("user_id")
        private val USER_EMAIL      = stringPreferencesKey("user_email")
        private val USER_PHONE      = stringPreferencesKey("user_phone")
        private val IS_ADMIN        = booleanPreferencesKey("is_admin")
        private val SUB_STATUS      = stringPreferencesKey("sub_status")
        private val DAYS_REMAINING  = intPreferencesKey("days_remaining")
        private val TRIAL_END_MS    = longPreferencesKey("trial_end_ms")   // for offline expiry check
        private val SUB_END_MS      = longPreferencesKey("sub_end_ms")     // for offline expiry check
    }

    // ── Flows ────────────────────────────────────────────────────────────────

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[ACCESS_TOKEN].isNullOrEmpty() }

    val isAdmin: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_ADMIN] ?: false }

    val userEmail: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[USER_EMAIL] ?: "" }

    val userPhone: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[USER_PHONE] ?: "" }

    val userId: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[USER_ID] ?: -1 }

    /**
     * Offline-safe subscription status.
     * Uses stored expiry timestamps compared against the device clock.
     * This works even without internet connectivity.
     */
    val subscriptionStatus: Flow<String> = context.dataStore.data.map { prefs ->
        val stored = prefs[SUB_STATUS] ?: return@map ""
        if (stored == "expired") return@map "expired"
        val now = System.currentTimeMillis()
        val trialEndMs = prefs[TRIAL_END_MS] ?: 0L
        if (stored == "trial" && trialEndMs > 0L && now > trialEndMs) return@map "expired"
        val subEndMs = prefs[SUB_END_MS] ?: 0L
        if (stored == "active" && subEndMs > 0L && now > subEndMs) return@map "expired"
        stored
    }

    val daysRemaining: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[DAYS_REMAINING] ?: 0 }

    // ── Suspend reads ────────────────────────────────────────────────────────

    suspend fun getAccessToken() = context.dataStore.data.first()[ACCESS_TOKEN]
    suspend fun getRefreshToken() = context.dataStore.data.first()[REFRESH_TOKEN]

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun saveAuthData(
        accessToken: String, refreshToken: String,
        userId: Int, email: String, phone: String, isAdmin: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN]   = accessToken
            prefs[REFRESH_TOKEN]  = refreshToken
            prefs[USER_ID]        = userId
            prefs[USER_EMAIL]     = email
            prefs[USER_PHONE]     = phone
            prefs[IS_ADMIN]       = isAdmin
        }
    }

    suspend fun saveSubscriptionStatus(
        status: String, daysRemaining: Int,
        trialEndMs: Long = 0L, subEndMs: Long = 0L
    ) {
        context.dataStore.edit { prefs ->
            prefs[SUB_STATUS]     = status
            prefs[DAYS_REMAINING] = daysRemaining
            if (trialEndMs > 0L) prefs[TRIAL_END_MS] = trialEndMs
            if (subEndMs > 0L)   prefs[SUB_END_MS]   = subEndMs
        }
    }

    suspend fun updateAccessToken(newToken: String) {
        context.dataStore.edit { prefs -> prefs[ACCESS_TOKEN] = newToken }
    }

    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }
}
