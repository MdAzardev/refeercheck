package com.example.myapplication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    profile: UserProfile,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onAdminPortal: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    val isAdmin       by authViewModel.isAdmin.collectAsStateWithLifecycle(false)
    val subStatus     by authViewModel.subscriptionStatus.collectAsStateWithLifecycle("")
    val daysRemaining by authViewModel.daysRemaining.collectAsStateWithLifecycle(0)
    val userEmail     by authViewModel.userEmail.collectAsStateWithLifecycle("")
    val userPhone     by authViewModel.userPhone.collectAsStateWithLifecycle("")

    var showDeleteDialog by remember { mutableStateOf(false) }

    val displayEmail = userEmail.ifEmpty { profile.email }
    val displayPhone = userPhone.ifEmpty { profile.phone }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (profile.name.isNotEmpty()) profile.name else displayEmail.substringBefore("@"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (profile.company.isNotEmpty()) {
                    Text(profile.company, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(Modifier.height(12.dp))

                // Subscription badge
                val (badgeColor, badgeText) = when (subStatus) {
                    "active"  -> Pair(Color(0xFF4CAF50), "✓ Active Subscription")
                    "trial"   -> Pair(Color(0xFFFF9800), "⏳ Trial — $daysRemaining days left")
                    "expired" -> Pair(Color(0xFFF44336), "✗ Subscription Expired")
                    else      -> Pair(Color.Gray, "Loading...")
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile info items
            if (displayEmail.isNotEmpty()) {
                ProfileInfoItem("Email", displayEmail, Icons.Default.Email)
            }
            if (displayPhone.isNotEmpty()) {
                ProfileInfoItem("Phone Number", displayPhone, Icons.Default.Phone)
            }
            if (profile.userId.isNotEmpty()) {
                ProfileInfoItem("User ID", profile.userId, Icons.Default.Fingerprint)
            }
            ProfileInfoItem(
                "Member Since",
                dateFormat.format(Date(profile.registrationDate)),
                Icons.Default.CalendarToday
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Admin Portal (only for admins)
            if (isAdmin) {
                Button(
                    onClick = onAdminPortal,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Admin Portal", fontWeight = FontWeight.Bold)
                }
            }

            // Send Feedback
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:mrrawthereltech@gmail.com,info@mrrawthereltech.com")
                        putExtra(Intent.EXTRA_SUBJECT, "App Feedback - ${displayEmail} (${profile.userId})")
                    }
                    try { context.startActivity(Intent.createChooser(intent, "Send Feedback")) } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Feedback, null)
                Spacer(Modifier.width(8.dp))
                Text("Send App Feedback")
            }

            Spacer(Modifier.height(4.dp))

            // Delete Account
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Account")
            }

            // Logout
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            Text(
                text = "v1.1.0 — Powered by M.R.Rawther",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Confirm delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and all data. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount { ok, msg -> if (ok) onLogout() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = value, fontSize = 15.sp, color = Color.Black)
            }
        }
    }
}
