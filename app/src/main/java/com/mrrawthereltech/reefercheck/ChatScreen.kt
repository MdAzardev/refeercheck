package com.mrrawthereltech.reefercheck

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean,
    val fileUri: String? = null,
    val fileType: String? = null // "image", "pdf", etc.
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(profile: UserProfile) {
    val context = LocalContext.current
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    
    // In a real app, these would come from a database/backend
    val allConversations = remember { mutableStateMapOf<String, MutableList<ChatMessage>>() }
    val friendNames = remember { mutableStateMapOf<String, String>() }

    // Initialize with a demo friend
    LaunchedEffect(Unit) {
        if (friendNames.isEmpty()) {
            val demoId = "RA-888888"
            friendNames[demoId] = "Technical Support"
            allConversations[demoId] = mutableStateListOf(
                ChatMessage(senderId = demoId, senderName = "Support", message = "Hello ${profile.name}! How can we help you today?", isMe = false)
            )
        }
    }

    if (selectedFriendId == null) {
        // Main Conversations List
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Messages", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                    actions = {
                        IconButton(onClick = { showAddFriendDialog = true }) {
                            Icon(Icons.Default.PersonAdd, "Add Friend", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
                if (friendNames.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No conversations yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(friendNames.keys.toList()) { friendId ->
                            val lastMsg = allConversations[friendId]?.lastOrNull()?.message ?: "No messages"
                            ConversationItem(
                                name = friendNames[friendId] ?: friendId,
                                lastMessage = lastMsg,
                                onClick = { selectedFriendId = friendId }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Private Chat View
        val friendId = selectedFriendId!!
        val messages = allConversations[friendId] ?: remember { mutableStateListOf<ChatMessage>() }
        
        PrivateChatView(
            profile = profile,
            friendId = friendId,
            friendName = friendNames[friendId] ?: friendId,
            messages = messages,
            onBack = { selectedFriendId = null },
            onSendMessage = { text, uri, type ->
                messages.add(ChatMessage(
                    senderId = profile.userId,
                    senderName = profile.name,
                    message = text,
                    isMe = true,
                    fileUri = uri,
                    fileType = type
                ))
            }
        )
    }

    if (showAddFriendDialog) {
        var friendIdInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Friend by ID") },
            text = {
                OutlinedTextField(
                    value = friendIdInput,
                    onValueChange = { friendIdInput = it },
                    label = { Text("User ID (e.g. RA-123456)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (friendIdInput.isNotBlank()) {
                        friendNames[friendIdInput] = "User $friendIdInput"
                        allConversations[friendIdInput] = mutableStateListOf()
                        showAddFriendDialog = false
                        Toast.makeText(context, "Friend added!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ConversationItem(name: String, lastMessage: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = lastMessage, fontSize = 14.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatView(
    profile: UserProfile,
    friendId: String,
    friendName: String,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onSendMessage("Shared a file", uri.toString(), "file")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(friendName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("ID: $friendId", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Calling $friendName...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Call, null, tint = Color.White)
                    }
                    IconButton(onClick = { Toast.makeText(context, "Starting video call with $friendName...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.VideoCall, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { fileLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText, null, null)
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF2F2F2)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val isMe = message.isMe
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.fileUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(
                            imageVector = if (message.fileType == "image") Icons.Default.Image else Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = if (isMe) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Attachment",
                            color = if (isMe) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Text(
                    text = message.message,
                    color = if (isMe) Color.White else Color.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
