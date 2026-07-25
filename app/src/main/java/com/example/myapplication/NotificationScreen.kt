package com.example.myapplication

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun NotificationScreen(
    context: Context,
    navController: NavController,
    verificationList: List<Verification>,
    onUpdate: (Verification, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val filteredList by remember(verificationList) {
        derivedStateOf {
            verificationList.filter { 
                it.status == "Not in Range" || it.status == "Alarm" || it.status == "Recheck" 
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Notifications", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.containerNumber }
                ) { item ->
                    val cardColor = when (item.status) {
                        "Alarm" -> Color(0xFFFFCDD2)
                        "Not in Range" -> Color(0xFFFFF9C4)
                        "Recheck" -> Color(0xFFE1F5FE)
                        else -> Color(0xFFC8E6C9)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Container: ${item.containerNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (item.status == "Recheck") {
                                    Surface(color = Color(0xFF0288D1), shape = MaterialTheme.shapes.extraSmall) {
                                        Text("ACTION REQUIRED", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Temp: ${item.actualTemp}", fontSize = 13.sp)
                                Text("Status: ${item.status}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            if (item.remark.isNotEmpty()) {
                                Text("Remark: ${item.remark}", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { 
                                        scope.launch {
                                            onUpdate(item, "In Range")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) { 
                                    Text("Mark In Range", fontSize = 12.sp) 
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            val containerNum = item.containerNumber
                                            // Navigation first ensures UI transition starts before state changes
                                            navController.navigate("stage1?containerNumber=${Uri.encode(containerNum)}")
                                            onUpdate(item, "Recheck")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) { 
                                    Text("Verify Now", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
