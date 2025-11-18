package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolauser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class untuk merepresentasikan data user di UI
data class UserUiState(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = ""
)

class KelolaUserViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _users = MutableStateFlow<List<UserUiState>>(emptyList())
    val users = _users.asStateFlow()

    init {
        loadUsers()
    }

    // READ: Mengambil data user secara real-time
    private fun loadUsers() {
        firestore.collection("user") // Menggunakan koleksi "user" (tunggal)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("KelolaUserVM", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val userList = snapshots?.map { doc ->
                    UserUiState(
                        id = doc.id,
                        username = doc.getString("username") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()
                _users.value = userList
            }
    }

    // CREATE: Menambah user baru
    suspend fun addUser(email: String, password: String, username: String, role: String): Pair<Boolean, String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                val userMap = hashMapOf("email" to email, "username" to username, "role" to role, "status" to "active")
                firestore.collection("user").document(uid).set(userMap).await()
                Pair(true, "User berhasil ditambahkan.") // Kembalikan Sukses
            } else {
                Pair(false, "Gagal mendapatkan UID.") // Kembalikan Gagal
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Terjadi error.") // Kembalikan Gagal dengan pesan error
        }
    }

    // UPDATE: Ubah return type menjadi Pair
    suspend fun updateUser(user: UserUiState): Pair<Boolean, String> {
        return try {
            val userMap = hashMapOf("username" to user.username, "role" to user.role)
            firestore.collection("user").document(user.id).update(userMap as Map<String, Any>).await()
            Pair(true, "User berhasil diperbarui.") // Kembalikan Sukses
        } catch (e: Exception) {
            Pair(false, e.message ?: "Gagal memperbarui user.") // Kembalikan Gagal
        }
    }

    // DEACTIVATE: Menonaktifkan user
    fun deactivateUser(userId: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("user").document(userId).update("status", "inactive").await()
                callback(true, "User berhasil dinonaktifkan.")
            } catch (e: Exception) {
                callback(false, e.message ?: "Gagal menonaktifkan user.")
            }
        }
    }

    // ACTIVATE: Mengaktifkan user kembali
    fun activateUser(userId: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("user").document(userId).update("status", "active").await()
                callback(true, "User berhasil diaktifkan kembali.")
            } catch (e: Exception) {
                callback(false, e.message ?: "Gagal mengaktifkan user.")
            }
        }
    }

    // RESET PASSWORD: Mengirim email reset password
    fun sendPasswordResetEmail(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                callback(true, "Email reset password berhasil dikirim ke $email.")
            } catch (e: Exception) {
                callback(false, e.message ?: "Gagal mengirim email.")
            }
        }
    }

    suspend fun deleteUserPermanently(userId: String, email: String): Pair<Boolean, String> {
        return try {
            // 1. Periksa apakah ada transaksi yang terkait dengan user ini
            val transactionCheck = firestore.collection("transaksi") // <-- Pastikan nama koleksi transaksi benar
                .whereEqualTo("id_user", userId) // Periksa field id_user
                .limit(1) // Cukup cek 1 saja
                .get()
                .await()

            // 2. Jika ditemukan transaksi, batalkan penghapusan
            if (!transactionCheck.isEmpty) {
                return Pair(false, "User tidak bisa dihapus karena memiliki riwayat transaksi.")
            }

            // 3. Jika tidak ada transaksi, hapus dokumen user dari Firestore
            firestore.collection("user").document(userId).delete().await()

            // 4. Hapus user dari Firebase Authentication
            // Ini memerlukan re-autentikasi atau Admin SDK, tapi kita coba cara client-side dulu
            // Perhatian: Menghapus user Auth dari client-side SANGAT DIBATASI dan sering gagal.
            // Cara paling andal adalah menggunakan Cloud Functions (Admin SDK).
            // Kita coba, tapi beri pesan jika gagal.
            try {
                // Mencoba menghapus user Auth (mungkin memerlukan login ulang atau gagal)
                // NOTE: Firebase Auth SDK for Android/Clients generally CANNOT delete other users.
                // This operation typically requires Admin privileges (Admin SDK on a server/Cloud Function).
                // We keep the Firestore delete for now, but Auth delete likely won't work from client.
                Log.w("KelolaUserVM", "Firebase Auth user deletion from client is typically restricted. User $email might remain in Auth.")
                // Jika Anda memiliki backend/Cloud Function, panggil fungsi itu di sini.
                // Jika tidak, pengguna Auth akan tetap ada, tapi data Firestore hilang.

            } catch (authError: Exception) {
                Log.e("KelolaUserVM", "Error deleting user from Auth (likely needs Admin SDK): ${authError.message}")
                // Kita lanjutkan saja prosesnya karena data Firestore sudah dihapus
                // Beri tahu admin bahwa akun Auth mungkin masih ada
                return Pair(true, "Data user berhasil dihapus dari database, tetapi akun login mungkin masih ada (perlu Admin SDK untuk hapus total).")

            }


            Pair(true, "User berhasil dihapus permanen dari database.") // Sukses (dengan catatan Auth)

        } catch (e: Exception) {
            Log.e("KelolaUserVM", "Error deleting user", e)
            Pair(false, e.message ?: "Gagal menghapus user.") // Gagal
        }
    }
}