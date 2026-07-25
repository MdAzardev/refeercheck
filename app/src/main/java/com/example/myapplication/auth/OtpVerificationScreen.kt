package com.example.myapplication.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.network.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OtpDark    = Color(0xFF0D1117)
private val OtpNavy    = Color(0xFF0D1B2A)
private val OtpAccent  = Color(0xFF00BFFF)
private val OtpGreen   = Color(0xFF39FF14)
private val OtpText    = Color(0xFFE0E1DD)
private val OtpSubText = Color(0xFF778DA9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    authViewModel: AuthViewModel,
    email: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    var otp         by remember { mutableStateOf("") }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var successMsg  by remember { mutableStateOf<String?>(null) }
    var resendTimer by remember { mutableIntStateOf(0) }
    val scope       = rememberCoroutineScope()

    val otpState by authViewModel.otpState.collectAsStateWithLifecycle()

    LaunchedEffect(otpState) {
        when (val s = otpState) {
            is NetworkResult.Success -> {
                successMsg = s.data
                authViewModel.resetOtpState()
                delay(1200L)
                onVerified()
            }
            is NetworkResult.Error -> { errorMsg = s.message; authViewModel.resetOtpState() }
            else -> {}
        }
    }

    // Resend countdown
    LaunchedEffect(resendTimer) {
        if (resendTimer > 0) {
            delay(1000L)
            resendTimer--
        }
    }

    val isLoading = otpState is NetworkResult.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OtpDark, OtpNavy, Color(0xFF0A1628))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OtpAccent)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Email icon
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = OtpAccent.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MarkEmailUnread, null, tint = OtpAccent, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Check Your Email", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OtpText)
            Spacer(Modifier.height(8.dp))
            Text(
                "We sent a 6-digit verification code to\n$email",
                fontSize = 13.sp, color = OtpSubText,
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )

            Spacer(Modifier.height(36.dp))

            // Error / success banners
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
            AnimatedVisibility(visible = successMsg != null) {
                Surface(
                    color = OtpGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = OtpGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(successMsg ?: "", color = OtpGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // OTP input
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { otp = it; errorMsg = null } },
                label = { Text("6-Digit Code", color = OtpSubText) },
                leadingIcon = { Icon(Icons.Default.Pin, null, tint = OtpAccent) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OtpText, unfocusedTextColor = OtpText,
                    focusedBorderColor = OtpAccent, unfocusedBorderColor = OtpSubText.copy(alpha = 0.4f),
                    cursorColor = OtpAccent
                )
            )

            // Character counter
            Text(
                "${otp.length}/6",
                color = if (otp.length == 6) OtpGreen else OtpSubText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
                textAlign = TextAlign.End
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    errorMsg = null
                    if (otp.length < 6) { errorMsg = "Please enter the 6-digit code"; return@Button }
                    authViewModel.verifyOtp(email, otp)
                },
                enabled = !isLoading && otp.length == 6,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OtpAccent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = OtpDark, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.VerifiedUser, null, tint = OtpDark, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OtpDark)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Resend OTP
            if (resendTimer > 0) {
                Text("Resend code in ${resendTimer}s", color = OtpSubText, fontSize = 13.sp)
            } else {
                TextButton(onClick = {
                    resendTimer = 60
                    authViewModel.resendOtp(email) { ok, msg ->
                        if (!ok) errorMsg = msg
                    }
                }) {
                    Icon(Icons.Default.Refresh, null, tint = OtpAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Resend OTP", color = OtpAccent, fontSize = 14.sp)
                }
            }
        }
    }
}
