// Lokasi: MainActivity.kt
package com.kelompok2.aplikasi_kasir_geprek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kelompok2.aplikasi_kasir_geprek.ui.login.LoginScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.main.MainScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.splash.SplashScreen // Import SplashScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.theme.AplikasiKasirGeprekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AplikasiKasirGeprekTheme {
                // NavController: Mengelola state navigasi
                val navController = rememberNavController()

                // NavHost: Container untuk semua halaman/tujuan navigasi
                NavHost(
                    navController = navController,
                    // Titik Awal Aplikasi: Layar splash untuk cek login
                    startDestination = "splash_screen"
                ) {
                    // --- Definisi Rute 1: SplashScreen ---
                    composable(route = "splash_screen") {
                        SplashScreen(
                            // Callback jika user belum login
                            onNavigateToLogin = {
                                navController.navigate("login_screen") {
                                    // Hapus splash screen dari riwayat
                                    popUpTo("splash_screen") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            // Callback jika user sudah login
                            onNavigateToMain = { role, username ->
                                navController.navigate("main_screen/$role/$username") {
                                    // Hapus splash screen dari riwayat
                                    popUpTo("splash_screen") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // --- Definisi Rute 2: LoginScreen ---
                    composable(route = "login_screen") {
                        LoginScreen(
                            // Callback setelah login berhasil
                            onLoginSuccess = { role, username ->
                                navController.navigate("main_screen/$role/$username") {
                                    // Hapus login screen dari riwayat
                                    popUpTo("login_screen") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // --- Definisi Rute 3: MainScreen ---
                    composable(
                        // Rute dinamis yang menerima role dan username
                        route = "main_screen/{role}/{username}",
                        arguments = listOf(
                            navArgument("role") { type = NavType.StringType },
                            navArgument("username") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        // Ambil argumen dari rute
                        val role = backStackEntry.arguments?.getString("role") ?: "Kasir"
                        val username = backStackEntry.arguments?.getString("username") ?: "Pengguna"

                        // Tampilkan MainScreen dan teruskan callback onLogout
                        MainScreen(
                            role = role,
                            username = username,
                            // Callback saat tombol Logout di sidebar diklik
                            onLogout = {
                                navController.navigate("login_screen") {
                                    // Hapus semua riwayat hingga titik awal (splash)
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true // Hapus juga titik awalnya
                                    }
                                    // Pastikan hanya ada satu LoginScreen
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}