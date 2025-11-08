package com.kelompok2.aplikasi_kasir_geprek.ui.transaksi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.R
import com.kelompok2.aplikasi_kasir_geprek.data.model.Kategori
import com.kelompok2.aplikasi_kasir_geprek.data.model.Menu
import com.kelompok2.aplikasi_kasir_geprek.utils.BluetoothPrinterService
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaksiScreen(
    userId: String,
    username: String,
    viewModel: TransaksiViewModel = viewModel()
) {
    // STATE MANAGEMENT
    val menuList by viewModel.menuList.collectAsState()
    val kategoriList by viewModel.kategoriList.collectAsState()
    val selectedKategoriId by viewModel.selectedKategoriId.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val filteredMenuList = if (selectedKategoriId == "all") {
        menuList
    } else {
        menuList.filter { it.id_kategori == selectedKategoriId }
    }

    val totalHarga = cartItems.values.sumOf { it.menu.harga * it.quantity }
    val formattedTotalHarga = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalHarga)
    val totalItems = cartItems.values.sumOf { it.quantity }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
    var showStruk by remember { mutableStateOf(false) }

    // PERSIAPAN BLUETOOTH & IZIN (REVISI)

    val bluetoothPrinterService = remember { BluetoothPrinterService(context) }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    fun arePermissionsGranted(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            showStruk = true
        } else {
            Toast.makeText(context, "Izin Bluetooth ditolak. Tidak dapat mencetak.", Toast.LENGTH_LONG).show()
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            bottomBar = {
                if (cartItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (arePermissionsGranted()) {
                                showStruk = true
                            } else {
                                permissionLauncher.launch(bluetoothPermissions.toTypedArray())
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF54525)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$totalItems Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(formattedTotalHarga, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lanjutkan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Lanjutkan")
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {

                CategoryFilter(
                    kategoriList = kategoriList,
                    selectedKategoriId = selectedKategoriId,
                    onKategoriSelected = { viewModel.selectKategori(it) }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMenuList, key = { it.id }) { menu ->
                        TransaksiMenuItemCard(
                            menu = menu,
                            quantityInCart = cartItems[menu.id]?.quantity ?: 0,
                            onAddToCart = { viewModel.addToCart(menu) },
                            onRemoveFromCart = { viewModel.removeFromCart(menu) }
                        )
                    }
                }
            }
        }

        if (showStruk) {
            StrukPembayaranScreen(
                viewModel = viewModel,
                namaKasir = username,
                onKembali = {
                    showStruk = false
                },
                onCetak = {
                    val currentCartItems = viewModel.cartItems.value
                    if (currentCartItems.isEmpty()) {
                        Toast.makeText(context, "Keranjang kosong.", Toast.LENGTH_SHORT).show()
                        return@StrukPembayaranScreen
                    }
                    val currentTotalHarga = currentCartItems.values.sumOf { it.menu.harga * it.quantity }

                    isSaving = true
                    coroutineScope.launch {
                        val (success, message) = viewModel.simpanTransaksi(userId, username)

                        if (success) {
                            try {
                                bluetoothPrinterService.printStruk(
                                    cartItems = currentCartItems,
                                    totalHarga = currentTotalHarga,
                                    namaKasir = username
                                )

                                Toast.makeText(context, "Transaksi disimpan & struk dicetak.", Toast.LENGTH_LONG).show()

                                viewModel.clearCart()
                                showStruk = false

                            } catch (e: Exception) {
                                Log.e("TransaksiScreen", "Gagal mencetak", e)
                                Toast.makeText(context, "Transaksi disimpan. GAGAL CETAK: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }

                        isSaving = false
                    }
                },
                isSaving = isSaving
            )
        }
    }
}

// Composable untuk Filter Kategori (Tidak berubah)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilter(
    kategoriList: List<Kategori>,
    selectedKategoriId: String?,
    onKategoriSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(kategoriList) { kategori ->
            FilterChip(
                selected = kategori.id == selectedKategoriId,
                onClick = { onKategoriSelected(kategori.id) },
                label = { Text(kategori.nama_kategori) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// Composable untuk Satu Item Menu (Tidak berubah)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransaksiMenuItemCard(
    menu: Menu,
    quantityInCart: Int,
    onAddToCart: () -> Unit,
    onRemoveFromCart: () -> Unit
) {
    val formattedPrice = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(menu.harga)

    val categoryIconId = when (menu.nama_kategori.lowercase()) {
        "ayam" -> R.drawable.logo_ayam
        "minuman" -> R.drawable.logo_minuman
        "mie" -> R.drawable.logo_mie
        else -> R.drawable.logo_ayam
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = categoryIconId),
                contentDescription = menu.nama_kategori,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menu.nama_menu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            AnimatedContent(
                targetState = quantityInCart == 0,
                label = "CartButtonAnimation"
            ) { isZero ->
                if (isZero) {
                    FilledTonalIconButton(
                        onClick = onAddToCart,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFF54525),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = "Tambah ke Keranjang"
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onRemoveFromCart, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                        }
                        Text(
                            text = "$quantityInCart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onAddToCart, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                        }
                    }
                }
            }
        }
    }
}