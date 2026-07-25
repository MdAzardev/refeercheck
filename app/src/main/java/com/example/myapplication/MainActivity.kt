package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainActivity : ComponentActivity() {
    private val gson = Gson()
    private val verificationFile by lazy { File(filesDir, "verification_data.json") }
    private val containerFile by lazy { File(filesDir, "container_data.json") }
    private val fileNameFile by lazy { File(filesDir, "imported_file_name.txt") }
    private val profileFile by lazy { File(filesDir, "user_profile.json") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load Alarm Database from disk on startup
        AlarmCodeProvider.loadData(this)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var userProfile by remember { mutableStateOf<UserProfile?>(null) }
            var isLoadingProfile by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                if (profileFile.exists()) {
                    try {
                        val json = profileFile.readText()
                        userProfile = gson.fromJson(json, UserProfile::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                isLoadingProfile = false
            }

            if (showSplash || isLoadingProfile) {
                SplashScreen(onAnimationFinished = { showSplash = false })
            } else if (userProfile == null) {
                ProfileRegistrationScreen(onProfileCreated = { profile ->
                    userProfile = profile
                    try {
                        profileFile.writeText(gson.toJson(profile))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            } else {
                MainContent(userProfile!!, onLogout = {
                    if (profileFile.exists()) profileFile.delete()
                    userProfile = null
                })
            }
        }
    }

    @Composable
    fun MainContent(profile: UserProfile, onLogout: () -> Unit) {
        val navController = rememberNavController()
        val verificationList = remember { mutableStateListOf<Verification>() }
        val containerList = remember { mutableStateOf<List<ContainerData>>(emptyList()) }
        val importedFileName = remember { mutableStateOf("") }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val notificationCount = verificationList.count { 
            it.status == "Not in Range" || it.status == "Alarm" || it.status == "Recheck" 
        }

        LaunchedEffect(Unit) {
            if (verificationFile.exists()) {
                try {
                    val json = verificationFile.readText()
                    val type = object : TypeToken<List<Verification>>() {}.type
                    val list: List<Verification> = gson.fromJson(json, type)
                    verificationList.addAll(list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (containerFile.exists()) {
                try {
                    val json = containerFile.readText()
                    val type = object : TypeToken<List<ContainerData>>() {}.type
                    val list: List<ContainerData> = gson.fromJson(json, type)
                    containerList.value = list
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (fileNameFile.exists()) {
                importedFileName.value = fileNameFile.readText()
            }
        }

        val saveVerification = {
            try {
                val json = gson.toJson(verificationList.toList())
                verificationFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val saveContainers = {
            try {
                val json = gson.toJson(containerList.value)
                containerFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Scaffold(
            bottomBar = { 
                if (currentRoute != "dashboard") {
                    BottomNavigationBar(navController, notificationCount) 
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardScreen(navController, profile)
                }
                composable(
                    "stage1?containerNumber={containerNumber}",
                    arguments = listOf(navArgument("containerNumber") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null 
                    })
                ) { backStackEntry ->
                    val containerNumber = backStackEntry.arguments?.getString("containerNumber")
                    Stage1Screen(
                        context = this@MainActivity,
                        verificationList = verificationList,
                        containerList = containerList.value,
                        navController = navController,
                        importedFileName = importedFileName.value,
                        initialContainerNumber = containerNumber,
                        onFileImported = { name, data, verifications ->
                            importedFileName.value = name
                            containerList.value = data
                            verificationList.clear()
                            verificationList.addAll(verifications)
                            fileNameFile.writeText(name)
                            saveContainers()
                            saveVerification()
                        },
                        onDeleteData = {
                            verificationList.clear()
                            containerList.value = emptyList()
                            importedFileName.value = ""
                            if (verificationFile.exists()) verificationFile.delete()
                            if (containerFile.exists()) containerFile.delete()
                            if (fileNameFile.exists()) fileNameFile.delete()
                        },
                        onSaveVerification = {
                            saveVerification()
                        }
                    )
                }
                composable(
                    "alarms?brand={brand}&code={code}",
                    arguments = listOf(
                        navArgument("brand") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("code") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val brand = backStackEntry.arguments?.getString("brand")
                    val code = backStackEntry.arguments?.getString("code")
                    AlarmsScreen(initialBrand = brand, initialCode = code)
                }
                composable("notifications") {
                    NotificationScreen(
                        context = this@MainActivity,
                        navController = navController,
                        verificationList = verificationList,
                        onUpdate = { verification, newStatus ->
                            val index = verificationList.indexOfFirst { it.containerNumber == verification.containerNumber }
                            if (index != -1) {
                                try {
                                    val updatedItem = verificationList[index].copy(status = newStatus)
                                    verificationList[index] = updatedItem
                                    saveVerification()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
                composable("chat") {
                    ChatScreen(profile = profile)
                }
                composable("manuals") {
                    ManualsScreen(navController)
                }
                composable("profile") {
                    ProfileScreen(
                        profile = profile,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
