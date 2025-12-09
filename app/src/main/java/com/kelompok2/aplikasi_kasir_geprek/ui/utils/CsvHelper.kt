package com.kelompok2.aplikasi_kasir_geprek.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

class CsvHelper(private val context: Context) {

    fun exportTransactionsToCsv(transactions: List<Transaksi>) {
        // 1. Buat Header CSV
        val csvHeader = "ID Transaksi,Tanggal,Waktu,Kasir,Detail Item,Total Harga\n"

        // 2. Buat Isi CSV
        val sb = StringBuilder()
        sb.append(csvHeader)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (trx in transactions) {
            val date = trx.tanggal.toDate()
            // Gabungkan nama item dan qty menjadi satu string (misal: "Ayam (2); Es Teh (1)")
            val itemsSummary = trx.items.joinToString("; ") { "${it.nama_menu} (${it.qty})" }

            // Hindari masalah koma dalam teks dengan membungkus field teks pakai tanda kutip
            sb.append("${trx.id},")
            sb.append("${dateFormat.format(date)},")
            sb.append("${timeFormat.format(date)},")
            sb.append("\"${trx.nama_kasir}\",") // Quote nama kasir
            sb.append("\"$itemsSummary\",") // Quote detail item karena ada koma/titik koma
            sb.append("${trx.total_harga}\n")
        }

        // 3. Simpan ke File Sementara (Cache)
        try {
            val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            writer.write(sb.toString())
            writer.close()

            // 4. Bagikan File (Share Intent)
            shareCsvFile(file)

        } catch (e: Exception) {
            e.printStackTrace()
            // Anda bisa menambahkan callback error jika mau
        }
    }

    private fun shareCsvFile(file: File) {
        // Menggunakan FileProvider agar aman dan kompatibel dengan Android terbaru
        // PENTING: Kita perlu setup provider di AndroidManifest.xml nanti
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Bagikan Laporan CSV")
        // Flag ini penting agar activity di luar aplikasi kita bisa baca file-nya
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}