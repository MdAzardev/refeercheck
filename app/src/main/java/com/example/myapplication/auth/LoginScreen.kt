package com.example.myapplication.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.network.NetworkResult

private val DarkBg       = Color(0xFF0D1117)
private val NavyDark     = Color(0xFF0D1B2A)
private val AccentBlue   = Color(0xFF00BFFF)
private val AccentGreen  = Color(0xFF39FF14)
private val TextPrimary  = Color(0xFFE0E1DD)
private val TextSecondary = Color(0xFF778DA9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val authState    by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading    by authViewModel.isLoginLoading.collectAsStateWithLifecycle()
    var visible      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is NetworkResult.Success -> {
                authViewModel.resetAuthState()
                onLoginSuccess()
            }
            is NetworkResult.Error -> { errorMsg = s.message; authViewModel.resetAuthState() }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DarkBg, NavyDark, Color(0xFF0A1628)))
            )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo area
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "REEFER CHECK",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 3.sp
                )
                Text(
                    "Sign in to continue",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(40.dp))

                // Error banner
                AnimatedVisibility(visible = errorMsg != null) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMsg ?: "", color = Color(0xFFEF5350), fontSize = 13.sp)
                        }
                    }
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = { Text("Email Address", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = AccentBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        cursorColor = AccentBlue
                    )
                )

                Spacer(Modifier.height(14.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("Password", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentBlue) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        cursorColor = AccentBlue
                    )
                )

                Spacer(Modifier.height(28.dp))

                // Login button
                Button(
                    onClick = {
                        errorMsg = null
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "Please fill in all fields"
                            return@Button
                        }
                        authViewModel.login(email.trim(), password)
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkBg)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account?", color = TextSecondary, fontSize = 14.sp)
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Register", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "Powered by M.R.Rawther",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
