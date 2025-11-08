package com.kelompok2.aplikasi_kasir_geprek.ui.transaksi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StrukPembayaranScreen(
    viewModel: TransaksiViewModel,
    namaKasir: String,
    onKembali: () -> Unit,
    onCetak: () -> Unit,
    isSaving: Boolean
) {
    // Ambil Data
    val cartItems by viewModel.cartItems.collectAsState()
    val totalHarga = cartItems.values.sumOf { it.menu.harga * it.quantity }
    val formattedTotalHarga = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalHarga).replace("Rp", "")

    val totalItems = cartItems.values.sumOf { it.quantity }

    // Format Tanggal untuk Struk
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val tanggalSekarang = sdf.format(Date())

    // Latar belakang semi-transparan
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Card luar untuk menampung struk dan tombol
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f), // Ambil 90% tinggi layar
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), // Warna abu-abu
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // KONTEN STRUK YANG BISA DI-SCROLL
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(4.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // (Bagian Header Struk, Item, dan Total)
                    Text(
                        "AYAM GEPREK MR.KRIUK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        "Jl. Bringin Kab. Ponorogo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Text(
                        tanggalSekarang,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text(
                            text = "Kasir: $namaKasir",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Start
                        )
                    }
                    Text(
                        "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("QTY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
                        Text("ITEM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(4f))
                        Text("TOTAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    cartItems.values.forEach { item ->
                        StrukItemRow(item = item)
                    }
                    Text(
                        "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Item:",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "$totalItems",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = formattedTotalHarga,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "TERIMA KASIH",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }

                // Tombol Aksi
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    OutlinedButton(
                        onClick = onKembali,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        val contentColor = if (!isSaving) Color.Black else Color.Gray

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Kembali",
                            color = contentColor
                        )
                    }

                    val originalRed = Color(0xFFF54525)
                    val darkerRed = Color(0xFFC0392B)

                    // Tombol CETAK (Sesuai permintaan Anda, konten berwarna Hitam)
                    Button(
                        onClick = onCetak,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = originalRed,
                            contentColor = Color.Black,
                            disabledContainerColor = darkerRed,
                            disabledContentColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Print, contentDescription = "Cetak")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cetak")
                        }
                    }
                }
            }
        }
    }
}

// Composable StrukItemRow (Tidak berubah)
@Composable
private fun StrukItemRow(item: CartItem) {
    val formattedHargaSatuan = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.menu.harga).replace("Rp", "")
    val formattedSubtotal = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.menu.harga * item.quantity).replace("Rp", "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${item.quantity}x",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        Column(modifier = Modifier.weight(4f)) {
            Text(
                text = item.menu.nama_menu,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
            Text(
                text = "@$formattedHargaSatuan",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
        Text(
            text = formattedSubtotal,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End,
            color = Color.Black
        )
    }
}