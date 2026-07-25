package com.example.myapplication

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color

@Composable
fun BottomNavigationBar(navController: NavController, notificationCount: Int) {
    val navBackStackEntry = navController.currentBackStackEntry
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { 
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = true }
                }
            },
            label = { Text("Home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
        )
        NavigationBarItem(
            selected = currentRoute?.contains("stage1") == true,
            onClick = { 
                navController.navigate("stage1") {
                    popUpTo("dashboard")
                }
            },
            label = { Text("Round") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Verification") }
        )
        NavigationBarItem(
            selected = currentRoute == "alarms",
            onClick = { navController.navigate("alarms") },
            label = { Text("Alarms") },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Alarm Lookup") }
        )
        NavigationBarItem(
            selected = currentRoute == "notifications",
            onClick = { navController.navigate("notifications") },
            label = { Text("Alerts") },
            icon = {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(notificationCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") },
            label = { Text("Profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )
    }
}
