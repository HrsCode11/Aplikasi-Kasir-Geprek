package com.kelompok2.aplikasi_kasir_geprek.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.R
import com.kelompok2.aplikasi_kasir_geprek.ui.theme.AplikasiKasirGeprekTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import android.util.Patterns

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (role: String, username: String, uid: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Ekstraksi logika login agar bisa dipakai ulang (oleh tombol & keyboard)
    val performLogin = {
        focusManager.clearFocus()
        coroutineScope.launch {
            when (val result = loginViewModel.signInWithUsername(username, password)) {
                is LoginResult.Success -> {
                    onLoginSuccess(result.role, result.username, result.uid)
                }
                is LoginResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_login),
            contentDescription = "Login Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(3.5f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Masuk",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold).copy(fontSize = 48.sp),
                    color = Color.Black
                )
                Text(
                    text = "Selamat Datang, Silahkan masuk!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Input field untuk Username
            CustomLoginTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input field untuk Password
            CustomLoginTextField(
                modifier = Modifier.focusRequester(passwordFocusRequester),
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"

                    // Tombol ikon yang membalik state 'passwordVisible' saat diklik
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description, tint = Color.LightGray)
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { performLogin() }
                )
            )

            TextButton(
                onClick = { showForgotPasswordDialog = true }, // Tampilkan dialog saat diklik
                modifier = Modifier.align(Alignment.End) // Posisikan di kanan
            ) {
                Text("Lupa Password?", color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(16.dp)) // Beri sedikit jarak sebelum tombol Login

            Button(
                onClick = { performLogin() }, // Tombol "MASUK" juga memanggil fungsi login yang sama
                modifier = Modifier
                    .fillMaxWidth()
                    .height(41.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF23A1E),
                    contentColor = Color.White
                )
            ) {
                Text("MASUK", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
        if (showForgotPasswordDialog) {
            // Gunakan composable dialog yang sudah kita buat sebelumnya
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                onSendEmail = { email ->
                    // Panggil fungsi ViewModel
                    loginViewModel.sendPasswordResetEmail(email) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            showForgotPasswordDialog = false // Tutup dialog jika berhasil
                        }
                    }
                }
            )
        }
    }
}

// Fungsi komponen untuk CustomLoginTextField
@Composable
fun CustomLoginTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = LocalTextStyle.current.copy(color = Color.White),
        cursorBrush = SolidColor(Color.White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .height(41.dp)
                    .background(
                        color = Color(0xFF585858),
                        shape = RoundedCornerShape(59.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Row untuk menampung teks dan ikon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Box untuk menampung placeholder dan teks input
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Tampilkan placeholder jika value kosong
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = Color.LightGray)
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            }
        }
    )
}

@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendEmail: suspend (email: String) -> Unit // Menggunakan suspend karena memanggil ViewModel
) {
    var emailForReset by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) } // State loading
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("Masukkan alamat email akun Anda untuk menerima link reset password.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = emailForReset,
                    onValueChange = { emailForReset = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSending = true // Mulai loading
                    coroutineScope.launch {
                        onSendEmail(emailForReset) // Panggil fungsi callback
                        isSending = false // Selesai loading (dipanggil di callback onSendEmail)
                    }
                },
                enabled = !isSending && emailForReset.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(emailForReset).matches() // Validasi email sederhana
            ) {
                Text(if (isSending) "Mengirim..." else "Kirim Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text("Batal")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AplikasiKasirGeprekTheme {
        LoginScreen(onLoginSuccess = {_, _, _ -> })
    }
}