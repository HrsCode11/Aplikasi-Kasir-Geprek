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
    fun addUser(email: String, password: String, username: String, role: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid

                if (uid != null) {
                    val userMap = hashMapOf(
                        "email" to email,
                        "username" to username,
                        "role" to role,
                        "status" to "active"
                    )
                    firestore.collection("user").document(uid).set(userMap).await()
                    callback(true, "User berhasil ditambahkan.")
                } else {
                    callback(false, "Gagal mendapatkan UID.")
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Terjadi error.")
            }
        }
    }

    // UPDATE: Mengubah data user di Firestore
    fun updateUser(user: UserUiState, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val userMap = hashMapOf(
                    "username" to user.username,
                    "role" to user.role
                )
                firestore.collection("user").document(user.id).update(userMap as Map<String, Any>).await()
                callback(true, "User berhasil diperbarui.")
            } catch (e: Exception) {
                callback(false, e.message ?: "Gagal memperbarui user.")
            }
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
}