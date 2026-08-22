package com.mrrawthereltech.reefercheck.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrrawthereltech.reefercheck.network.NetworkResult

private val DarkBg2      = Color(0xFF0D1117)
private val NavyDark2    = Color(0xFF0D1B2A)
private val AccentBlue2  = Color(0xFF00BFFF)
private val TextPrimary2 = Color(0xFFE0E1DD)
private val TextSec2     = Color(0xFF778DA9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegistered: (String) -> Unit   // passes email to OTP screen
) {
    var email       by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var visible     by remember { mutableStateOf(false) }

    val registerState by authViewModel.registerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(registerState) {
        when (val s = registerState) {
            is NetworkResult.Success -> {
                authViewModel.resetRegisterState()
                onRegistered(email.trim())
            }
            is NetworkResult.Error -> {
                errorMsg = s.message
                authViewModel.resetRegisterState()
            }
            else -> {}
        }
    }

    val isLoading = registerState is NetworkResult.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg2, NavyDark2, Color(0xFF0A1628))))
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentBlue2)
                    }
                    Text(
                        "Create Account",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary2,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Register to start your 30-day free trial",
                    fontSize = 13.sp,
                    color = TextSec2,
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                )

                Spacer(Modifier.height(28.dp))

                // Trial badge
                Surface(
                    color = Color(0xFF39FF14).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF39FF14), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("30-Day Free Trial", fontWeight = FontWeight.Bold, color = Color(0xFF39FF14), fontSize = 13.sp)
                            Text("Full access. No credit card required.", color = TextSec2, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Error banner
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

                // Fields
                listOf<@Composable () -> Unit>(
                    {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = null },
                            label = { Text("Email Address", color = TextSec2) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = AccentBlue2) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    },
                    {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMsg = null },
                            label = { Text("Phone Number", color = TextSec2) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = AccentBlue2) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    },
                    {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = null },
                            label = { Text("Password (min 6 chars)", color = TextSec2) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentBlue2) },
                            trailingIcon = {
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSec2)
                                }
                            },
                            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    },
                    {
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it; errorMsg = null },
                            label = { Text("Confirm Password", color = TextSec2) },
                            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = AccentBlue2) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    }
                ).forEachIndexed { i, field ->
                    field()
                    if (i < 3) Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        errorMsg = null
                        when {
                            email.isBlank() || phone.isBlank() || password.isBlank() ->
                                errorMsg = "All fields are required"
                            !email.contains("@") ->
                                errorMsg = "Please enter a valid email address"
                            phone.length < 7 ->
                                errorMsg = "Please enter a valid phone number"
                            password.length < 6 ->
                                errorMsg = "Password must be at least 6 characters"
                            password != confirmPass ->
                                errorMsg = "Passwords do not match"
                            else -> authViewModel.register(email.trim(), phone.trim(), password)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue2,
                        disabledContainerColor = AccentBlue2.copy(alpha = 0.7f),
                        disabledContentColor = DarkBg2
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBg2, strokeWidth = 3.dp)
                    } else {
                        Text("Register & Get OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkBg2)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", color = TextSec2, fontSize = 14.sp)
                    TextButton(onClick = onNavigateBack) {
                        Text("Sign In", color = AccentBlue2, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary2,
    unfocusedTextColor = TextPrimary2,
    focusedBorderColor = AccentBlue2,
    unfocusedBorderColor = TextSec2.copy(alpha = 0.4f),
    cursorColor = AccentBlue2
)
