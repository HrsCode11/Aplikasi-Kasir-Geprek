package com.kelompok2.aplikasi_kasir_geprek.ui.riwayat

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.ui.transaksi.Transaksi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class RiwayatViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    // State untuk menyimpan daftar riwayat transaksi
    private val _riwayatList = MutableStateFlow<List<Transaksi>>(emptyList())
    val riwayatList = _riwayatList.asStateFlow()

    init {
        loadRiwayat()
    }

    // Mengambil data riwayat, diurutkan dari yang terbaru
    private fun loadRiwayat() {
        firestore.collection("transaksi")
            .orderBy("tanggal", Query.Direction.DESCENDING) // Terbaru di atas
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("RiwayatViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                _riwayatList.value = snapshots?.mapNotNull { doc ->
                    doc.toObject(Transaksi::class.java)
                } ?: emptyList()
            }
    }

    // FUNGSI LAMA (Bisa Anda hapus jika tidak diperlukan lagi)
    suspend fun hapusSemuaRiwayat(): Pair<Boolean, String> {
        return try {
            val querySnapshot = firestore.collection("transaksi").get().await()
            if (querySnapshot.isEmpty) {
                return Pair(true, "Riwayat sudah kosong.")
            }

            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Pair(true, "Semua riwayat berhasil dihapus.")

        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Gagal hapus batch", e)
            Pair(false, "Gagal menghapus riwayat: ${e.message}")
        }
    }

    // FUNGSI BARU UNTUK HAPUS PER ITEM
    suspend fun hapusRiwayatById(transaksiId: String): Pair<Boolean, String> {
        if (transaksiId.isBlank()) return Pair(false, "ID Transaksi tidak valid.")

        return try {
            firestore.collection("transaksi").document(transaksiId).delete().await()
            Pair(true, "Riwayat berhasil dihapus.")
        } catch (e: Exception) {
            Log.e("RiwayatViewModel", "Gagal hapus item", e)
            Pair(false, "Gagal menghapus: ${e.message}")
        }
    }
}