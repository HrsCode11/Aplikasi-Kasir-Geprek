package com.kelompok2.aplikasi_kasir_geprek.ui.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

sealed class LoginResult {
    data class Success(val role: String, val username: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class LoginViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    suspend fun signInWithUsername(username: String, password: String): LoginResult {
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

            val role = userDocument.getString("role") ?: "unknown"
            val fetchedUsername = userDocument.getString("username") ?: "Pengguna"

            LoginResult.Success(role, fetchedUsername)

        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Terjadi error tidak diketahui.")
        }
    }
}