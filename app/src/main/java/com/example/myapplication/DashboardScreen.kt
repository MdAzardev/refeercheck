package com.example.myapplication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.navigation.NavController

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color,
    val description: String
)

@Composable
fun DashboardScreen(navController: NavController, profile: UserProfile) {
    val context = LocalContext.current
    var showChatBlockedDialog by remember { mutableStateOf(false) }
    
    val items = listOf(
        DashboardItem(
            "Reefer Round", 
            Icons.AutoMirrored.Filled.FactCheck, 
            "stage1", 
            Color(0xFF4CAF50),
            "Start container verification rounds"
        ),
        DashboardItem(
            "Alarm Lookup", 
            Icons.Default.Warning, 
            "alarms", 
            Color(0xFFD32F2F),
            "Search Carrier/TK/Daikin/StarCool"
        ),
        DashboardItem(
            "Chat with Others", 
            Icons.AutoMirrored.Filled.Chat, 
            "chat", 
            Color(0xFF1976D2),
            "Communicate with your team"
        ),
        DashboardItem(
            "Reefer Manuals", 
            Icons.AutoMirrored.Filled.MenuBook, 
            "manuals", 
            Color(0xFFF57C00),
            "Access unit technical guides"
        ),
        DashboardItem(
            "Send Feedback", 
            Icons.Default.Feedback, 
            "feedback", 
            Color(0xFF673AB7),
            "Report bugs or suggest features"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dashboard Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome, ${profile.name.substringBefore(" ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "REEFER ASSISTANCE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Operational Excellence Dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:mrrawthereltech@gmail.com,info@mrrawthereltech.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback from RA App - ${profile.name}")
                            putExtra(Intent.EXTRA_TEXT, "User ID: ${profile.userId}\n\nPlease enter your feedback below:\n\n")
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                        } catch (e: Exception) {
                            // Handle
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send Feedback", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select an Activity",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.weight(1f)
    ) {
        items(items) { item ->
            DashboardCard(item) {
                when (item.route) {
                    "chat" -> showChatBlockedDialog = true
                    "feedback" -> {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:mrrawthereltech@gmail.com,info@mrrawthereltech.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback from RA App - ${profile.name}")
                            putExtra(Intent.EXTRA_TEXT, "User ID: ${profile.userId}\n\nFeedback:\n")
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                        } catch (_: Exception) {}
                    }
                    else -> navController.navigate(item.route)
                }
            }
        }
    }

    if (showChatBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showChatBlockedDialog = false },
            title = { Text("Feature Coming Soon") },
            text = { Text("The Team Chat and Communication features are currently under development and will be available in a future update.") },
            confirmButton = {
                Button(onClick = { showChatBlockedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
        
        // Footer/Version info
        Text(
            text = "v1.0.3 - Powered by M.R.Rawther",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun DashboardCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = item.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.description,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                color = Color.Gray
            )
        }
    }
}
