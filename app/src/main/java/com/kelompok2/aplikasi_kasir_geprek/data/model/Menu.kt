package com.kelompok2.aplikasi_kasir_geprek.data.model

data class Menu(
    val id: String = "",
    val nama_menu: String = "",
    val harga: Int = 0,
    val id_kategori: String = "",
    val nama_kategori: String = "", // Denormalisasi untuk tampilan
   // val url_gambar: String = "" // Untuk menyimpan URL download gambar
)