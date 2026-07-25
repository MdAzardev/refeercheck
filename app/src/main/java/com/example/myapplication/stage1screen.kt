package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun Stage1Screen(
    context: Context,
    verificationList: MutableList<Verification>,
    containerList: List<ContainerData>,
    navController: NavController,
    importedFileName: String,
    initialContainerNumber: String? = null,
    onFileImported: (String, List<ContainerData>, List<Verification>) -> Unit,
    onDeleteData: () -> Unit,
    onSaveVerification: () -> Unit
) {
    var searchText by remember { mutableStateOf(initialContainerNumber ?: "") }
    var result by remember { mutableStateOf<ContainerData?>(null) }
    var actualTemp by remember { mutableStateOf("") }
    var actualHumidity by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var alarmCodeState by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val total = containerList.size
    val checked = verificationList.size
    val alarmsCount = verificationList.count { it.status == "Alarm" }
    val notInRange = verificationList.count { it.status == "Not in Range" }

    LaunchedEffect(Unit) {
        AlarmCodeProvider.loadData(context)
    }

    val closeScanner = { showScanner = false }
    val openScanner = { showScanner = true }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openScanner()
        else Toast.makeText(context, "Camera permission is required to scan.", Toast.LENGTH_LONG).show()
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) photoUri = null
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val spokenText = activityResult.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                remark = if (remark.isEmpty()) spokenText else "$remark $spokenText"
            }
        }
    }

    LaunchedEffect(initialContainerNumber, containerList) {
        if (!initialContainerNumber.isNullOrEmpty() && containerList.isNotEmpty()) {
            val found = containerList.find { it.containerNumber.equals(initialContainerNumber, ignoreCase = true) }
            if (found != null) {
                searchText = found.containerNumber
                result = found
                val existing = verificationList.find { it.containerNumber.equals(found.containerNumber, ignoreCase = true) }
                actualTemp = existing?.actualTemp ?: ""
                actualHumidity = existing?.actualHumidity ?: ""
                remark = existing?.remark ?: ""
                alarmCodeState = existing?.alarmCode ?: ""
                status = existing?.status ?: ""
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val excelResult = readExcel(context, uri)
            if (excelResult != null) {
                onFileImported(excelResult.fileName, excelResult.containerData, excelResult.verificationData)
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onDismiss = closeScanner,
            onBarcodeScanned = { detected ->
                searchText = detected
                closeScanner()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrandingHeader()

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (importedFileName.isEmpty()) "No File Imported" else "File: $importedFileName",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                if (importedFileName.isNotEmpty() || verificationList.isNotEmpty()) {
                    IconButton(
                        onClick = { onDeleteData(); result = null; searchText = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete All Data", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (total > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Total", total.toString(), Color.Black)
                        StatItem("Done", checked.toString(), Color(0xFF4CAF50))
                        StatItem("Left", (total - checked).toString(), Color.Gray)
                        StatItem("Alarm", alarmsCount.toString(), Color.Red)
                        StatItem("Not Range", notInRange.toString(), Color(0xFFFBC02D))
                    }
                }
            }

            if (importedFileName.isEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import Excel") }
                    
                    OutlinedButton(
                        onClick = { generateSampleExcel(context) },
                        modifier = Modifier.weight(1f)
                    ) { 
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sample") 
                    }
                }
            }

            OutlinedTextField(
                value = searchText,
                onValueChange = { newValue -> 
                    searchText = newValue 
                    if (newValue.isEmpty()) result = null
                },
                label = { Text("Search Container Number") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    IconButton(onClick = { 
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openScanner()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) { Icon(Icons.Default.PhotoCamera, contentDescription = "Scan Container") }
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = ""; result = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                enabled = containerList.isNotEmpty()
            )

            if (searchText.isNotEmpty() && result == null) {
                val filteredList = containerList.filter {
                    it.containerNumber.contains(searchText.trim(), ignoreCase = true)
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        filteredList.take(5).forEach { item ->
                            val isVerified = verificationList.any { it.containerNumber == item.containerNumber }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchText = item.containerNumber
                                        result = item
                                        val existing = verificationList.find { it.containerNumber == item.containerNumber }
                                        actualTemp = existing?.actualTemp ?: ""
                                        actualHumidity = existing?.actualHumidity ?: ""
                                        remark = existing?.remark ?: ""
                                        alarmCodeState = existing?.alarmCode ?: ""
                                        status = existing?.status ?: ""
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item.containerNumber, color = if (isVerified) Color(0xFF4CAF50) else Color.Unspecified)
                                if (item.reeferType.isNotEmpty()) {
                                    Surface(color = Color(0xFF1976D2), shape = MaterialTheme.shapes.extraSmall) {
                                        Text(text = item.reeferType, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            result?.let { currentContainer ->
                val existingVerification = verificationList.find { v -> v.containerNumber == currentContainer.containerNumber }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (existingVerification != null) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pos: ${currentContainer.position}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Container: ${currentContainer.containerNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(text = "TYPE: ${currentContainer.reeferType}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2), fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Set Temp: ${currentContainer.setTemp}", fontSize = 13.sp)
                            Text("Humidity: ${currentContainer.humidity}", fontSize = 13.sp)
                        }
                        if (existingVerification != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            Text("Current: Temp: ${existingVerification.actualTemp} | Status: ${existingVerification.status}", fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = actualTemp, onValueChange = { actualTemp = it }, label = { Text("Act. Temp") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = actualHumidity, onValueChange = { actualHumidity = it }, label = { Text("Act. Humid") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Remark") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechLauncher.launch(intent)
                        }) { Icon(Icons.Default.Mic, contentDescription = "Voice Record") }
                    }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Status (Required)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (status == "Recheck") {
                                Surface(color = Color(0xFF0288D1), shape = RoundedCornerShape(4.dp)) {
                                    Text("RE-VERIFICATION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatusRadio("In Range", status) { status = it }
                            StatusRadio("Not in Range", status) { status = it }
                            StatusRadio("Alarm", status) { status = it }
                        }
                    }
                }

                if (status == "Alarm" || status == "Recheck") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = alarmCodeState,
                            onValueChange = { alarmCodeState = it },
                            label = { Text("Alarm Code") },
                            placeholder = { Text("e.g. 06, AL06 or 517") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (alarmCodeState.isNotEmpty()) {
                                    IconButton(onClick = { alarmCodeState = "" }) { Icon(Icons.Default.Clear, null) }
                                }
                            }
                        )

                        // EXPANDED Brand-Aware Direct Reflection
                        val detectedAlarm = remember(alarmCodeState, currentContainer.reeferType) {
                            val input = alarmCodeState.trim().uppercase()
                            if (input.isNotBlank()) {
                                val brandFilter = when {
                                    currentContainer.reeferType.contains("Carrier", ignoreCase = true) -> "Carrier"
                                    currentContainer.reeferType.contains("TK", ignoreCase = true) || 
                                    currentContainer.reeferType.contains("Thermo", ignoreCase = true) -> "Thermo King"
                                    currentContainer.reeferType.contains("Star", ignoreCase = true) -> "StarCool"
                                    currentContainer.reeferType.contains("Daikin", ignoreCase = true) -> "Daikin"
                                    else -> null
                                }
                                AlarmCodeProvider.alarmCodes.find { alarm ->
                                    val dbCode = alarm.code.uppercase()
                                    val ni = input.replace("AL", "").replace(" ", "").trimStart('0')
                                    val nd = dbCode.replace("AL", "").replace(" ", "").trimStart('0')
                                    
                                    val codeMatches = dbCode == input || (ni.isNotEmpty() && nd == ni)
                                    val brandMatches = brandFilter == null || alarm.brand.equals(brandFilter, ignoreCase = true)
                                    
                                    codeMatches && brandMatches
                                }
                            } else null
                        }

                        if (detectedAlarm != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "${detectedAlarm.brand} ALARM: ${detectedAlarm.code}", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 16.sp)
                                    
                                    ReflectItem("Description", detectedAlarm.description)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val photoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Alarm_${currentContainer.containerNumber}_${System.currentTimeMillis()}.jpg")
                                photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                takePictureLauncher.launch(photoUri!!)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (photoUri != null) "Photo Captured" else "Capture Alarm Evidence")
                        }
                    }
                }

                Button(
                    onClick = {
                        val detectedDesc = if (status == "Alarm") {
                            val input = alarmCodeState.trim().uppercase()
                            val brandFilter = when {
                                currentContainer.reeferType.contains("Carrier", ignoreCase = true) -> "Carrier"
                                currentContainer.reeferType.contains("TK", ignoreCase = true) || 
                                currentContainer.reeferType.contains("Thermo", ignoreCase = true) -> "Thermo King"
                                currentContainer.reeferType.contains("Star", ignoreCase = true) -> "StarCool"
                                currentContainer.reeferType.contains("Daikin", ignoreCase = true) -> "Daikin"
                                else -> null
                            }
                            AlarmCodeProvider.alarmCodes.find { alarm ->
                                val dbCode = alarm.code.uppercase()
                                val ni = input.replace("AL", "").replace(" ", "").trimStart('0')
                                val nd = dbCode.replace("AL", "").replace(" ", "").trimStart('0')
                                val codeMatches = dbCode == input || (ni.isNotEmpty() && nd == ni)
                                val brandMatches = brandFilter == null || alarm.brand.equals(brandFilter, ignoreCase = true)
                                codeMatches && brandMatches
                            }?.description ?: ""
                        } else ""

                        val locAction = { lat: Double?, lon: Double? ->
                            val newVerification = Verification(
                                currentContainer.containerNumber, actualTemp, actualHumidity, remark, status,
                                alarmCode = alarmCodeState,
                                alarmDescription = detectedDesc,
                                latitude = lat, longitude = lon,
                                photoPath = photoUri?.toString()
                            )
                            saveAndClear(newVerification, verificationList, onSaveVerification, navController)
                            searchText = ""; result = null; actualTemp = ""; actualHumidity = ""; remark = ""; alarmCodeState = ""; status = ""; photoUri = null
                        }

                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc -> locAction(loc?.latitude, loc?.longitude) }
                                .addOnFailureListener { locAction(null, null) }
                        } else { locAction(null, null) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = status.isNotEmpty()
                ) { Text(if (existingVerification != null) "Update Round" else "Save Round Data") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { exportToExcel(context, containerList, verificationList) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), enabled = containerList.isNotEmpty()) { Text("Export & Share Report") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReflectItem(label: String, content: String) {
    Column {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
        Text(text = content, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerDialog(onDismiss: () -> Unit, onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var isDetected by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f), shape = MaterialTheme.shapes.large) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                            val barcodeScanner = BarcodeScanning.getClient()
                            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()
                            val imageAnalysis = ImageAnalysis.Builder().setResolutionSelector(resolutionSelector).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isDetected) { imageProxy.close(); return@setAnalyzer }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    barcodeScanner.process(image).addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !isDetected) { barcodes.firstOrNull()?.displayValue?.let { isDetected = true; onBarcodeScanned(it) } }
                                    }.addOnCompleteListener {
                                        if (!isDetected) {
                                            textRecognizer.process(image).addOnSuccessListener { visionText ->
                                                if (!isDetected && visionText.text.isNotEmpty()) {
                                                    val cleanText = visionText.text.uppercase().replace(Regex("[^A-Z0-9]"), "")
                                                    val pattern = Regex("[A-Z]{4}[0-9]{7}")
                                                    val match = pattern.find(cleanText)
                                                    if (match != null) { isDetected = true; onBarcodeScanned(match.value) }
                                                }
                                            }.addOnCompleteListener { imageProxy.close() }
                                        } else { imageProxy.close() }
                                    }
                                } else { imageProxy.close() }
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                            } catch (e: Exception) { Log.e("Scanner", "Camera binding failed", e) }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
                
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onDismiss, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Box(modifier = Modifier.size(width = 300.dp, height = 150.dp).background(Color.White.copy(alpha = 0.05f)).padding(2.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 3.dp.toPx()
                            val cornerSize = 30.dp.toPx()
                            val color = Color.Green
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(cornerSize, 0f), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, cornerSize), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - cornerSize, 0f), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, cornerSize), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(cornerSize, size.height), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - cornerSize), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - cornerSize, size.height), strokeWidth)
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - cornerSize), strokeWidth)
                        }
                    }
                    Text("Align container number within green box.", modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small).padding(12.dp), color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun saveAndClear(verification: Verification, list: MutableList<Verification>, onSave: () -> Unit, nav: NavController) {
    val index = list.indexOfFirst { it.containerNumber == verification.containerNumber }
    if (index != -1) list[index] = verification else list.add(verification)
    onSave()
    nav.navigate("stage1") { popUpTo("stage1") { inclusive = true } }
}

