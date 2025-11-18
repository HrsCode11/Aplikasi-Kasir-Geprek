package com.kelompok2.aplikasi_kasir_geprek.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import com.kelompok2.aplikasi_kasir_geprek.data.model.TransaksiItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private fun formatHarga(harga: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(harga).replace("Rp", "Rp.")
}

private fun formatHargaTanpaMataUang(harga: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(harga).replace("Rp", "").replace(".", "").trim()
}

private fun formatTimestampToDateHeader(timestamp: Timestamp): String {
    val date = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))
}

private fun formatTimestampToStrukDate(timestamp: Timestamp): String {
    val date = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("id", "ID")))
}

private fun formatTimestampToStrukTime(timestamp: Timestamp): String {
    val time = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}

// --- RIWAYAT SCREEN UTAMA (DAFTAR) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RiwayatScreen(
    viewModel: RiwayatViewModel = viewModel(),
    onTransaksiClick: (String) -> Unit // Fungsi navigasi
) {
    val riwayatList by viewModel.riwayatList.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteOneDialog by remember { mutableStateOf<String?>(null) }

    val groupedRiwayat by remember(riwayatList) {
        derivedStateOf { riwayatList.groupBy { formatTimestampToDateHeader(it.tanggal) } }
    }

    if (showDeleteOneDialog != null) {
        val transaksiIdToDelete = showDeleteOneDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteOneDialog = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Yakin ingin menghapus riwayat ini? Data akan hilang permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val (_, message) = viewModel.hapusRiwayatById(transaksiIdToDelete)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            showDeleteOneDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteOneDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (groupedRiwayat.isEmpty()) {
            item {
                Text(
                    text = "Belum ada riwayat transaksi.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        groupedRiwayat.forEach { (tanggal, listTransaksi) ->
            item {
                Text(
                    text = tanggal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    color = Color.Black
                )
            }

            items(listTransaksi, key = { it.id }) { transaksi ->
                val dismissState = rememberDismissState(
                    confirmStateChange = { dismissValue ->
                        if (dismissValue == DismissValue.DismissedToEnd || dismissValue == DismissValue.DismissedToStart) {
                            showDeleteOneDialog = transaksi.id
                            true
                        } else false
                    }
                )

                LaunchedEffect(showDeleteOneDialog) {
                    if (showDeleteOneDialog == null) dismissState.reset()
                }

                SwipeToDismiss(
                    state = dismissState,
                    background = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                        }
                    },
                    dismissContent = {
                        RiwayatItemCard(
                            transaksi = transaksi,
                            onClick = { onTransaksiClick(transaksi.id) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RiwayatItemCard(transaksi: Transaksi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // <-- Item menjadi dapat di-tap
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFBDBDBD)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ShoppingCart, contentDescription = "Transaksi", tint = Color.White) }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaksi.nama_kasir.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(text = formatTimestampToStrukTime(transaksi.tanggal), fontSize = 14.sp, color = Color.Gray)
            }

            Text(
                text = formatHarga(transaksi.total_harga),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFF57C00)
            )
        }
    }
}

// --- DETAIL STRUK RIWAYAT (LAYAR PENUH) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailStrukRiwayatScreen(
    transaksiId: String,
    viewModel: RiwayatViewModel = viewModel(),
    onKembali: () -> Unit
) {
    LaunchedEffect(transaksiId) {
        viewModel.loadDetailTransaksi(transaksiId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearDetailTransaksi()
        }
    }

    val transaksi by viewModel.detailTransaksi.collectAsState()
    val isLoading by viewModel.isLoadingDetail.collectAsState()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    Scaffold(
        // TopBar sudah dihapus
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else if (transaksi != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    StrukDetailContent(transaksi = transaksi!!)

                    // TOMBOL AKSI di bawah struk card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 64.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onKembali,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.Gray)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali", color = Color.Black)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Fungsi Cetak belum diimplementasi", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cetak", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak", color = Color.White)
                        }
                    }
                }
            }
            else {
                Text(
                    text = "Gagal memuat detail transaksi atau transaksi tidak ditemukan.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp).align(Alignment.Center)
                )
            }
        }
    }
}

// --- FUNGSI STRUK DENGAN CARD SEMI-TRANSPARAN DAN LAYOUT RAAPI ---

@Composable
private fun StrukDetailContent(transaksi: Transaksi) {
    val formattedDateTime = "${formatTimestampToStrukDate(transaksi.tanggal)} ${formatTimestampToStrukTime(transaksi.tanggal)}"
    val formattedTotalHarga = formatHarga(transaksi.total_harga)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // JUDUL TOKO
            Text(
                "AYAM GEPREK MR.KRIUK",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            // ALAMAT
            Text(
                "Jl. Bringin Kab. Ponorogo",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // TANGGAL & WAKTU
            Text(
                formattedDateTime,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // KASIR
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    "Kasir: ${transaksi.nama_kasir.replaceFirstChar { it.titlecase(Locale.getDefault()) }}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Garis Pemisah (Strip-strip)
            Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            // HEADER ITEM
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QTY", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(40.dp))
                Text("ITEM", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
            }

            Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            // DAFTAR ITEM
            transaksi.items.forEach { item ->
                StrukDetailItemRow(item = item)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            // TOTAL ITEM
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Item:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    transaksi.items.sumOf { it.qty }.toString(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(80.dp)
                )
            }

            // TOTAL HARGA
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    formattedTotalHarga,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.width(80.dp)
                )
            }

            // UCAPAN TERIMA KASIH
            Text(
                "TERIMA KASIH",
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun StrukDetailItemRow(item: TransaksiItem) {
    val formattedHargaSatuan = formatHargaTanpaMataUang(item.harga)
    val formattedSubtotal = formatHarga(item.sub_total)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Kolom QTY (1x)
        Text(
            text = "${item.qty}x",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.width(40.dp)
        )

        // Kolom Item (@Harga Satuan)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = item.nama_menu,
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "@${formattedHargaSatuan}",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Kolom TOTAL
        Text(
            text = formattedSubtotal,
            fontSize = 14.sp,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
    }
}