package com.kelompok2.aplikasi_kasir_geprek.ui.admin.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AdminHomeScreen() {
    // Box digunakan untuk menempatkan konten di tengah
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Selamat Datang, Admin!",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}