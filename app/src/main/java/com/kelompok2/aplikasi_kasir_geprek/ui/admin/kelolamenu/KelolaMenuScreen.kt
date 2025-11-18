package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolamenu

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
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
    // === State Management ===
    val menuList by viewModel.menuList.collectAsState()
    val kategoriList by viewModel.kategoriList.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var menuToEdit by remember { mutableStateOf<Menu?>(null) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // State untuk konfirmasi hapus
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuToDelete by remember { mutableStateOf<Menu?>(null) }

    val context = LocalContext.current

    // === UI Layout ===
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFabExpanded) {
                        menuToEdit = null // Mode Tambah
                        showDialog = true
                    } else {
                        isFabExpanded = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Menu") },
                text = { Text("Tambah Menu") },
                expanded = isFabExpanded
            )
        },
        containerColor = Color.Transparent // Latar belakang transparan
    ) { paddingValues ->

        // === Tampilan Daftar Menu ===
        if (menuList.isEmpty()) {
            // Tampilan loading
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
            // Daftar menu
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
                            menuToEdit = menu // Set data untuk mode Edit
                            showDialog = true
                            isFabExpanded = false
                        },
                        onDeleteClick = {
                            // Tampilkan dialog konfirmasi hapus
                            menuToDelete = menu
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }

        // === Dialog Tambah/Edit Menu ===
        if (showDialog) {
            MenuDialog(
                menuToEdit = menuToEdit,
                kategoriList = kategoriList,
                onDismiss = {
                    showDialog = false
                    isFabExpanded = false
                },
                onSave = { nama, harga, kategori ->
                    if (menuToEdit == null) {
                        // Simpan menu baru
                        viewModel.addMenu(nama, harga, kategori) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showDialog = false
                                isFabExpanded = false
                            }
                        }
                    } else {
                        // Update menu yang ada
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

        // === Dialog Konfirmasi Hapus ===
        if (showDeleteConfirmDialog && menuToDelete != null) {
            DeleteConfirmationDialog(
                itemName = menuToDelete!!.nama_menu,
                onConfirm = {
                    // Hapus menu setelah konfirmasi
                    viewModel.deleteMenu(menuToDelete!!.id) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    showDeleteConfirmDialog = false
                    menuToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    menuToDelete = null
                }
            )
        }
    }
}

// === Composable untuk Satu Item Menu (Tanpa Gambar) ===
@Composable
private fun MenuItemCard(
    menu: Menu,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            // Kolom Teks
            Column(modifier = Modifier.weight(1f)) {
                Text(menu.nama_menu, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(menu.nama_kategori, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(formattedPrice, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            // Tombol Aksi
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

// === Composable untuk Dialog Konfirmasi Hapus ===
@Composable
private fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Peringatan") },
        title = { Text("Konfirmasi Hapus") },
        text = { Text("Apakah Anda yakin ingin menghapus \"$itemName\"?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Hapus") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}