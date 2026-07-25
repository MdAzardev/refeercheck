package com.example.myapplication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(initialBrand: String? = "All", initialCode: String? = null) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(if (initialCode == null || initialCode == "null") "" else initialCode) }
    var selectedBrand by remember { 
        mutableStateOf(if (initialBrand.isNullOrBlank() || initialBrand == "null" || initialBrand == "All") "All" else initialBrand) 
    }
    var selectedModel by remember { mutableStateOf("") }
    
    val brands = listOf("Carrier", "StarCool", "Daikin", "Thermo King")
    val brandModels = mapOf(
        "Daikin" to listOf("LX10E", "LX10F"),
        "StarCool" to listOf("CIM 5", "CIM 6")
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val count = importAlarmsFromExcel(context, uri)
            if (count > 0) {
                AlarmCodeProvider.saveData(context)
                Toast.makeText(context, "Successfully imported $count alarm codes!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Import failed. Check Excel format.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedBrand != "All") {
                    IconButton(onClick = { 
                        if (selectedModel.isNotEmpty() && brandModels.containsKey(selectedBrand)) {
                            selectedModel = ""
                        } else {
                            selectedBrand = "All"
                            selectedModel = ""
                            searchQuery = "" 
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            selectedBrand == "All" -> "Select Reefer Brand"
                            selectedModel.isNotEmpty() -> "$selectedBrand $selectedModel Alarms"
                            else -> "$selectedBrand Alarms"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import Database", tint = Color.White)
                }
            }
        }

        if (selectedBrand == "All") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(brands) { brand ->
                    BrandCard(brand) { 
                        selectedBrand = brand 
                        if (!brandModels.containsKey(brand)) {
                            selectedModel = "" // No models for this brand
                        }
                    }
                }
            }
        } else if (brandModels.containsKey(selectedBrand) && selectedModel.isEmpty()) {
            // Model selection
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select $selectedBrand Model",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(brandModels[selectedBrand]!!) { model ->
                        ModelCard(model, selectedBrand) { selectedModel = model }
                    }
                }
            }
        } else {
            // Search and results
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alarm Code (ex: AL003/dAL89)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (searchQuery.isNotBlank()) {
                    val filteredAlarms = AlarmCodeProvider.alarmCodes.filter { alarm ->
                        alarm.brand.equals(selectedBrand, ignoreCase = true) &&
                        (selectedModel.isEmpty() || alarm.model.equals(selectedModel, ignoreCase = true)) &&
                        (alarm.code.contains(searchQuery, ignoreCase = true) ||
                         alarm.description.contains(searchQuery, ignoreCase = true))
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredAlarms) { alarm ->
                            DetailedAlarmResult(alarm)
                        }
                        
                        if (filteredAlarms.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No information found for \"$searchQuery\"", color = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
                        Text(
                            "Enter an alarm code or keyword to see details",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrandCard(brand: String, onClick: () -> Unit) {
    val color = when(brand) {
        "Carrier" -> Color(0xFF1976D2)
        "Thermo King" -> Color(0xFFD32F2F)
        "Daikin" -> Color(0xFF388E3C)
        "StarCool" -> Color(0xFFE65100)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = brand, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
        }
    }
}

@Composable
fun ModelCard(model: String, brand: String, onClick: () -> Unit) {
    val color = when(brand) {
        "Daikin" -> Color(0xFF388E3C)
        "StarCool" -> Color(0xFFE65100)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = model, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        }
    }
}

@Composable
fun DetailedAlarmResult(alarm: AlarmCode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CODE: ${alarm.code}", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 22.sp, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (alarm.model.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = alarm.model,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            AlarmInfoSection("Alarm Description", alarm.description, Color.Black)
            
            if (alarm.cause.isNotEmpty()) {
                AlarmInfoSection("Possible Cause", alarm.cause, Color(0xFFC62828))
            }

            if (alarm.correctiveAction.isNotEmpty()) {
                AlarmInfoSection("Corrective Action", alarm.correctiveAction, Color(0xFF2E7D32))
            }

            if (alarm.controllerAction.isNotEmpty()) {
                AlarmInfoSection("Controller Action", alarm.controllerAction, Color(0xFF1565C0))
            }

            if (alarm.components.isNotEmpty()) {
                alarm.components.forEach { comp ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.LightGray)
                    if (comp.component.isNotEmpty()) {
                        Text(text = "Component: ${comp.component}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    }
                    if (comp.troubleshooting.isNotEmpty()) {
                        AlarmInfoSection("Troubleshooting", comp.troubleshooting, Color(0xFF2E7D32))
                    }
                    if (comp.correctiveAction.isNotEmpty()) {
                        AlarmInfoSection("Corrective Action", comp.correctiveAction, Color(0xFFEF6C00))
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmInfoSection(label: String, content: String, labelColor: Color) {
    Column {
        Text(text = label, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = labelColor.copy(alpha = 0.8f))
        Spacer(Modifier.height(2.dp))
        Text(text = content, style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
    }
}
