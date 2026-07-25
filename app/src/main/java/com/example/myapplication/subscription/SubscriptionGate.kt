package com.example.myapplication.subscription

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.auth.AuthViewModel

/**
 * Wraps the entire main app content.
 * - "trial" or "active" → shows content normally
 * - "expired"            → shows paywall (SubscriptionScreen)
 * - ""  (loading/empty)  → shows nothing until status resolves
 */
@Composable
fun SubscriptionGate(
    authViewModel: AuthViewModel,
    content: @Composable () -> Unit
) {
    val subscriptionStatus by authViewModel.subscriptionStatus.collectAsStateWithLifecycle("")

    when (subscriptionStatus) {
        "active", "trial" -> content()
        "expired"         -> SubscriptionScreen(authViewModel = authViewModel)
        // empty string = still loading from DataStore, show nothing
    }
}
