// Lokasi: MainActivity.kt
package com.kelompok2.aplikasi_kasir_geprek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kelompok2.aplikasi_kasir_geprek.ui.login.LoginScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.main.MainScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.splash.SplashScreen // Import SplashScreen
import com.kelompok2.aplikasi_kasir_geprek.ui.theme.AplikasiKasirGeprekTheme
import androidx.navigation.NavGraph.Companion.findStartDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AplikasiKasirGeprekTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    // 1. Jadikan "splash_screen" sebagai titik awal
                    startDestination = "splash_screen"
                ) {
                    // 2. Buat rute baru untuk SplashScreen
                    composable(route = "splash_screen") {
                        SplashScreen(
                            onNavigateToLogin = {
                                navController.navigate("login_screen") {
                                    // Hapus splash screen dari riwayat
                                    popUpTo("splash_screen") { inclusive = true }
                                }
                            },
                            onNavigateToMain = { role, username ->
                                navController.navigate("main_screen/$role/$username") {
                                    // Hapus splash screen dari riwayat
                                    popUpTo("splash_screen") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(route = "login_screen") {
                        LoginScreen(
                            onLoginSuccess = { role, username ->
                                navController.navigate("main_screen/$role/$username") {
                                    popUpTo("login_screen") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = "main_screen/{role}/{username}",
                        arguments = listOf(
                            navArgument("role") { type = NavType.StringType },
                            navArgument("username") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Kasir"
                        val username = backStackEntry.arguments?.getString("username") ?: "Pengguna"
                        MainScreen(
                            role = role,
                            username = username,
                            onLogout = {
                                navController.navigate("login_screen") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}