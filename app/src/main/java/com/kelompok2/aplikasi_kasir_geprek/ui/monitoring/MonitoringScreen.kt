package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Helper format Rupiah
private fun formatRupiah(amount: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

// Helper format Persentase
private fun formatPercentage(value: Float?): String {
    if (value == null) return "0%"
    val rounded = (value * 10).roundToInt() / 10f
    return (if (rounded >= 0) "+" else "") + rounded.toString() + "%"
}

@Composable
fun MonitoringScreen(
    viewModel: MonitoringViewModel = viewModel()
) {
    // === STATE ===
    val stats by viewModel.stats.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val chartData by viewModel.chartData.collectAsState() // Data grafik 7 hari

    // Ambil state bulan terpilih
    val selectedExportDate by viewModel.selectedExportDate.collectAsState()
    val context = LocalContext.current

    // Helper untuk memformat nama bulan (misal: "November 2024")
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    val displayMonth = monthFormat.format(selectedExportDate.time)

    // === UI ===
    Column(modifier = Modifier.fillMaxSize()) {

        // --- HEADER DENGAN TOMBOL EKSPOR ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Penjualan",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Baris Kontrol: < Bulan >  [Tombol Ekspor]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- PEMILIH BULAN ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Tombol Bulan Sebelumnya
                        IconButton(onClick = {
                            val cal = selectedExportDate.clone() as Calendar
                            cal.add(Calendar.MONTH, -1) // Mundur 1 bulan
                            viewModel.setExportDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Lalu")
                        }

                        // Teks Bulan
                        Text(
                            text = displayMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 120.dp), // Lebar minimum agar tidak goyang
                            textAlign = TextAlign.Center
                        )

                        // Tombol Bulan Berikutnya
                        IconButton(onClick = {
                            val cal = selectedExportDate.clone() as Calendar
                            cal.add(Calendar.MONTH, 1) // Maju 1 bulan
                            viewModel.setExportDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Depan")
                        }
                    }

                    // --- TOMBOL EKSPOR ---
                    Button(
                        onClick = { viewModel.exportDataToExcel(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 14.sp)
                    }
                }
            }
        }

        // --- KONTEN UTAMA (SCROLLABLE) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // --- 1. Kartu Ringkasan Utama ---
            item {
                DashboardSummaryCard(
                    totalSales = formatRupiah(stats.totalPenjualanBulanIni),
                    growth = stats.salesGrowthMoM,
                    lastMonthSales = formatRupiah(stats.totalPenjualanBulanLalu)
                )
            }

            // --- 2. Grid 2x2 untuk KPI ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = "Rata-rata Transaksi",
                        value = formatRupiah(stats.avgTransactionValue),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Produk Terlaris",
                        value = stats.bestSellingProduct?.first ?: "-",
                        subValue = "(${stats.bestSellingProduct?.second ?: 0} Pcs)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = "Total Transaksi (Bln)",
                        value = "${stats.totalTransaksiBulanIni}",
                        subValue = "Transaksi",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Penjualan Hari Ini",
                        value = formatRupiah(stats.totalPenjualanHariIni),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 3. Kartu Grafik Tren (7 Hari Terakhir) ---
            item {
                Text(
                    "Tren Penjualan (7 Hari)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Grafik Batang Buatan Sendiri
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    if (chartData.isEmpty() && stats.totalPenjualanBulanIni == 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Panggil Composable grafik buatan kita
                        CustomBarChart(
                            data = chartData,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // --- 4. Daftar Transaksi Terbaru ---
            item {
                Text(
                    "Riwayat Transaksi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (recentTransactions.isEmpty()) {
                item { Text("Belum ada transaksi bulan ini.", color = Color.Gray) }
            } else {
                items(recentTransactions, key = { it.id }) { transaksi ->
                    TransactionItemCard(transaksi = transaksi)
                }
            }
        }
    }
}

// --- Composable GRAFIK BATANG DENGAN WARNA TREN ---
@Composable
private fun CustomBarChart(
    data: List<DailyChartEntry>,
    modifier: Modifier = Modifier
) {
    // Cari nilai maksimum untuk skala
    val maxSale = data.maxOfOrNull { it.yValue }?.coerceAtLeast(1f) ?: 1f
    val defaultColor = MaterialTheme.colorScheme.primary
    val upColor = Color(0xFF4CAF50) // Hijau
    val downColor = Color(0xFFE53935) // Merah

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Label Y-Axis (0, 50%, 100%)
    val yAxisLabels = listOf(maxSale.toInt(), (maxSale / 2).toInt(), 0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp)
    ) {
        // 1. KOLOM LABEL Y-AXIS (KIRI) - Tidak berubah
        Column(
            modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yAxisLabels.forEach { value ->
                Text(
                    text = "${(value / 1000)}k",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. AREA KANAN (GRAFIK + LABEL X-AXIS)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // 2a. AREA BATANG
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                val maxHeight = maxHeight

                // Garis Grid Horizontal - Tidak berubah
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) { Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp) }
                }

                // Baris Batang Grafik
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { index, entry ->
                        val heightFraction = (entry.yValue / maxSale).coerceIn(0f, 1f)

                        val animatedHeight by animateDpAsState(
                            targetValue = maxHeight * heightFraction,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "barHeight"
                        )

                        // --- LOGIKA WARNA BARU ---
                        val barColor = if (index == 0) {
                            defaultColor // Hari pertama: default
                        } else {
                            val prevValue = data[index - 1].yValue
                            if (entry.yValue > prevValue) upColor      // Naik: Hijau
                            else if (entry.yValue < prevValue) downColor // Turun: Merah
                            else defaultColor                          // Sama: Default
                        }
                        // -------------------------

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = if (selectedIndex == index) null else index },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Tooltip
                            if (selectedIndex == index) {
                                Box(
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatRupiah(entry.yValue.toInt()),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Batang Visual (Dengan Warna Dinamis)
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(if (animatedHeight < 1.dp) 1.dp else animatedHeight) // Min height 1dp agar tetap terlihat garisnya jika 0
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (selectedIndex == index) barColor.copy(alpha = 0.8f) else barColor)
                            )
                        }
                    }
                }
            }

            // 2b. LABEL X-AXIS (TANGGAL) - Tidak berubah
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEachIndexed { index, entry ->
                    Text(
                        text = entry.date.format(DateTimeFormatter.ofPattern("dd/MM")),
                        fontSize = 10.sp,
                        color = if (selectedIndex == index) Color.Black else Color.Gray,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


// --- Composable Kartu Ringkasan Utama ---
@Composable
fun DashboardSummaryCard(
    totalSales: String,
    growth: Float?,
    lastMonthSales: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF54525))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Penjualan Bulan Ini", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totalSales, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                if (growth != null) {
                    val (color, prefix) = if (growth >= 0) Pair(Color(0xFFB9F6CA), "+") else Pair(Color(0xFFFFCDD2), "")
                    Text(
                        "$prefix${formatPercentage(growth)} vs Bln Lalu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Bulan Lalu: $lastMonthSales", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// --- Composable Kartu KPI ---
@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color.Gray, fontSize = 13.sp)
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subValue != null) {
                    Text(text = subValue, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

// --- Composable Satu Baris Transaksi ---
@Composable
fun TransactionItemCard(transaksi: Transaksi) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(transaksi.tanggal.toDate())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Kasir: ${transaksi.nama_kasir}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = formattedDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(text = formatRupiah(transaksi.total_harga), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}