package com.example.myapplication.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.auth.AuthViewModel
import com.example.myapplication.network.ApiClient
import kotlinx.coroutines.launch

private val SubDark   = Color(0xFF0D1117)
private val SubNavy   = Color(0xFF0D1B2A)
private val SubAccent = Color(0xFF00BFFF)
private val SubGold   = Color(0xFFFFD700)
private val SubText   = Color(0xFFE0E1DD)
private val SubSub    = Color(0xFF778DA9)

@Composable
fun SubscriptionScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isStripeLoading  by remember { mutableStateOf(false) }
    var isPaypalLoading  by remember { mutableStateOf(false) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var visible          by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val features = listOf(
        Triple(Icons.Default.AcUnit,           "Full Alarm Lookup",         "All brands: Carrier, TK, Daikin, StarCool"),
        Triple(Icons.Default.Assignment,        "Reefer Rounds",             "Full container verification workflow"),
        Triple(Icons.Default.MenuBook,          "Technical Manuals",         "Access all reefer unit guides"),
        Triple(Icons.Default.Notifications,     "Instant Notifications",     "Real-time alarm & status alerts"),
        Triple(Icons.Default.Chat,              "Team Chat",                 "Communicate with your crew"),
        Triple(Icons.Default.CloudSync,         "Offline Support",           "Works without internet connection"),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SubDark, SubNavy, Color(0xFF0A1628))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Lock icon
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = SubGold.copy(alpha = 0.12f),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = SubGold, modifier = Modifier.size(50.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Subscription Required", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = SubText)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your trial has ended. Upgrade to continue\naccessing all Reefer Check features.",
                        fontSize = 14.sp, color = SubSub, textAlign = TextAlign.Center, lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(28.dp))

                    // Price card
                    Surface(
                        color = SubAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ANNUAL PLAN", fontSize = 11.sp, color = SubAccent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$99", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = SubText)
                                Text(" / year", fontSize = 16.sp, color = SubSub, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Text("≈ $8.25/month — less than a meal", fontSize = 12.sp, color = SubSub)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Feature list
                    Text("What's Included", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SubText, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    features.forEach { (icon, title, desc) ->
                        FeatureRow(icon = icon, title = title, description = desc)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    // Error
                    AnimatedVisibility(visible = errorMsg != null) {
                        Surface(
                            color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(errorMsg ?: "", color = Color(0xFFEF5350), fontSize = 13.sp)
                            }
                        }
                    }

                    // Stripe button
                    Button(
                        onClick = {
                            isStripeLoading = true
                            errorMsg = null
                            scope.launch {
                                try {
                                    val resp = ApiClient.api.createStripeSession()
                                    if (resp.isSuccessful && resp.body()?.success == true) {
                                        val url = resp.body()?.data?.url
                                        if (!url.isNullOrEmpty()) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    } else {
                                        errorMsg = resp.body()?.message ?: "Failed to create payment session"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Network error"
                                } finally {
                                    isStripeLoading = false
                                }
                            }
                        },
                        enabled = !isStripeLoading && !isPaypalLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SubAccent)
                    ) {
                        if (isStripeLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SubDark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CreditCard, null, tint = SubDark, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pay with Card (Stripe)", fontWeight = FontWeight.Bold, color = SubDark)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // PayPal button
                    OutlinedButton(
                        onClick = {
                            isPaypalLoading = true
                            errorMsg = null
                            scope.launch {
                                try {
                                    val resp = ApiClient.api.createPaypalOrder()
                                    if (resp.isSuccessful && resp.body()?.success == true) {
                                        // In production, open PayPal approval URL; for now show order ID
                                        val orderId = resp.body()?.data?.orderId
                                        errorMsg = "PayPal Order created: $orderId\nComplete payment in your PayPal app."
                                    } else {
                                        errorMsg = resp.body()?.message ?: "Failed to create PayPal order"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Network error"
                                } finally {
                                    isPaypalLoading = false
                                }
                            }
                        },
                        enabled = !isStripeLoading && !isPaypalLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF003087)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF009CDE))
                    ) {
                        if (isPaypalLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF003087), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF009CDE), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pay with PayPal", fontWeight = FontWeight.Bold, color = Color(0xFF009CDE))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, null, tint = SubSub, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = SubSub, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SubAccent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = SubAccent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = SubText, fontSize = 13.sp)
                Text(description, color = SubSub, fontSize = 11.sp)
            }
        }
    }
}
