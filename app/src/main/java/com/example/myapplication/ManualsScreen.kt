package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualsScreen(navController: NavController) {
    val context = LocalContext.current
    val manualsDir = remember { File(context.filesDir, "manuals").apply { if (!exists()) mkdirs() } }
    
    var userManuals by remember { mutableStateOf(manualsDir.listFiles()?.toList() ?: emptyList()) }
    var bundledManualsGrouped by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            val rootFolders = context.assets.list("manuals") ?: emptyArray()
            val grouped = mutableMapOf<String, List<String>>()
            
            rootFolders.forEach { folderName ->
                val subFiles = mutableListOf<String>()
                
                fun collectManuals(path: String, prefix: String = "") {
                    context.assets.list(path)?.forEach { item ->
                        val fullPath = if (path.isEmpty()) item else "$path/$item"
                        val ext = item.lowercase()
                        if (ext.endsWith(".pdf") || ext.endsWith(".docx") || ext.endsWith(".doc") || 
                            ext.endsWith(".pptx") || ext.endsWith(".ppt")) {
                            subFiles.add(if (prefix.isEmpty()) item else "$prefix/$item")
                        } else {
                            val nested = context.assets.list(fullPath)
                            if (!nested.isNullOrEmpty()) {
                                collectManuals(fullPath, if (prefix.isEmpty()) item else "$prefix/$item")
                            }
                        }
                    }
                }

                val lowerName = folderName.lowercase()
                if (lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc") || 
                    lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                    val currentList = grouped["General Guides"] ?: emptyList()
                    grouped["General Guides"] = currentList + folderName
                } else {
                    collectManuals("manuals/$folderName")
                    if (subFiles.isNotEmpty()) {
                        grouped[folderName] = subFiles
                    }
                }
            }
            bundledManualsGrouped = grouped
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "manual_${System.currentTimeMillis()}"
            if (copyFileToInternal(context, uri, File(manualsDir, fileName))) {
                userManuals = manualsDir.listFiles()?.toList() ?: emptyList()
                Toast.makeText(context, "File saved to library!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Library", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    uploadLauncher.launch("*/*") 
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.UploadFile, "Add File", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bundledManualsGrouped.forEach { (maker, files) ->
                item {
                    Text(
                        text = maker.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(files) { filePath ->
                    val displayName = filePath.substringAfterLast("/")
                    FileItemCard(displayName, isOfficial = true) { 
                        openAssetFile(context, maker, filePath) 
                    }
                }
            }

            if (userManuals.isNotEmpty()) {
                item {
                    Text(
                        text = "MY UPLOADS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(userManuals.sortedBy { it.name }) { file ->
                    FileItemCard(
                        fileName = file.name,
                        fileSize = "${String.format(Locale.US, "%.2f", file.length() / 1024.0 / 1024.0)} MB",
                        isOfficial = false,
                        onDelete = {
                            file.delete()
                            userManuals = manualsDir.listFiles()?.toList() ?: emptyList()
                        },
                        onOpen = { openLocalFile(context, file) }
                    )
                }
            }

            if (bundledManualsGrouped.isEmpty() && userManuals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("Your library is empty", color = Color.Gray)
                            Text("Add PDF, Word or PPT files for offline access", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(
    fileName: String, 
    fileSize: String? = null,
    isOfficial: Boolean, 
    onDelete: (() -> Unit)? = null,
    onOpen: () -> Unit
) {
    val extension = fileName.substringAfterLast(".").lowercase()
    val (icon, color) = when (extension) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFD32F2F)
        "doc", "docx" -> Icons.Default.Description to Color(0xFF1976D2)
        "ppt", "pptx" -> Icons.Default.Slideshow to Color(0xFFE65100)
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (fileSize != null) {
                    Text(text = fileSize, fontSize = 11.sp, color = Color.Gray)
                }
            }
            if (isOfficial) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, "Official", tint = Color.LightGray, modifier = Modifier.size(14.dp))
            } else if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
}

private fun copyFileToInternal(context: Context, uri: Uri, destFile: File): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast(".").lowercase()
    return when (extension) {
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "*/*"
    }
}

private fun openLocalFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "No suitable app found to open this file", Toast.LENGTH_LONG).show()
    }
}

private fun openAssetFile(context: Context, maker: String, filePath: String) {
    try {
        val assetPath = if (maker == "General Guides") "manuals/$filePath" else "manuals/$maker/$filePath"
        val fileName = filePath.substringAfterLast("/")
        val inputStream = context.assets.open(assetPath)
        val tempFile = File(context.cacheDir, fileName)
        
        FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(fileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
