package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolauser

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDialog(
    userToEdit: UserUiState?,
    onDismiss: () -> Unit,
    // Callback diubah untuk menerima fungsi suspend
    onSaveUser: suspend (userData: UserUiState, password: String) -> Pair<Boolean, String>,
    onUpdateUser: suspend (userData: UserUiState) -> Pair<Boolean, String>
) {
    // === STATE INPUT ===
    var username by remember { mutableStateOf(userToEdit?.username ?: "") }
    var email by remember { mutableStateOf(userToEdit?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(userToEdit?.role ?: "Kasir") }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    val roles = listOf("Admin", "Kasir")

    // === STATE VALIDASI & PROSES ===
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    // Langsung valid jika mode edit, karena data awal dianggap valid
    var isFormValid by remember { mutableStateOf(userToEdit != null) }
    var isSaving by remember { mutableStateOf(false) } // State loading
    var generalError by remember { mutableStateOf<String?>(null) } // Error dari Firebase
    var passwordVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- FUNGSI VALIDASI ---
    fun validate(isEditMode: Boolean): Boolean {
        usernameError = null
        emailError = null
        passwordError = null
        generalError = null // Reset general error
        var isValid = true

        if (username.isBlank()) {
            usernameError = "Username tidak boleh kosong"; isValid = false
        }
        if (!isEditMode) { // Hanya validasi email & pass saat tambah
            if (email.isBlank()) {
                emailError = "Email tidak boleh kosong"; isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Format email tidak valid"; isValid = false
            }
            if (password.isBlank()) {
                passwordError = "Password tidak boleh kosong"; isValid = false
            } else if (password.length < 6) {
                passwordError = "Password minimal 6 karakter"; isValid = false
            } else if (!password.any { it.isDigit() || !it.isLetterOrDigit() }) {
                passwordError = "Password harus mengandung angka atau simbol"
                isValid = false
            } else if (!password.any { it.isUpperCase() }) { // Cek apakah ada huruf besar
                passwordError = "Password harus mengandung huruf besar"
                isValid = false
            } else if (!password.any { it.isLowerCase() }) { // Cek apakah ada huruf kecil
                passwordError = "Password harus mengandung huruf kecil"
                isValid = false
            }
        }
        return isValid
    }

    // --- EFEK VALIDASI OTOMATIS ---
    LaunchedEffect(username, email, password) {
        // Validasi hanya jika mode tambah, atau jika mode edit tapi form sudah dianggap valid
        if (userToEdit == null || isFormValid) {
            isFormValid = validate(userToEdit != null)
        }
    }

    // === DIALOG UI ===
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (userToEdit == null) "Tambah User Baru" else "Edit User") },
        text = {
            Column {
                // --- Input Username ---
                OutlinedTextField(
                    value = username,
                    // Reset general error saat input berubah
                    onValueChange = { username = it; generalError = null },
                    label = { Text("Username") },
                    isError = usernameError != null || generalError != null,
                    supportingText = {
                        usernameError?.let { Text(it) }
                        // Tampilkan general error jika tidak terkait email/password
                            ?: generalError?.let { if (!it.contains("password", ignoreCase = true) && !it.contains("email", ignoreCase = true)) Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Input Email ---
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; generalError = null },
                    label = { Text("Email") },
                    enabled = userToEdit == null, // Nonaktif saat edit
                    // Tampilkan error jika ada error email lokal ATAU error dari Firebase mengandung kata 'email'
                    isError = emailError != null || (generalError != null && generalError!!.contains("email", ignoreCase = true)),
                    supportingText = {
                        emailError?.let { Text(it) }
                        // Tampilkan error email dari Firebase jika ada
                            ?: generalError?.let { if (it.contains("email", ignoreCase = true)) Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Input Password (hanya saat tambah) ---
                if (userToEdit == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; generalError = null },
                        label = { Text("Password") },
                        // Tentukan VisualTransformation berdasarkan state passwordVisible
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null || (generalError != null && generalError!!.contains("password", ignoreCase = true)),
                        supportingText = {
                            passwordError?.let { Text(it) }
                                ?: generalError?.let { if (it.contains("password", ignoreCase = true)) Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        // Tambahkan trailingIcon (ikon di akhir field)
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

                            // Buat ikon bisa diklik
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Sembunyikan password" else "Tampilkan password")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Dropdown Role ---
                ExposedDropdownMenuBox(
                    expanded = isRoleDropdownExpanded,
                    onExpandedChange = { isRoleDropdownExpanded = !isRoleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth() // Pastikan fillMaxWidth
                    )
                    ExposedDropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        roles.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    role = selectionOption
                                    isRoleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate(userToEdit != null)) {
                        isSaving = true // Mulai loading
                        val userData = UserUiState(
                            id = userToEdit?.id ?: "", username = username, email = email, role = role
                        )
                        coroutineScope.launch {
                            val (success, message) = if (userToEdit == null) {
                                onSaveUser(userData, password) // Panggil fungsi tambah
                            } else {
                                onUpdateUser(userData) // Panggil fungsi update
                            }
                            isSaving = false // Selesai loading
                            if (success) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                onDismiss() // Tutup dialog HANYA jika sukses
                            } else {
                                generalError = message // Simpan pesan error Firebase
                                validate(userToEdit != null) // Validasi ulang untuk tampilkan error
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                // Tombol aktif jika (form valid ATAU mode edit) DAN tidak sedang menyimpan
                enabled = !isSaving && (isFormValid || userToEdit != null)
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            // Tombol batal nonaktif saat proses menyimpan
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") }
        }
    )
}