@Composable
fun BrandingHeader() {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sideBodyPath = Path().apply {
                        moveTo(size.width * 0.35f, size.height * 0.4f)
                        lineTo(size.width * 0.85f, size.height * 0.45f)
                        lineTo(size.width * 0.85f, size.height * 0.75f)
                        lineTo(size.width * 0.35f, size.height * 0.78f)
                        close()
                    }
                    drawPath(sideBodyPath, color = Color(0xFFF0F0F0), style = Fill)
                    drawPath(sideBodyPath, color = Color.LightGray, style = Stroke(width = 0.5.dp.toPx()))
                    val frontUnitPath = Path().apply {
                        moveTo(size.width *(0.15f), size.height * 0.45f)
                        lineTo(size.width * 0.35f, size.height * 0.4f)
                        lineTo(size.width * 0.35f, size.height * 0.78f)
                        lineTo(size.width * 0.15f, size.height * 0.7f)
                        close()
                    }
                    drawPath(frontUnitPath, color = Color(0xFF003399), style = Fill)
                    val checkmarkPath = Path().apply {
                        moveTo(size.width * 0.3f, size.height * 0.6f)
                        lineTo(size.width * 0.45f, size.height * 0.75f)
                        lineTo(size.width * 0.85f, size.height * 0.42f)
                    }
                    drawPath(checkmarkPath, color = Color.Black.copy(alpha = 0.3f), style = Stroke(width = 4.dp.toPx()))
                    drawPath(checkmarkPath, color = Color(0xFF4CAF50), style = Stroke(width = 2.5.dp.toPx()))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Reefer Assistance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(text = "Powered by M.R.Rawther", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun StatusRadio(label: String, current: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = current == label, onClick = { onSelect(label) })
        Text(label, fontSize = 11.sp)
    }
}
