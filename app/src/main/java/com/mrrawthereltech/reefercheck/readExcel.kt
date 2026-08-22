package com.mrrawthereltech.reefercheck

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Locale

data class ExcelImportResult(
    val fileName: String,
    val containerData: List<ContainerData>,
    val verificationData: List<Verification>,
)

fun readExcel(context: Context, uri: Uri): ExcelImportResult? {
    val containers = mutableListOf<ContainerData>()
    val verifications = mutableListOf<Verification>()
    var fileName = "Unknown File"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            fileName = cursor.getString(nameIndex)
        }
    }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet.drop(1)) { // skip header
                val containerNum = getCellValueAsString(row.getCell(1))
                if (containerNum.isNotEmpty()) {
                    containers.add(
                        ContainerData(
                            position = getCellValueAsString(row.getCell(0)),
                            containerNumber = containerNum,
                            setTemp = getCellValueAsString(row.getCell(2)),
                            humidity = getCellValueAsString(row.getCell(3)),
                            vent = getCellValueAsString(row.getCell(4)),
                            pol = getCellValueAsString(row.getCell(5)),
                            pod = getCellValueAsString(row.getCell(6)),
                            opr = getCellValueAsString(row.getCell(7)),
                            reeferType = getCellValueAsString(row.getCell(8))
                        )
                    )

                    val actualTemp = getCellValueAsString(row.getCell(9))
                    val status = getCellValueAsString(row.getCell(11))
                    
                    if ((status.isNotEmpty()) && (actualTemp != "PENDING")) {
                        val actualHum = getCellValueAsString(row.getCell(10))
                        val remark = getCellValueAsString(row.getCell(12))
                        val dateStr = getCellValueAsString(row.getCell(13))
                        val timestamp = try {
                            if (dateStr.isNotEmpty()) dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                            else System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }

                        verifications.add(
                            Verification(
                                containerNumber = containerNum,
                                actualTemp = actualTemp,
                                actualHumidity = actualHum,
                                remark = remark,
                                status = status,
                                timestamp = timestamp
                            )
                        )
                    }
                }
            }
            workbook.close()
        }
        return ExcelImportResult(fileName, containers, verifications)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * Imports detailed alarm codes from an Excel file.
 * Groups components by Brand + Code and merges with existing data.
 */
fun importAlarmsFromExcel(context: Context, uri: Uri): Int {
    var importedCount = 0
    try {
        // Load existing data to merge
        AlarmCodeProvider.loadData(context)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val formatter = DataFormatter()
            val incomingAlarmsMap = mutableMapOf<String, AlarmCode>()

            for (s in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(s)
                for (row in sheet.drop(1)) {
                    var brand = formatter.formatCellValue(row.getCell(0))
                    val code = formatter.formatCellValue(row.getCell(1))
                    
                    if (code.isNotEmpty()) {
                        if (brand.isEmpty()) brand = "Carrier" // Default to Carrier if blank
                        
                        val description = formatter.formatCellValue(row.getCell(2))
                        val cause = formatter.formatCellValue(row.getCell(3))
                        val componentName = formatter.formatCellValue(row.getCell(4))
                        val troubleshooting = formatter.formatCellValue(row.getCell(5))
                        val correctiveAction = formatter.formatCellValue(row.getCell(6))
                        
                        val component = AlarmComponent(
                            component = componentName,
                            troubleshooting = troubleshooting,
                            correctiveAction = correctiveAction
                        )

                        val key = "$brand|$code".uppercase()
                        val existingInMap = incomingAlarmsMap[key]
                        
                        if (existingInMap != null) {
                            incomingAlarmsMap[key] = existingInMap.copy(
                                components = existingInMap.components + component
                            )
                        } else {
                            incomingAlarmsMap[key] = AlarmCode(
                                brand = brand,
                                code = code,
                                description = description,
                                cause = cause,
                                components = listOf(component)
                            )
                        }
                    }
                }
            }
            
            if (incomingAlarmsMap.isNotEmpty()) {
                val currentAlarms = AlarmCodeProvider.alarmCodes.toMutableList()
                
                for (newAlarm in incomingAlarmsMap.values) {
                    val key = "${newAlarm.brand}|${newAlarm.code}".uppercase()
                    val index = currentAlarms.indexOfFirst { 
                        "${it.brand}|${it.code}".uppercase() == key 
                    }
                    
                    if (index != -1) {
                        currentAlarms[index] = newAlarm
                    } else {
                        currentAlarms.add(newAlarm)
                    }
                    importedCount++
                }
                
                AlarmCodeProvider.alarmCodes.clear()
                AlarmCodeProvider.alarmCodes.addAll(currentAlarms)
                AlarmCodeProvider.saveData(context)
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return importedCount
}

private fun getCellValueAsString(cell: Cell?): String {
    if (cell == null) return ""
    return when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue ?: ""
        CellType.NUMERIC -> {
            val value = cell.numericCellValue
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        }
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        CellType.FORMULA -> {
            try {
                cell.stringCellValue ?: ""
            } catch (e: Exception) {
                cell.numericCellValue.toString()
            }
        }
        else -> ""
    }
}
