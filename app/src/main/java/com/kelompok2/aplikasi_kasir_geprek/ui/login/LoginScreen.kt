package com.kelompok2.aplikasi_kasir_geprek.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.R
import com.kelompok2.aplikasi_kasir_geprek.ui.theme.AplikasiKasirGeprekTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (role: String, username: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background_login),
            contentDescription = "Login Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds // Sesuaikan skala gambar
        )

        // Overlay untuk sedikit meredupkan gambar jika perlu (opsional)
        // Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center, // Pusatkan konten utama
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dorong konten ke bagian bawah sedikit agar tidak terlalu ke tengah
            Spacer(modifier = Modifier.weight(0.5f))

            // Bagian Judul "Masuk"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Masuk",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black // Sesuaikan warna teks
                )
                Text(
                    text = "Selamat Datang, Silahkan masuk!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray // Sesuaikan warna teks
                )
            }

            Spacer(modifier = Modifier.height(32.dp)) // Jarak antara judul dan input

            // Input field untuk Username
            CustomLoginTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username" // Menggunakan placeholder
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input field untuk Password
            CustomLoginTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Login
            Button(
                onClick = {
                    coroutineScope.launch {
                        when (val result = loginViewModel.signInWithUsername(username, password)) {
                            is LoginResult.Success -> {
                                onLoginSuccess(result.role, result.username)
                            }
                            is LoginResult.Error -> {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Tinggi tombol lebih besar
                shape = RoundedCornerShape(12.dp), // Sudut tombol bulat
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F), // Warna tombol merah
                    contentColor = Color.White
                )
            ) {
                Text("MASUK", style = MaterialTheme.typography.titleMedium)
            }

            // Dorong konten ke bagian atas sedikit agar tidak terlalu ke tengah
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

// Composable kustom untuk TextField agar sesuai desain mockup
@Composable
fun CustomLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: PasswordVisualTransformation? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray) }, // Placeholder dengan warna
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // Tinggi field lebih besar
        shape = RoundedCornerShape(12.dp), // Sudut field bulat
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF535353), // Warna latar belakang saat fokus
            unfocusedContainerColor = Color(0xFF535353), // Warna latar belakang saat tidak fokus
            focusedTextColor = Color.White, // Warna teks saat fokus
            unfocusedTextColor = Color.White, // Warna teks saat tidak fokus
            cursorColor = Color.White, // Warna kursor
            focusedIndicatorColor = Color.Transparent, // Hilangkan indikator bawah saat fokus
            unfocusedIndicatorColor = Color.Transparent, // Hilangkan indikator bawah saat tidak fokus
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        visualTransformation = visualTransformation ?: VisualTransformation.None
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AplikasiKasirGeprekTheme {
        LoginScreen(onLoginSuccess = {} as (String, String) -> Unit)
    }
}