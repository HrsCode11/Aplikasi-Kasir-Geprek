package com.kelompok2.aplikasi_kasir_geprek.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.R
import com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolauser.KelolaUserScreen
import kotlinx.coroutines.launch
import com.kelompok2.aplikasi_kasir_geprek.ui.admin.kelolamenu.KelolaMenuScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    role: String,
    username: String,
    onLogout: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val sidebarItems = if (role == "Admin") adminSidebarItems else kasirSidebarItems
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItem by remember { mutableStateOf(sidebarItems[0]) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Gambar Latar Belakang Utama
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Main Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                // Sidebar dengan Latar Belakang Putih
                ModalDrawerSheet(
                    drawerContainerColor = Color.White
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Sidebar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Daftar Item Menu Sidebar
                        sidebarItems.forEach { item ->
                            NavigationDrawerItem(
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.title) },
                                selected = item == selectedItem,
                                onClick = {
                                    selectedItem = item
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Black,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Pendorong ke Bawah
                        Spacer(modifier = Modifier.weight(1f))

                        // Item Logout
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout") },
                            label = { Text("Logout") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                mainViewModel.signOut()
                                onLogout()
                            },
                            modifier = Modifier.padding(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Black
                            )
                        )
                    }
                }
            }
        ) {
            // Konten Utama Aplikasi
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = selectedItem.title) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.Black,
                            navigationIconContentColor = Color.Black
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Konten Dinamis Berdasarkan Pilihan Sidebar
                    when (selectedItem.route) {
                        "transaksi" -> ContentPlaceholder(title = selectedItem.title)
                        "kelola_menu" -> {
                            KelolaMenuScreen()
                        }
                        "kelola_user" -> KelolaUserScreen()
                        "monitoring" -> ContentPlaceholder(title = selectedItem.title)
                        "riwayat" -> ContentPlaceholder(title = selectedItem.title)
                        else -> ContentPlaceholder(title = selectedItem.title)
                    }
                }
            }
        }
    }
}

// Composable sementara untuk halaman yang belum dibuat
@Composable
private fun ContentPlaceholder(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Halaman untuk: $title", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
    }
}
