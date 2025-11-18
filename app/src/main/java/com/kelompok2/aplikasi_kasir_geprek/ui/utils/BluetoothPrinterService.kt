package com.kelompok2.aplikasi_kasir_geprek.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.kelompok2.aplikasi_kasir_geprek.ui.transaksi.CartItem
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BluetoothPrinterService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Perintah Dasar ESC/POS
    private val ESC_POS_RESET = byteArrayOf(0x1B, 0x40)
    private val ESC_POS_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_POS_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_POS_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_POS_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_POS_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_POS_FEED_LINES = byteArrayOf(0x1B, 0x64)

    private val PRINTER_CHARSET: Charset = Charsets.ISO_8859_1

    // SET Lebar 32 karakter untuk printer 58mm
    private val TOTAL_WIDTH = 32
    private val LINE_SEPARATOR = "-".repeat(TOTAL_WIDTH) + "\n"
    private val GANG = " ".repeat(TOTAL_WIDTH) + "\n"

    // Helper Format
    private fun formatHarga(harga: Int): String {
        val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(harga)
    }

    private fun getTanggalSekarang(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // Helper Padding (Disesuaikan untuk 32 Karakter)
    private fun createRow(left: String, right: String): String {
        val safeLeft = left.take(TOTAL_WIDTH - right.length - 1)
        val padding = TOTAL_WIDTH - safeLeft.length - right.length
        return safeLeft + " ".repeat(padding.coerceAtLeast(0)) + right + "\n"
    }

    private val COL_WIDTH_QTY = 5   // "999x "
    private val COL_WIDTH_TOTAL = 10  // " 9.999.999"
    private val COL_WIDTH_ITEM = 17 // Sisa: 32 - 5 - 10

    private fun createItemHeader(): String {
        val qty = "QTY".padEnd(COL_WIDTH_QTY)
        val item = "ITEM".padEnd(COL_WIDTH_ITEM)
        val total = "TOTAL".padStart(COL_WIDTH_TOTAL)
        return qty + item + total + "\n"
    }

    private fun createItemRow(item: CartItem): String {
        val qty = "${item.quantity}x".padEnd(COL_WIDTH_QTY)
        val subtotal = formatHarga(item.menu.harga * item.quantity).padStart(COL_WIDTH_TOTAL)
        val nama = item.menu.nama_menu
        val hargaSatuan = "@${formatHarga(item.menu.harga)}"

        val builder = StringBuilder()
        val itemLines = nama.chunked(COL_WIDTH_ITEM)

        itemLines.forEachIndexed { index, line ->
            if (index == 0) {
                builder.append(qty + line.padEnd(COL_WIDTH_ITEM) + subtotal + "\n")
            } else {
                builder.append("".padEnd(COL_WIDTH_QTY) + line.padEnd(COL_WIDTH_ITEM) + "".padStart(COL_WIDTH_TOTAL) + "\n")
            }
        }

        builder.append("".padEnd(COL_WIDTH_QTY) + hargaSatuan.padEnd(COL_WIDTH_ITEM) + "".padStart(COL_WIDTH_TOTAL) + "\n")
        return builder.toString()
    }


    @SuppressLint("MissingPermission")
    private fun findPrinterDevice(): BluetoothDevice? {
        if (bluetoothAdapter == null) {
            throw IOException("Perangkat ini tidak mendukung Bluetooth.")
        }
        if (!bluetoothAdapter!!.isEnabled) {
            throw IOException("Bluetooth tidak aktif. Mohon aktifkan Bluetooth.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Izin BLUETOOTH_CONNECT ditolak.")
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Izin BLUETOOTH ditolak.")
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        return pairedDevices?.firstOrNull {
            val deviceName = it.name.lowercase(Locale.getDefault())
            deviceName.contains("printer") ||
                    deviceName.contains("mtp") ||
                    deviceName.contains("rpp")
        }
    }

    @SuppressLint("MissingPermission")
    fun printStruk(
        cartItems: Map<String, CartItem>,
        totalHarga: Int,
        namaKasir: String
    ) {
        val device = findPrinterDevice()
            ?: throw IOException("Printer Bluetooth tidak ditemukan. Pastikan printer sudah di-pairing di Pengaturan Android.")

        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            outputStream = socket.outputStream

            // FUNGSI DENGAN DELAY
            // Helper baru untuk menulis BYTE (perintah)
            fun writeBytes(bytes: ByteArray) {
                outputStream.write(bytes)
                Thread.sleep(30) // Jeda 30ms agar printer tidak "tersedak"
            }

            // Helper baru untuk menulis STRING (teks)
            fun writeString(text: String) {
                outputStream.write(text.toByteArray(PRINTER_CHARSET))
                Thread.sleep(30) // Jeda 30ms agar printer tidak "tersedak"
            }

            // MULAI BUAT STRUK (Gunakan helper baru)
            writeBytes(ESC_POS_RESET)
            writeBytes(ESC_POS_ALIGN_CENTER)
            writeBytes(ESC_POS_BOLD_ON)

            // Header
            writeString("AYAM GEPREK MR.KRIUK\n")
            writeBytes(ESC_POS_BOLD_OFF)
            writeString("Jl. Bringin Kab. Ponorogo\n")
            writeString(getTanggalSekarang() + "\n")

            writeString(GANG)
            // Info Kasir
            writeBytes(ESC_POS_ALIGN_LEFT)
            writeString(createRow("Kasir: $namaKasir", ""))
            writeString(LINE_SEPARATOR)

            // Header Item
            writeString(createItemHeader())
            writeString(LINE_SEPARATOR)

            // Loop untuk semua item
            cartItems.values.forEach { item ->
                writeString(createItemRow(item)) // Ini akan meloop SEMUA item satu per satu
            }

            // Separator setelah item
            writeString(LINE_SEPARATOR)

            // Total Item
            val totalItems = cartItems.values.sumOf { it.quantity }
            writeString(createRow("Total Item:", "$totalItems"))

            // Total
            writeBytes(ESC_POS_ALIGN_RIGHT)
            writeBytes(ESC_POS_BOLD_ON)
            writeString(createRow("TOTAL", formatHarga(totalHarga)))
            writeBytes(ESC_POS_BOLD_OFF)

            // Footer
            writeBytes(ESC_POS_ALIGN_CENTER)
            writeString("\n")
            writeString("TERIMA KASIH\n")
            writeString("\n\n")

            // Dorong kertas
            writeBytes(ESC_POS_FEED_LINES)
            writeBytes(byteArrayOf(3))
            writeBytes(ESC_POS_RESET)
            // SELESEI BUAT STRUK

            outputStream.flush()
            Log.d("BluetoothPrinterService", "Struk berhasil dikirim ke printer.")

        } catch (e: Exception) {
            Log.e("BluetoothPrinterService", "Gagal mencetak", e)
            // Tambahkan throw agar TransaksiScreen bisa menangkap error ini
            throw IOException("Gagal menulis ke printer: ${e.message}")
        } finally {
            outputStream?.close()
            socket?.close()
        }
    }
}