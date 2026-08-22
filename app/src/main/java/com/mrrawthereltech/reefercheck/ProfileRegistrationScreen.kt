package com.mrrawthereltech.reefercheck

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRegistrationScreen(onProfileCreated: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var registrationMethod by remember { mutableStateOf<String?>(null) } // "google" or "phone"
    
    var isOtpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isInitialFormValid = name.isNotBlank() && (
        (registrationMethod == "google" && email.contains("@")) ||
        (registrationMethod == "phone" && phone.length >= 8)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandingHeader()
            
            Spacer(modifier = Modifier.height(32.dp))

            if (registrationMethod == null) {
                Text(
                    text = "Welcome to Reefer Assist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Please choose a registration method to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { registrationMethod = "google" },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Google / Gmail")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { registrationMethod = "phone" },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Phone Number")
                }
            } else if (!isOtpSent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { registrationMethod = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text(
                        text = "Complete Your Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (registrationMethod == "google") {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Gmail Address") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Business, null) },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        isOtpSent = true
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "OTP sent! For demo use: 123456",
                                duration = SnackbarDuration.Long
                            )
                        }
                    },
                    enabled = isInitialFormValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Verification Code", fontWeight = FontWeight.Bold)
                }
            } else {
                // OTP Verification Step
                Text(
                    text = "Verify Your Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter the 6-digit code sent to ${if(registrationMethod=="google") email else phone}\n(Demo Mode: use 123456)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    label = { Text("6-Digit Code") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("123456") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isVerifying) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { 
                            isVerifying = true
                            scope.launch {
                                delay(1500L) // Simulate network delay
                                if (otpCode == "123456" || otpCode.length == 6) { // Accept any 6 digits for simulation
                                    val newId = "RA-${System.currentTimeMillis().toString().takeLast(6)}"
                                    onProfileCreated(UserProfile(userId = newId, name = name, email = email, phone = phone, company = company))
                                } else {
                                    snackbarHostState.showSnackbar("Invalid code. Please try again.")
                                    isVerifying = false
                                }
                            }
                        },
                        enabled = otpCode.length == 6,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Register", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { isOtpSent = false; otpCode = "" },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Change Details")
                    }
                }
            }
        }
    }
}
