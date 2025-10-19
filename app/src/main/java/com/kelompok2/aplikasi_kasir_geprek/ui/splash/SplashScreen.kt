package com.kelompok2.aplikasi_kasir_geprek.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplashScreen(
    splashViewModel: SplashViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: (role: String, username: String) -> Unit
) {
    val authState by splashViewModel.authState.collectAsState()

    // LaunchedEffect akan berjalan saat authState berubah
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                // Jika sudah terautentikasi, panggil navigasi ke MainScreen
                onNavigateToMain(state.role, state.username)
            }
            is AuthState.Unauthenticated -> {
                // Jika tidak, panggil navigasi ke LoginScreen
                onNavigateToLogin()
            }
            AuthState.Loading -> {
                // Lakukan tidak ada, biarkan loading indicator tampil
            }
        }
    }

    // Tampilan loading
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}