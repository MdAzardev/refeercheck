package com.mrrawthereltech.reefercheck

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportToExcel(context: Context, containerList: List<ContainerData>, verificationList: List<Verification>) {
    if (containerList.isEmpty()) {
        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Verification Report")

        // Date Formatter
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

        // Header Style
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
        }

        // Color Styles
        val greenStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val yellowStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val redStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.RED.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.bold = true
            font.color = IndexedColors.WHITE.index
            setFont(font)
        }
        val greyStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val specialStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val header = sheet.createRow(0)
        val headers = listOf("Pos", "Container Number", "Set Temp", "Humidity", "Vent", "POL", "POD", "OPR", "Reefer Type", "Actual Temp", "Actual Humidity", "Status", "Alarm Code", "Alarm Description", "Remark", "Date & Time")
        headers.forEachIndexed { i, title ->
            val cell = header.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        containerList.forEachIndexed { index, container ->
            val row = sheet.createRow(index + 1)
            val verification = verificationList.find { it.containerNumber == container.containerNumber }

            row.createCell(0).setCellValue(container.position)
            row.createCell(1).setCellValue(container.containerNumber)
            row.createCell(2).setCellValue(container.setTemp)
            row.createCell(3).setCellValue(container.humidity)
            row.createCell(4).setCellValue(container.vent)
            row.createCell(5).setCellValue(container.pol)
            row.createCell(6).setCellValue(container.pod)
            row.createCell(7).setCellValue(container.opr)
            row.createCell(8).setCellValue(container.reeferType)

            // Special highlighting for CA/CT reefers in Excel
            val isSpecial = container.reeferType.isNotEmpty()

            if (verification != null) {
                row.createCell(9).setCellValue(verification.actualTemp)
                row.createCell(10).setCellValue(verification.actualHumidity)
                row.createCell(11).setCellValue(verification.status)
                row.createCell(12).setCellValue(verification.alarmCode)
                row.createCell(13).setCellValue(verification.alarmDescription)
                row.createCell(14).setCellValue(verification.remark)
                
                // Format Timestamp to Readable Date/Time
                val formattedDate = dateFormat.format(Date(verification.timestamp))
                row.createCell(15).setCellValue(formattedDate)

                val style = when {
                    verification.status == "Alarm" -> redStyle
                    verification.status == "Not in Range" -> yellowStyle
                    verification.status == "In Range" -> greenStyle
                    else -> if (isSpecial) specialStyle else null
                }
                
                if (style != null) {
                    for (i in 0..15) {
                        val cell = row.getCell(i) ?: row.createCell(i)
                        cell.cellStyle = style
                    }
                }
            } else {
                row.createCell(9).setCellValue("PENDING")
                val style = if (isSpecial) specialStyle else greyStyle
                for (i in 0..15) {
                    val cell = row.getCell(i) ?: row.createCell(i)
                    cell.cellStyle = style
                }
            }
        }

        for (i in 0 until headers.size) {
            sheet.setColumnWidth(i, 15 * 256)
        }
        sheet.setColumnWidth(13, 20 * 256) // Alarm Description
        sheet.setColumnWidth(15, 25 * 256) // Wider for date

        val fileName = "verification_full_report_${System.currentTimeMillis()}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileOutputStream(file).use { output -> 
            workbook.write(output) 
        }
        workbook.close()

        Toast.makeText(context, "Report exported: ${verificationList.size}/${containerList.size} verified", Toast.LENGTH_LONG).show()
        shareFile(context, file)

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun generateSampleExcel(context: Context) {
    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sample Sheet")

        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
        }

        val header = sheet.createRow(0)
        val headers = listOf("Pos", "Container Number", "Set Temp", "Humidity", "Vent", "POL", "POD", "OPR", "Reefer Type", "Actual Temp", "Actual Humidity", "Status", "Alarm Code", "Alarm Description", "Remark", "Date & Time")
        headers.forEachIndexed { i, title ->
            val cell = header.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Add standard sample row
        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue("A101")
        row1.createCell(1).setCellValue("TCLU1234567")
        row1.createCell(2).setCellValue("-18.0")
        row1.createCell(3).setCellValue("Off")
        row1.createCell(4).setCellValue("15")
        row1.createCell(5).setCellValue("SHANGHAI")
        row1.createCell(6).setCellValue("HAMBURG")
        row1.createCell(7).setCellValue("CMA")
        row1.createCell(8).setCellValue("") // Normal Reefer
        row1.createCell(9).setCellValue("PENDING")

        // Add Special Reefer sample row (Highlighted)
        val row2 = sheet.createRow(2)
        row2.createCell(0).setCellValue("B205")
        row2.createCell(1).setCellValue("MSCU9876543")
        row2.createCell(2).setCellValue("2.0")
        row2.createCell(3).setCellValue("85%")
        row2.createCell(4).setCellValue("25")
        row2.createCell(5).setCellValue("MUMBAI")
        row2.createCell(6).setCellValue("DUBAI")
        row2.createCell(7).setCellValue("MSC")
        row2.createCell(8).setCellValue("CA Reefer") // Special Reefer
        row2.createCell(9).setCellValue("PENDING")

        for (i in 0 until headers.size) {
            sheet.setColumnWidth(i, 15 * 256)
        }

        val fileName = "sample_reefer_list.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileOutputStream(file).use { output -> 
            workbook.write(output) 
        }
        workbook.close()

        Toast.makeText(context, "Sample excel generated", Toast.LENGTH_SHORT).show()
        shareFile(context, file)

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate sample: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    context.startActivity(Intent.createChooser(intent, "Share File"))
}
