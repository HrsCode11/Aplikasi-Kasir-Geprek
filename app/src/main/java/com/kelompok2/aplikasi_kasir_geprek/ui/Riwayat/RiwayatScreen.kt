package com.kelompok2.aplikasi_kasir_geprek.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.kelompok2.aplikasi_kasir_geprek.ui.transaksi.Transaksi
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// Fungsi Helper Format

private fun formatHarga(harga: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(harga).replace("Rp", "Rp.")
}

private fun formatTimestampToDateHeader(timestamp: Timestamp): String {
    val date = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))
}

private fun formatTimestampToTime(timestamp: Timestamp): String {
    val time = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

// Composable Utama

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RiwayatScreen(
    viewModel: RiwayatViewModel = viewModel()
) {
    val riwayatList by viewModel.riwayatList.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteOneDialog by remember { mutableStateOf<String?>(null) }

    val groupedRiwayat by remember(riwayatList) {
        derivedStateOf { riwayatList.groupBy { formatTimestampToDateHeader(it.tanggal) } }
    }

    // Dialog Konfirmasi Hapus PER ITEM
    if (showDeleteOneDialog != null) {
        val transaksiIdToDelete = showDeleteOneDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteOneDialog = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Yakin ingin menghapus riwayat ini?") },
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

    // UI Utama

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)),
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
            item { RiwayatDateHeader(tanggal = tanggal) }

            items(listTransaksi, key = { it.id }) { transaksi ->

                val dismissState = rememberDismissState(
                    confirmStateChange = { dismissValue ->
                        if (dismissValue == DismissValue.DismissedToEnd ||
                            dismissValue == DismissValue.DismissedToStart
                        ) {
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
                                .background(
                                    Color.Red.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = Color.White
                            )
                        }
                    },
                    dismissContent = { RiwayatItemCard(transaksi = transaksi) }
                )
            }
        }
    }
}

// Header Tanggal
@Composable
private fun RiwayatDateHeader(tanggal: String) {
    Text(
        text = tanggal,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        color = Color.Black
    )
}

// Item Card
@Composable
private fun RiwayatItemCard(transaksi: Transaksi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFBDBDBD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Transaksi",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nama kasir & waktu
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaksi.nama_kasir.replaceFirstChar {
                        it.titlecase(Locale.getDefault())
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formatTimestampToTime(transaksi.tanggal),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Total harga
            Text(
                text = formatHarga(transaksi.total_harga),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFF57C00)
            )
        }
    }
}
