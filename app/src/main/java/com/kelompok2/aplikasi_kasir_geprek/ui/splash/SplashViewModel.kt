package com.kelompok2.aplikasi_kasir_geprek.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Kelas untuk merepresentasikan status autentikasi
sealed class AuthState {
    data object Loading : AuthState() // Status saat sedang memeriksa
    // Tambahan UID
    data class Authenticated(val role: String, val username: String, val uid: String) : AuthState() // User sudah login
    data object Unauthenticated : AuthState() // User belum login
}

class SplashViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Jika ada user, ambil datanya dari Firestore
                try {
                    // Gunakan "user" (tunggal) sesuai koreksi sebelumnya
                    val doc = firestore.collection("user").document(currentUser.uid).get().await()
                    if (doc != null && doc.exists()) {
                        val role = doc.getString("role") ?: ""
                        val username = doc.getString("username") ?: ""
                        // UBAH DI SINI: Kirimkan uid juga
                        _authState.value = AuthState.Authenticated(role, username, currentUser.uid)
                    } else {
                        // Data di Firestore tidak ada, paksa logout
                        auth.signOut()
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    // Error saat mengambil data, paksa logout
                    auth.signOut()
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                // Jika tidak ada user, langsung set status Unauthenticated
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
}