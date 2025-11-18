package com.kelompok2.aplikasi_kasir_geprek.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Patterns

sealed class LoginResult {
    data class Success(val role: String, val username: String, val uid: String) : LoginResult()    data class Error(val message: String) : LoginResult()
}

class LoginViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    suspend fun signInWithUsername(username: String, password: String): LoginResult {
        if (username.isBlank() || password.isBlank()) {
            return LoginResult.Error("Username dan password tidak boleh kosong.")
        }
        return try {
            val querySnapshot = firestore.collection("user")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return LoginResult.Error("Username tidak ditemukan.")
            }

            val userDocument = querySnapshot.documents[0]
            val email = userDocument.getString("email")
                ?: return LoginResult.Error("Data email untuk user ini tidak ada.")

            auth.signInWithEmailAndPassword(email, password).await()

            val status = userDocument.getString("status") ?: "inactive"
            if (status != "active") {
                auth.signOut()
                return LoginResult.Error("Akun Anda tidak aktif. Silahkan hubungi Admin.")
            }

            // DAPATKAN UID SETELAH LOGIN
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return LoginResult.Error("Gagal mendapatkan UID.")

            val role = userDocument.getString("role") ?: "unknown"
            val fetchedUsername = userDocument.getString("username") ?: "Pengguna"

            LoginResult.Success(role, fetchedUsername, uid)

        } catch (e: Exception) {
            val errorMessage = when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Password salah."
                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Email tidak terdaftar."
                else -> e.message ?: "Terjadi error tidak diketahui."
            }
            LoginResult.Error(errorMessage)
        }
    }

    fun sendPasswordResetEmail(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // Validasi format email sederhana di client sebelum mengirim
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    callback(false, "Format email tidak valid.")
                    return@launch // Hentikan jika format salah
                }

                auth.sendPasswordResetEmail(email).await()
                // Berikan pesan yang tidak mengonfirmasi keberadaan email
                callback(true, "Jika email Anda terdaftar, link reset password akan dikirim.")
            } catch (e: Exception) {
                callback(false, e.message ?: "Gagal mengirim permintaan reset password.")
            }
        }
    }
}