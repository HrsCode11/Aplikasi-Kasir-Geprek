package com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolauser

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun KelolaUserScreen(viewModel: KelolaUserViewModel = viewModel()) {
    val users by viewModel.users.collectAsState()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserUiState?>(null) }

    // State untuk mengontrol status expand/collapse FAB
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isFabExpanded) {
                        // Jika sudah expanded, jalankan aksi utama (buka dialog)
                        userToEdit = null
                        showDialog = true
                    } else {
                        // Jika belum, expand saja dulu
                        isFabExpanded = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah User") },
                text = { Text("Tambah User") },
                // Hubungkan state dengan properti expanded dari tombol
                expanded = isFabExpanded
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        if (users.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Memuat data user...", color = Color.Black)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    UserItemCard(
                        user = user,
                        onEditClick = {
                            userToEdit = user
                            showDialog = true
                            isFabExpanded = false // Collapse FAB saat edit
                        },
                        onDeactivateClick = {
                            viewModel.deactivateUser(user.id) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onActivateClick = {
                            viewModel.activateUser(user.id) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onResetPasswordClick = {
                            viewModel.sendPasswordResetEmail(user.email) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }

        if (showDialog) {
            UserDialog(
                userToEdit = userToEdit,
                onDismiss = {
                    showDialog = false
                    isFabExpanded = false // Collapse FAB saat dialog ditutup
                },
                onSave = { userData, password ->
                    if (userToEdit == null) {
                        viewModel.addUser(userData.email, password, userData.username, userData.role) { _, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        viewModel.updateUser(userData) { _, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    showDialog = false
                    isFabExpanded = false // Collapse FAB saat data disimpan
                }
            )
        }
    }
}

@Composable
fun UserItemCard(
    user: UserUiState,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit,
    onResetPasswordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text("Role: ${user.role}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${user.status}", style = MaterialTheme.typography.bodyMedium,
                color = if (user.status == "active") Color(0xFF4CAF50) else Color.Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onResetPasswordClick) { Text("Reset Pass") }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onEditClick) { Text("Edit") }
                Spacer(modifier = Modifier.width(4.dp))
                if (user.status == "active") {
                    TextButton(onClick = onDeactivateClick) {
                        Text("Nonaktifkan", color = Color.Red)
                    }
                } else {
                    TextButton(onClick = onActivateClick) {
                        Text("Aktifkan", color = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}