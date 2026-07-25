package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.admin.AdminPortalScreen
import com.example.myapplication.auth.AuthViewModel
import com.example.myapplication.auth.LoginScreen
import com.example.myapplication.auth.OtpVerificationScreen
import com.example.myapplication.auth.RegisterScreen
import com.example.myapplication.network.ApiClient
import com.example.myapplication.subscription.SubscriptionGate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val gson = Gson()
    private val verificationFile by lazy { File(filesDir, "verification_data.json") }
    private val containerFile by lazy { File(filesDir, "container_data.json") }
    private val fileNameFile by lazy { File(filesDir, "imported_file_name.txt") }
    // Legacy local profile file (kept for backward compat; new auth uses DataStore)
    private val profileFile by lazy { File(filesDir, "user_profile.json") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise ApiClient with context (needed for AuthManager DataStore)
        ApiClient.init(this)

        // Load alarm database from assets on startup
        AlarmCodeProvider.loadData(this)

        setContent {
            val authViewModel = remember { AuthViewModel(applicationContext) }

            val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle(false)

            // Show splash, then route based on login state
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen(onAnimationFinished = { showSplash = false })
            } else if (!isLoggedIn) {
                AuthNavGraph(authViewModel = authViewModel)
            } else {
                SubscriptionGate(authViewModel = authViewModel) {
                    MainContent(authViewModel = authViewModel)
                }
            }
        }
    }

    // ── Auth Navigation Graph ─────────────────────────────────────────────────

    @Composable
    fun AuthNavGraph(authViewModel: AuthViewModel) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        // Handled by isLoggedIn flow in parent
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRegistered = { email ->
                        val encoded = URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                        navController.navigate("otp/$encoded") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                "otp/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStack ->
                val encoded = backStack.arguments?.getString("email") ?: ""
                val email = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                OtpVerificationScreen(
                    authViewModel = authViewModel,
                    email = email,
                    onBack = { navController.popBackStack() },
                    onVerified = {
                        // isLoggedIn flow will trigger re-compose of root content
                    }
                )
            }
        }
    }

    // ── Main Content (authenticated) ─────────────────────────────────────────

    @Composable
    fun MainContent(authViewModel: AuthViewModel) {
        val navController = rememberNavController()

        val verificationList = remember { mutableStateListOf<Verification>() }
        val containerList = remember { mutableStateOf<List<ContainerData>>(emptyList()) }
        val importedFileName = remember { mutableStateOf("") }

        // Local profile (legacy — kept for name/company display)
        val userProfile = remember { mutableStateOf<UserProfile?>(null) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val notificationCount = verificationList.count {
            it.status == "Not in Range" || it.status == "Alarm" || it.status == "Recheck"
        }

        LaunchedEffect(Unit) {
            // Load legacy local profile if it exists (for name / company display)
            if (profileFile.exists()) {
                try { userProfile.value = gson.fromJson(profileFile.readText(), UserProfile::class.java) }
                catch (_: Exception) {}
            }
            // Load verification data
            if (verificationFile.exists()) {
                try {
                    val type = object : TypeToken<List<Verification>>() {}.type
                    val list: List<Verification> = gson.fromJson(verificationFile.readText(), type)
                    verificationList.addAll(list)
                } catch (_: Exception) {}
            }
            // Load container data
            if (containerFile.exists()) {
                try {
                    val type = object : TypeToken<List<ContainerData>>() {}.type
                    val list: List<ContainerData> = gson.fromJson(containerFile.readText(), type)
                    containerList.value = list
                } catch (_: Exception) {}
            }
            if (fileNameFile.exists()) importedFileName.value = fileNameFile.readText()

            // Sync subscription from server
            authViewModel.fetchSubscription()
        }

        val saveVerification = {
            try { verificationFile.writeText(gson.toJson(verificationList.toList())) }
            catch (_: Exception) {}
        }
        val saveContainers = {
            try { containerFile.writeText(gson.toJson(containerList.value)) }
            catch (_: Exception) {}
        }

        val profile = userProfile.value ?: UserProfile()

        Scaffold(
            bottomBar = {
                if (currentRoute != "dashboard" && currentRoute != "admin_portal") {
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
                        onSaveVerification = { saveVerification() }
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
                                    verificationList[index] = verificationList[index].copy(status = newStatus)
                                    saveVerification()
                                } catch (_: Exception) {}
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
                        authViewModel = authViewModel,
                        onLogout = {
                            authViewModel.logout()
                            // isLoggedIn flow will re-route to AuthNavGraph
                        },
                        onAdminPortal = {
                            navController.navigate("admin_portal")
                        }
                    )
                }
                composable("admin_portal") {
                    AdminPortalScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
