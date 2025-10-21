package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolamenu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.data.model.Kategori
import com.kelompok2.aplikasi_kasir_geprek.data.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class KelolaMenuViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    // Firebase Storage dihapus

    private val _menuList = MutableStateFlow<List<Menu>>(emptyList())
    val menuList = _menuList.asStateFlow()

    private val _kategoriList = MutableStateFlow<List<Kategori>>(emptyList())
    val kategoriList = _kategoriList.asStateFlow()

    init {
        loadMenu()
        loadKategori()
    }

    // READ Menu
    private fun loadMenu() {
        firestore.collection("menu")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("KelolaMenuVM", "Listen failed.", error)
                    return@addSnapshotListener
                }
                // Petakan dokumen secara manual untuk mendapatkan ID
                _menuList.value = snapshots?.map { doc ->
                    Menu(
                        id = doc.id,
                        nama_menu = doc.getString("nama_menu") ?: "",
                        harga = doc.getLong("harga")?.toInt() ?: 0,
                        id_kategori = doc.getString("id_kategori") ?: "",
                        nama_kategori = doc.getString("nama_kategori") ?: ""
                    )
                } ?: emptyList()
            }
    }

    // READ Kategori
    private fun loadKategori() {
        firestore.collection("kategori")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("KelolaMenuVM", "Listen failed for kategori.", error)
                    return@addSnapshotListener
                }
                val list = snapshots?.map { doc ->
                    Kategori(id = doc.id, nama_kategori = doc.getString("nama_kategori") ?: "")
                } ?: emptyList()
                _kategoriList.value = list
            }
    }


    fun addMenu(
        nama: String,
        harga: Int,
        kategori: Kategori,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val menuDocRef = firestore.collection("menu").document()
                val menuData = Menu(
                    id = menuDocRef.id,
                    nama_menu = nama,
                    harga = harga,
                    id_kategori = kategori.id,
                    nama_kategori = kategori.nama_kategori
                )
                menuDocRef.set(menuData).await()
                callback(true, "Menu berhasil ditambahkan.")
            } catch (e: Exception) {
                Log.e("KelolaMenuVM", "Error adding menu", e)
                callback(false, e.message ?: "Gagal menambah menu.")
            }
        }
    }

    // UPDATE: Mengubah data menu
    fun updateMenu(
        menuId: String,
        nama: String,
        harga: Int,
        kategori: Kategori,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val menuRef = firestore.collection("menu").document(menuId)
                val updates = mapOf(
                    "nama_menu" to nama,
                    "harga" to harga,
                    "id_kategori" to kategori.id,
                    "nama_kategori" to kategori.nama_kategori
                )
                menuRef.update(updates).await()
                callback(true, "Menu berhasil diperbarui.")
            } catch (e: Exception) {
                Log.e("KelolaMenuVM", "Error updating menu", e)
                callback(false, e.message ?: "Gagal memperbarui menu.")
            }
        }
    }

    // DELETE: Menghapus menu
    fun deleteMenu(menuId: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("menu").document(menuId).delete().await()
                callback(true, "Menu berhasil dihapus.")
            } catch (e: Exception) {
                Log.e("KelolaMenuVM", "Error deleting menu", e)
                callback(false, e.message ?: "Gagal menghapus menu.")
            }
        }
    }
}