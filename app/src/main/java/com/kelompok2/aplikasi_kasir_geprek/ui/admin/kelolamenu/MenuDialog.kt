package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolamenu

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kelompok2.aplikasi_kasir_geprek.data.model.Kategori
import com.kelompok2.aplikasi_kasir_geprek.data.model.Menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDialog(
    menuToEdit: Menu?, // Sekarang bisa menerima data menu untuk diedit
    kategoriList: List<Kategori>,
    onDismiss: () -> Unit,
    // Callback disederhanakan, tidak ada Uri
    onSave: (nama: String, harga: Int, kategori: Kategori) -> Unit
) {
    // === STATE VARIABLES ===
    // Inisialisasi state dengan data dari menuToEdit jika ada
    var namaMenu by remember { mutableStateOf(menuToEdit?.nama_menu ?: "") }
    var harga by remember { mutableStateOf(menuToEdit?.harga?.toString() ?: "") }

    // Cari Kategori yang cocok di dalam list
    val initialKategori = kategoriList.find { it.id == menuToEdit?.id_kategori }
    var selectedKategori by remember { mutableStateOf(initialKategori) }

    var isKategoriExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // === DIALOG UI ===
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (menuToEdit == null) "Tambah Menu Baru" else "Edit Menu") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                // Bagian Pemilih Gambar dihapus

                // --- Input Field Nama Menu ---
                OutlinedTextField(
                    value = namaMenu,
                    onValueChange = { namaMenu = it },
                    label = { Text("Nama Menu") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Input Field Harga ---
                OutlinedTextField(
                    value = harga,
                    onValueChange = { newValue ->
                        harga = newValue.filter { it.isDigit() }.take(9)
                    },
                    label = { Text("Harga") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Dropdown Kategori ---
                ExposedDropdownMenuBox(
                    expanded = isKategoriExpanded,
                    onExpandedChange = { isKategoriExpanded = !isKategoriExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedKategori?.nama_kategori ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKategoriExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isKategoriExpanded,
                        onDismissRequest = { isKategoriExpanded = false }
                    ) {
                        kategoriList.forEach { kategori ->
                            DropdownMenuItem(
                                text = { Text(kategori.nama_kategori) },
                                onClick = {
                                    selectedKategori = kategori
                                    isKategoriExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hargaInt = harga.toIntOrNull()
                    // Validasi disederhanakan (tanpa imageUri)
                    if (namaMenu.isBlank() || hargaInt == null || selectedKategori == null) {
                        Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        onSave(namaMenu, hargaInt, selectedKategori!!)
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}