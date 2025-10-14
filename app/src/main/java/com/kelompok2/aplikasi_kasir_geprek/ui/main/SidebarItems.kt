package com.kelompok2.aplikasi_kasir_geprek.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

// Data class untuk merepresentasikan satu item di sidebar
data class SidebarItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

// Daftar item untuk Admin
val adminSidebarItems = listOf(
    SidebarItem("Transaksi", "transaksi", Icons.Default.AddCircle),
    SidebarItem("Kelola Menu", "kelola_menu", Icons.Default.List),
    SidebarItem("Kelola User", "kelola_user", Icons.Default.Face),
    SidebarItem("Monitoring", "monitoring", Icons.Default.DateRange),
    SidebarItem("Riwayat", "riwayat", Icons.Default.Home)
)

// Daftar item untuk Kasir
val kasirSidebarItems = listOf(
    SidebarItem("Transaksi", "transaksi", Icons.Default.AddCircle),
    SidebarItem("Monitoring", "monitoring", Icons.Default.DateRange),
    SidebarItem("Riwayat", "riwayat", Icons.Default.Home)
)