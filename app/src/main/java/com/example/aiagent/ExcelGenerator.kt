package com.example.aiagent

import android.content.Context
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class ExcelGenerator(context: Context) {
    private val appContext = context.applicationContext

    fun createExcelFile(data: List<List<Any>>, fileName: String): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Data")

        // Create header row
        val headerRow = sheet.createRow(0)
        data.firstOrNull()?.forEachIndexed { index, value ->
            headerRow.createCell(index).setCellValue(value.toString())
        }

        // Create data rows
        data.drop(1).forEachIndexed { rowIndex, rowData ->
            val row = sheet.createRow(rowIndex + 1)
            rowData.forEachIndexed { cellIndex, cellData ->
                when (cellData) {
                    is Number -> row.createCell(cellIndex).setCellValue(cellData.toDouble())
                    else -> row.createCell(cellIndex).setCellValue(cellData.toString())
                }
            }
        }

        // Auto-size columns
        data.firstOrNull()?.let { firstRow ->
            for (i in 0 until firstRow.size) {
                sheet.autoSizeColumn(i)
            }
        }

        // Save file
        val file = File(appContext.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { output ->
            workbook.write(output)
        }
        workbook.close()

        return file
    }

    fun readExcelFile(file: File): List<List<String>> {
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)
        val data = mutableListOf<List<String>>()

        for (row in sheet) {
            val rowData = mutableListOf<String>()
            for (cell in row) {
                rowData.add(
                    when (cell.cellType) {
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> cell.stringCellValue
                    }
                )
            }
            data.add(rowData)
        }

        workbook.close()
        return data
    }

    fun editExcelCell(file: File, rowIndex: Int, colIndex: Int, newValue: String) {
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)
        
        val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        val cell = row.getCell(colIndex) ?: row.createCell(colIndex)
        
        cell.setCellValue(newValue)
        
        FileOutputStream(file).use { output ->
            workbook.write(output)
        }
        workbook.close()
    }
}
