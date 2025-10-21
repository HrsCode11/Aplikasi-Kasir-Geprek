package com.kelompok2.aplikasi_kasir_geprek.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

// Data class untuk merepresentasikan satu item di sidebar
data class SidebarItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

// Daftar item untuk Admin
val adminSidebarItems = listOf(
    SidebarItem("Transaksi", "transaksi", Icons.Default.ShoppingCart),
    SidebarItem("Kelola Menu", "kelola_menu", Icons.Default.RestaurantMenu),
    SidebarItem("Kelola User", "kelola_user", Icons.Default.Face),
    SidebarItem("Monitoring", "monitoring", Icons.Default.BarChart),
    SidebarItem("Riwayat", "riwayat", Icons.Default.History)
)

// Daftar item untuk Kasir
val kasirSidebarItems = listOf(
    SidebarItem("Transaksi", "transaksi", Icons.Default.ShoppingCart),
    SidebarItem("Monitoring", "monitoring", Icons.Default.BarChart),
    SidebarItem("Riwayat", "riwayat", Icons.Default.History)
)