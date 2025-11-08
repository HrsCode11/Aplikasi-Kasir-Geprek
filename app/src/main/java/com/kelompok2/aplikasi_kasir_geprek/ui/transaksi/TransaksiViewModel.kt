package com.kelompok2.aplikasi_kasir_geprek.ui.transaksi

import android.util.Log
import androidx.lifecycle.ViewModel
import com.kelompok2.aplikasi_kasir_geprek.data.model.Kategori
import com.kelompok2.aplikasi_kasir_geprek.data.model.Menu
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

// Data class untuk menyimpan item di keranjang
data class CartItem(val menu: Menu, val quantity: Int)

// Data class untuk satu item di dalam dokumen transaksi
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

class TransaksiViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    // State untuk daftar menu lengkap dari Firestore
    private val _menuList = MutableStateFlow<List<Menu>>(emptyList())
    val menuList = _menuList.asStateFlow()

    // State untuk daftar kategori dari Firestore
    private val _kategoriList = MutableStateFlow<List<Kategori>>(emptyList())
    val kategoriList = _kategoriList.asStateFlow()

    // State untuk menyimpan item yang ada di keranjang
    private val _cartItems = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cartItems = _cartItems.asStateFlow()

    // State untuk melacak kategori yang sedang dipilih
    private val _selectedKategoriId = MutableStateFlow<String?>("all")
    val selectedKategoriId = _selectedKategoriId.asStateFlow()

    init {
        loadMenu()
        loadKategori()
    }

    // Mengambil data menu dari Firestore (Diperbarui agar mengambil doc.id)
    private fun loadMenu() {
        firestore.collection("menu")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("TransaksiVM", "Listen failed.", error)
                    return@addSnapshotListener
                }
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

    // Mengambil data kategori dari Firestore (Diperbarui agar mengambil doc.id)
    private fun loadKategori() {
        firestore.collection("kategori")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("TransaksiVM", "Kategori listen failed.", error)
                    return@addSnapshotListener
                }
                val list = snapshots?.map { doc ->
                    Kategori(id = doc.id, nama_kategori = doc.getString("nama_kategori") ?: "") // Ambil ID Dokumen
                } ?: emptyList()
                // Tambahkan kategori "All" secara manual
                _kategoriList.value = listOf(Kategori(id = "all", nama_kategori = "All")) + list
            }
    }

    // Fungsi untuk mengubah filter kategori
    fun selectKategori(kategoriId: String) {
        _selectedKategoriId.value = kategoriId
    }

    fun addToCart(menu: Menu) {
        _cartItems.update { currentCart ->
            val cart = currentCart.toMutableMap()
            val existingItem = cart[menu.id]
            if (existingItem != null) {
                cart[menu.id] = existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                cart[menu.id] = CartItem(menu = menu, quantity = 1)
            }
            cart
        }
    }

    fun removeFromCart(menu: Menu) {
        _cartItems.update { currentCart ->
            val cart = currentCart.toMutableMap()
            val existingItem = cart[menu.id]
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    cart[menu.id] = existingItem.copy(quantity = existingItem.quantity - 1)
                } else {
                    cart.remove(menu.id)
                }
            }
            cart
        }
    }

    // FUNGSI BARU UNTUK MENGOSONGKAN KERANJANG
    fun clearCart() {
        _cartItems.value = emptyMap()
    }

    // FUNGSI UNTUK MENYIMPAN TRANSAKSI (REVISI)
    suspend fun simpanTransaksi(userId: String, username: String): Pair<Boolean, String> {
        val cart = _cartItems.value
        if (cart.isEmpty()) {
            return Pair(false, "Keranjang kosong.")
        }

        // Hitung total
        val totalHarga = cart.values.sumOf { it.menu.harga * it.quantity }

        // Ubah isi keranjang (Map) menjadi daftar List<TransaksiItem>
        val itemsList = cart.values.map { cartItem ->
            TransaksiItem(
                id_menu = cartItem.menu.id,
                nama_menu = cartItem.menu.nama_menu,
                harga = cartItem.menu.harga,
                qty = cartItem.quantity,
                sub_total = cartItem.menu.harga * cartItem.quantity
            )
        }

        // Buat dokumen transaksi baru
        val transDocRef = firestore.collection("transaksi").document()
        val transaksiBaru = Transaksi(
            id = transDocRef.id,
            tanggal = Timestamp.now(),
            total_harga = totalHarga,
            id_user = userId,
            nama_kasir = username,
            items = itemsList
        )

        // Simpan ke Firestore
        return try {
            transDocRef.set(transaksiBaru).await()
            Pair(true, "Transaksi berhasil disimpan!")
        } catch (e: Exception) {
            Log.e("TransaksiVM", "Gagal menyimpan transaksi", e)
            Pair(false, e.message ?: "Gagal menyimpan transaksi.")
        }
    }
}