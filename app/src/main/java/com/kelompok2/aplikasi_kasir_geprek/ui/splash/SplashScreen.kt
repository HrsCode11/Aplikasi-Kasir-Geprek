package com.kelompok2.aplikasi_kasir_geprek.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background // <-- IMPORT TAMBAHAN
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // <-- IMPORT TAMBAHAN
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.R

@Composable
fun SplashScreen(
    splashViewModel: SplashViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: (role: String, username: String) -> Unit
) {
    val authState by splashViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                onNavigateToMain(state.role, state.username)
            }
            is AuthState.Unauthenticated -> {
                onNavigateToLogin()
            }
            AuthState.Loading -> {
            }
        }
    }

    // Tampilan loading dengan Logo dan Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFC51E00)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Aplikasi",
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Color.White
            )
        }
    }
}