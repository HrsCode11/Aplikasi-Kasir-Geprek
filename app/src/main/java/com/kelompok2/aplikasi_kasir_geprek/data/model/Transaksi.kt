package com.kelompok2.aplikasi_kasir_geprek.data.model

import com.google.firebase.Timestamp

data class TransaksiItem(
    val id_menu: String = "",
    val nama_menu: String = "",
    val harga: Int = 0,
    val qty: Int = 0,
    val sub_total: Int = 0
)

// Data class untuk dokumen transaksi utama
data class Transaksi(
    val id: String = "",
    val tanggal: Timestamp = Timestamp.now(),
    val total_harga: Int = 0,
    val id_user: String = "",
    val nama_kasir: String = "",
    val items: List<TransaksiItem> = emptyList()
)