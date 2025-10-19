package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolauser

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDialog(
    userToEdit: UserUiState?,
    onDismiss: () -> Unit,
    onSave: (UserUiState, String) -> Unit
) {
    var username by remember { mutableStateOf(userToEdit?.username ?: "") }
    var email by remember { mutableStateOf(userToEdit?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(userToEdit?.role ?: "Kasir") }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    val roles = listOf("Admin", "Kasir")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (userToEdit == null) "Tambah User Baru" else "Edit User") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    enabled = userToEdit == null // Email tidak bisa diubah saat mode edit
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (userToEdit == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                        modifier = Modifier.menuAnchor()
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
                    val userData = UserUiState(
                        id = userToEdit?.id ?: "",
                        username = username,
                        email = email,
                        role = role
                    )
                    onSave(userData, password)
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}