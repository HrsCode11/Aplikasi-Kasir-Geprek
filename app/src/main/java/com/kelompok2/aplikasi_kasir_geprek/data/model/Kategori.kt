package com.kelompok2.aplikasi_kasir_geprek.data.model

data class Kategori(
    val id: String = "",
    val nama_kategori: String = ""
) {
    // Ini membantu dropdown menampilkannya dengan benar
    override fun toString(): String {
        return nama_kategori
    }
}