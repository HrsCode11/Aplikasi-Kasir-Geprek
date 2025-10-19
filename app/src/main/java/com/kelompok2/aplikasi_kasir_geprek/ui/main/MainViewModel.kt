package com.kelompok2.aplikasi_kasir_geprek.ui.main

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainViewModel : ViewModel() {

    private val auth = Firebase.auth

    fun signOut() {
        auth.signOut()
    }
}