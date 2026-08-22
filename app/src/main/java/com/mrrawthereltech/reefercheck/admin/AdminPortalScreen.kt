package com.mrrawthereltech.reefercheck.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrrawthereltech.reefercheck.AdminStats
import com.mrrawthereltech.reefercheck.AdminUserItem
import com.mrrawthereltech.reefercheck.network.ApiClient
import kotlinx.coroutines.launch

private val AdminBg     = Color(0xFF0D1117)
private val AdminCard   = Color(0xFF161B22)
private val AdminAccent = Color(0xFF00BFFF)
private val AdminText   = Color(0xFFE0E1DD)
private val AdminSub    = Color(0xFF778DA9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(onBack: () -> Unit) {
    val scope  = rememberCoroutineScope()
    var users  by remember { mutableStateOf<List<AdminUserItem>>(emptyList()) }
    var stats  by remember { mutableStateOf<AdminStats?>(null) }
    var search by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    var snackMsg  by remember { mutableStateOf<String?>(null) }

    fun loadData(q: String? = null) {
        isLoading = true; errorMsg = null
        scope.launch {
            try {
                val usersResp = ApiClient.api.getAdminUsers(search = if (q.isNullOrBlank()) null else q)
                val statsResp = ApiClient.api.getAdminStats()
                if (usersResp.isSuccessful) users = usersResp.body()?.data?.users ?: emptyList()
                if (statsResp.isSuccessful) stats = statsResp.body()?.data
            } catch (e: Exception) { errorMsg = e.message }
            finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text("Admin Portal", color = AdminText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AdminAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData(search) }) {
                        Icon(Icons.Default.Refresh, null, tint = AdminAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        snackbarHost = {
            snackMsg?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = AdminCard,
                    contentColor = AdminText
                ) { Text(it) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Stats row
            stats?.let { s ->
                item {
                    Text("Overview", fontSize = 13.sp, color = AdminSub, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip("Total", s.totalUsers.toString(), Color(0xFF1565C0), Modifier.weight(1f))
                        StatChip("Active", s.activeSubscriptions.toString(), Color(0xFF2E7D32), Modifier.weight(1f))
                        StatChip("Trial", s.trialUsers.toString(), Color(0xFFF57F17), Modifier.weight(1f))
                        StatChip("Expired", s.expiredUsers.toString(), Color(0xFFC62828), Modifier.weight(1f))
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search by email or phone", color = AdminSub) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = AdminAccent) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = ""; loadData(null) }) {
                                Icon(Icons.Default.Clear, null, tint = AdminSub)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AdminText, unfocusedTextColor = AdminText,
                        focusedBorderColor = AdminAccent, unfocusedBorderColor = AdminSub.copy(alpha = 0.4f),
                        cursorColor = AdminAccent
                    )
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { loadData(search) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminAccent)
                ) {
                    Text("Search", color = AdminBg, fontWeight = FontWeight.Bold)
                }
            }

            // Error
            errorMsg?.let { msg ->
                item {
                    Surface(color = Color(0xFFD32F2F).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = Color(0xFFEF5350), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminAccent)
                    }
                }
            }

            // Users header
            item {
                Text(
                    "Users (${users.size})",
                    fontSize = 13.sp, color = AdminSub,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }

            // User cards
            items(users) { user ->
                AdminUserCard(
                    user = user,
                    onUpdateSubscription = { status ->
                        scope.launch {
                            try {
                                val resp = ApiClient.api.updateAdminSubscription(user.id, mapOf("status" to status))
                                snackMsg = if (resp.isSuccessful) "Updated: ${user.email} → $status"
                                else resp.body()?.message ?: "Update failed"
                                loadData(search)
                            } catch (e: Exception) { snackMsg = e.message }
                        }
                    },
                    onDeleteUser = {
                        scope.launch {
                            try {
                                val resp = ApiClient.api.deleteUser(user.id)
                                snackMsg = if (resp.isSuccessful) "Deleted: ${user.email}"
                                else "Delete failed"
                                loadData(search)
                            } catch (e: Exception) { snackMsg = e.message }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 10.sp, color = AdminSub)
        }
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUserItem,
    onUpdateSubscription: (String) -> Unit,
    onDeleteUser: () -> Unit
) {
    var showMenu   by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val subStatus  = user.subscription?.status ?: "none"
    val statusColor = when (subStatus) {
        "active"  -> Color(0xFF4CAF50)
        "trial"   -> Color(0xFFFF9800)
        "expired" -> Color(0xFFF44336)
        else      -> AdminSub
    }

    Surface(
        color = AdminCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.email, fontWeight = FontWeight.Bold, color = AdminText, fontSize = 14.sp)
                    if (!user.phone.isNullOrEmpty()) {
                        Text(user.phone, color = AdminSub, fontSize = 12.sp)
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        subStatus.uppercase(),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user.isAdmin) {
                    Surface(color = Color(0xFF7B1FA2).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("ADMIN", color = Color(0xFFCE93D8), fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                if (!user.isVerified) {
                    Surface(color = Color(0xFFD32F2F).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("UNVERIFIED", color = Color(0xFFEF9A9A), fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                user.subscription?.daysRemaining?.let { days ->
                    if (days > 0) {
                        Surface(color = AdminAccent.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                            Text("${days}d left", color = AdminAccent, fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = AdminSub.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Subscription dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminAccent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Status", fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = AdminCard
                    ) {
                        listOf("active", "trial", "expired").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.capitalize(), color = AdminText) },
                                onClick = { showMenu = false; onUpdateSubscription(s) }
                            )
                        }
                    }
                }

                // Delete button
                OutlinedButton(
                    onClick = { showDelete = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // Confirm delete dialog
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = AdminCard,
            title = { Text("Delete User", color = AdminText) },
            text = { Text("Delete ${user.email} and all their data? This cannot be undone.", color = AdminSub) },
            confirmButton = {
                Button(
                    onClick = { showDelete = false; onDeleteUser() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = AdminSub)
                }
            }
        )
    }
}
