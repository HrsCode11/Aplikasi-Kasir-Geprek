package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolamenu

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.data.model.Menu
import java.text.NumberFormat
import java.util.Locale

@Composable
fun KelolaMenuScreen(
    viewModel: KelolaMenuViewModel = viewModel()
) {
    // --- STATE MANAGEMENT ---

    // Mengambil daftar menu dan kategori dari ViewModel
    val menuList by viewModel.menuList.collectAsState()
    val kategoriList by viewModel.kategoriList.collectAsState()

    // State untuk mengontrol dialog
    var showDialog by remember { mutableStateOf(false) }
    // State untuk membedakan mode Tambah (null) atau mode Edit (berisi data)
    var menuToEdit by remember { mutableStateOf<Menu?>(null) }
    // State untuk mengontrol FAB (Floating Action Button)
    var isFabExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // --- UI LAYOUT ---
    Scaffold(
        // Tombol FAB yang bisa expand/collapse
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFabExpanded) {
                        // Jika sudah expanded, buka dialog dalam mode "Tambah"
                        menuToEdit = null // Pastikan null untuk mode Tambah
                        showDialog = true
                    } else {
                        // Jika belum, expand dulu
                        isFabExpanded = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Menu") },
                text = { Text("Tambah Menu") },
                expanded = isFabExpanded
            )
        },
        // Latar belakang transparan agar gambar background utama terlihat
        containerColor = Color.Transparent
    ) { paddingValues ->

        // --- Tampilan Daftar Menu ---
        if (menuList.isEmpty()) {
            // Tampilan loading jika daftar masih kosong
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Memuat data menu...", color = Color.Black)
            }
        } else {
            // Tampilkan daftar menu menggunakan LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuList, key = { it.id }) { menu ->
                    MenuItemCard(
                        menu = menu,
                        onEditClick = {
                            // Saat "Edit" diklik, isi menuToEdit dan buka dialog
                            menuToEdit = menu
                            showDialog = true
                            isFabExpanded = false // Tutup FAB
                        },
                        onDeleteClick = {
                            // Panggil fungsi hapus dari ViewModel
                            viewModel.deleteMenu(menu.id) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // --- LOGIKA DIALOG (Tambah/Edit) ---
        if (showDialog) {
            MenuDialog(
                menuToEdit = menuToEdit, // Kirim data menu (null jika mode Tambah)
                kategoriList = kategoriList,
                onDismiss = {
                    showDialog = false
                    isFabExpanded = false // Tutup FAB saat dialog ditutup
                },
                onSave = { nama, harga, kategori ->
                    if (menuToEdit == null) {
                        // --- Mode TAMBAH ---
                        viewModel.addMenu(nama, harga, kategori) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showDialog = false
                                isFabExpanded = false
                            }
                        }
                    } else {
                        // --- Mode EDIT ---
                        viewModel.updateMenu(menuToEdit!!.id, nama, harga, kategori) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showDialog = false
                                isFabExpanded = false
                            }
                        }
                    }
                }
            )
        }
    }
}

// --- Composable untuk Satu Item Menu (Tanpa Gambar) ---
@Composable
private fun MenuItemCard(
    menu: Menu,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Format harga ke format Rupiah (Contoh: Rp 15.000)
    val formattedPrice = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(menu.harga)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom untuk Teks (Nama, Kategori, Harga)
            Column(modifier = Modifier.weight(1f)) {
                Text(menu.nama_menu, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(menu.nama_kategori, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(formattedPrice, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            // Tombol Aksi (Edit, Delete)
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFFA000))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}