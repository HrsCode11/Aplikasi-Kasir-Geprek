package com.kelompok2.aplikasi_kasir_geprek.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ExcelHelper(private val context: Context) {

    fun exportToExcel(transactions: List<Transaksi>) {
        // 1. Buat Workbook (File Excel) dan Sheet (Lembar)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Laporan Penjualan")

        // --- STYLE (GAYA) ---

        // Style Header: Bold, Background Abu-abu, Border
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont()
            font.bold = true
            setFont(font)
        }

        // Style Data Biasa: Border Tipis
        val dataStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.TOP
            wrapText = true // Agar teks panjang turun ke bawah
        }

        // Style Uang (Rata Kanan): Border Tipis
        val currencyStyle = workbook.createCellStyle().apply {
            cloneStyleFrom(dataStyle)
            alignment = HorizontalAlignment.RIGHT
        }

        // 2. Buat Header Row (Baris Judul)
        val headers = listOf("No", "Tanggal", "Waktu", "Kasir", "Rincian Item", "Total (Rp)")
        val headerRow = sheet.createRow(0)

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // 3. Isi Data
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        var rowNum = 1
        var grandTotal = 0L

        for ((index, trx) in transactions.withIndex()) {
            val row = sheet.createRow(rowNum++)
            val date = trx.tanggal.toDate()

            // Format Item: Ganti titik koma dengan baris baru (Alt+Enter di Excel)
            val itemsDetail = trx.items.joinToString("\n") { item ->
                "- ${item.nama_menu} (${item.qty}x)"
            }

            // Col 0: No
            createCell(row, 0, (index + 1).toString(), dataStyle)
            // Col 1: Tanggal
            createCell(row, 1, dateFormat.format(date), dataStyle)
            // Col 2: Waktu
            createCell(row, 2, timeFormat.format(date), dataStyle)
            // Col 3: Kasir
            createCell(row, 3, trx.nama_kasir, dataStyle)
            // Col 4: Rincian
            createCell(row, 4, itemsDetail, dataStyle)
            // Col 5: Total (Angka)
            val totalCell = row.createCell(5)
            totalCell.setCellValue(trx.total_harga.toDouble())
            totalCell.cellStyle = currencyStyle

            grandTotal += trx.total_harga
        }

        // 4. Baris Total Akhir
        val footerRow = sheet.createRow(rowNum)
        val totalLabelCell = footerRow.createCell(4)
        totalLabelCell.setCellValue("TOTAL PENDAPATAN")
        totalLabelCell.cellStyle = headerStyle // Pakai style header agar tebal

        val totalValueCell = footerRow.createCell(5)
        totalValueCell.setCellValue(grandTotal.toDouble())
        totalValueCell.cellStyle = headerStyle // Pakai style header agar tebal & border

        // 5. Auto Size Kolom (Agar lebar kolom pas dengan teks)
        // Hati-hati: Auto size bisa lambat jika datanya ribuan baris
        sheet.setColumnWidth(0, 1500) // No (Fixed)
        sheet.setColumnWidth(1, 3000) // Tanggal
        sheet.setColumnWidth(2, 2000) // Waktu
        sheet.setColumnWidth(3, 4000) // Kasir
        sheet.setColumnWidth(4, 8000) // Item (Lebar)
        sheet.setColumnWidth(5, 4000) // Total

        // 6. Simpan File
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
            val fileName = "Laporan_Geprek_$timestamp.xlsx" // Ekstensi .xlsx
            val file = File(context.cacheDir, fileName)
            val fileOut = FileOutputStream(file)
            workbook.write(fileOut)
            fileOut.close()
            workbook.close()

            shareExcelFile(file)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCell(row: Row, col: Int, value: String, style: CellStyle) {
        val cell = row.createCell(col)
        cell.setCellValue(value)
        cell.cellStyle = style
    }

    private fun shareExcelFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            // MIME type khusus untuk Excel .xlsx
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan (Excel)")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Bagikan Excel via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}