package com.mrrawthereltech.reefercheck.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrrawthereltech.reefercheck.auth.AuthViewModel

// ─── Launch Mode Flag ────────────────────────────────────────────────
// Set to false once payment integration is live and tested.
private const val PAYMENT_ENABLED = false

// Support contact details
private const val SUPPORT_EMAIL = "support@reefercheck.app"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.mrrawthereltech.reefercheck"

// ─── Colour palette ──────────────────────────────────────────────────
private val SubDark   = Color(0xFF0D1117)
private val SubNavy   = Color(0xFF0D1B2A)
private val SubAccent = Color(0xFF00BFFF)
private val SubGold   = Color(0xFFFFD700)
private val SubText   = Color(0xFFE0E1DD)
private val SubSub    = Color(0xFF778DA9)
private val SubAmber  = Color(0xFFFFA726)
private val SubGreen  = Color(0xFF39FF14)

@Composable
fun SubscriptionScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Pulsing animation for the "coming soon" badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    val features = listOf(
        Triple(Icons.Default.AcUnit,        "Full Alarm Lookup",     "All brands: Carrier, TK, Daikin, StarCool"),
        Triple(Icons.Default.Assignment,    "Reefer Rounds",         "Full container verification workflow"),
        Triple(Icons.Default.MenuBook,      "Technical Manuals",     "Access all reefer unit guides"),
        Triple(Icons.Default.Notifications, "Instant Notifications", "Real-time alarm & status alerts"),
        Triple(Icons.Default.Chat,          "Team Chat",             "Communicate with your crew"),
        Triple(Icons.Default.CloudSync,     "Offline Support",       "Works without internet connection"),
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -40 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // ── Lock icon ─────────────────────────────────────────
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

                    Text(
                        "Your Free Trial Has Ended",
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SubText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Reefer Check Pro is required to continue.\nSubscription payments are coming very soon!",
                        fontSize = 14.sp, color = SubSub, textAlign = TextAlign.Center, lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── "Coming Soon" notice banner ───────────────────────
                    Surface(
                        color = SubAmber.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubAmber.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info, null,
                                tint = SubAmber,
                                modifier = Modifier.size(22.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Payment Integration — Coming Soon",
                                    fontWeight = FontWeight.Bold, color = SubAmber, fontSize = 14.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "We are actively working on enabling in-app payments. " +
                                    "If you are facing any issues or need an extension, " +
                                    "please contact our support team and we will sort it out immediately.",
                                    color = SubText.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Price card ────────────────────────────────────────
                    Surface(
                        color = SubAccent.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SubAccent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // "Coming soon" badge
                            Surface(
                                color = SubAmber.copy(alpha = pulse * 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "  PAYMENT COMING SOON  ",
                                    fontSize = 10.sp, color = SubAmber,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("MONTHLY PLAN", fontSize = 11.sp, color = SubAccent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("₹200", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = SubText)
                                Text(" / month", fontSize = 16.sp, color = SubSub, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Text("Full unlimited access to all features", fontSize = 12.sp, color = SubSub)
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── Feature list ──────────────────────────────────────
                    Text(
                        "What's Included",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SubText,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    features.forEach { (icon, title, desc) ->
                        FeatureRow(icon = icon, title = title, description = desc)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Disabled Pay button (shows "Coming Soon") ─────────
                    if (!PAYMENT_ENABLED) {
                        Button(
                            onClick = { /* payments not yet live */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF003087).copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.55f)
                            )
                        ) {
                            Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pay with PayPal — Coming Soon", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(14.dp))

                        // ── Contact Support button ─────────────────────────
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$SUPPORT_EMAIL")
                                    putExtra(Intent.EXTRA_SUBJECT, "Reefer Check - Subscription / Support Request")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Hi Reefer Check Team,\n\nI need assistance with my subscription.\n\nRegistered Email: \nIssue: \n\nThank you."
                                    )
                                }
                                try { context.startActivity(Intent.createChooser(intent, "Send email via...")) }
                                catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SubGreen.copy(alpha = 0.85f),
                                contentColor = Color(0xFF0D1117)
                            )
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Check for App Update button ────────────────────
                        OutlinedButton(
                            onClick = {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))) }
                                catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SubAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SubAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check for App Update", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
