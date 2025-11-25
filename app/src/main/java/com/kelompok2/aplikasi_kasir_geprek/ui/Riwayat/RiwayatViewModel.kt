package com.kelompok2.aplikasi_kasir_geprek.ui.riwayat

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class RiwayatViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val COLLECTION_PATH = "transaksi"
    private val TAG = "RiwayatViewModel"

    private val _riwayatList = MutableStateFlow<List<Transaksi>>(emptyList())
    val riwayatList = _riwayatList.asStateFlow()

    private val _detailTransaksi = MutableStateFlow<Transaksi?>(null)
    val detailTransaksi = _detailTransaksi.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail = _isLoadingDetail.asStateFlow()

    init {
        loadRiwayat()
    }

    private fun loadRiwayat() {
        firestore.collection(COLLECTION_PATH)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for riwayat list.", error)
                    return@addSnapshotListener
                }

                _riwayatList.value = snapshots?.mapNotNull { doc ->
                    try {
                        doc.toObject(Transaksi::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Transaksi: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
            }
    }

    suspend fun loadDetailTransaksi(transaksiId: String) {
        if (transaksiId.isBlank()) return

        _isLoadingDetail.value = true
        _detailTransaksi.value = null

        try {
            val doc = firestore.collection(COLLECTION_PATH).document(transaksiId).get().await()
            val transaksi = doc.toObject(Transaksi::class.java)?.copy(id = doc.id)
            _detailTransaksi.value = transaksi
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil detail transaksi: $transaksiId", e)
            _detailTransaksi.value = null
        } finally {
            _isLoadingDetail.value = false
        }
    }

    fun clearDetailTransaksi() {
        _detailTransaksi.value = null
        _isLoadingDetail.value = false
    }

    suspend fun hapusRiwayatById(transaksiId: String): Pair<Boolean, String> {
        if (transaksiId.isBlank()) return Pair(false, "ID Transaksi tidak valid.")

        return try {
            firestore.collection(COLLECTION_PATH).document(transaksiId).delete().await()
            Pair(true, "Riwayat berhasil dihapus.")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal hapus item: $transaksiId", e)
            Pair(false, "Gagal menghapus: ${e.message}")
        }
    }
}
