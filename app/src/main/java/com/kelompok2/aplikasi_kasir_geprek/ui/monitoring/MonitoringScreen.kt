package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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

// --- Helper Functions ---

/**
 * Memformat angka integer menjadi format mata uang Rupiah (contoh: Rp150.000).
 */
private fun formatRupiah(amount: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

/**
 * Memformat angka float menjadi persentase dengan tanda +/- (contoh: +15.5%).
 */
private fun formatPercentage(value: Float?): String {
    if (value == null) return "0%"
    val rounded = (value * 10).roundToInt() / 10f
    return (if (rounded >= 0) "+" else "") + rounded.toString() + "%"
}

@Composable
fun MonitoringScreen(
    viewModel: MonitoringViewModel = viewModel()
) {
    // --- State Collection ---
    val stats by viewModel.stats.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val selectedExportDate by viewModel.selectedExportDate.collectAsState()

    val context = LocalContext.current

    // Format tampilan bulan header (misal: "Desember 2025")
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    val displayMonth = monthFormat.format(selectedExportDate.time)

    // Logika Validasi: Cek apakah boleh maju ke bulan depan (Tidak boleh melebihi bulan saat ini)
    val canGoNext = remember(selectedExportDate) {
        val currentCal = Calendar.getInstance()
        // Reset waktu ke awal hari untuk perbandingan tanggal murni
        currentCal.set(Calendar.DAY_OF_MONTH, 1); currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0); currentCal.set(Calendar.SECOND, 0); currentCal.set(Calendar.MILLISECOND, 0)

        val selectedCal = selectedExportDate.clone() as Calendar
        selectedCal.set(Calendar.DAY_OF_MONTH, 1); selectedCal.set(Calendar.HOUR_OF_DAY, 0)
        selectedCal.set(Calendar.MINUTE, 0); selectedCal.set(Calendar.SECOND, 0); selectedCal.set(Calendar.MILLISECOND, 0)

        selectedCal.before(currentCal) // True jika bulan terpilih < bulan sekarang
    }

    // === UI Layout ===
    Column(modifier = Modifier.fillMaxSize()) {

        // 1. Header Kontrol Bulan & Ekspor
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigasi Bulan (< Bulan >)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val cal = selectedExportDate.clone() as Calendar
                            cal.add(Calendar.MONTH, -1) // Mundur 1 bulan
                            viewModel.setExportDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Lalu")
                        }

                        Text(
                            text = displayMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 130.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                val cal = selectedExportDate.clone() as Calendar
                                cal.add(Calendar.MONTH, 1) // Maju 1 bulan
                                viewModel.setExportDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                            },
                            enabled = canGoNext // Matikan tombol jika sudah di bulan ini
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Bulan Depan",
                                tint = if (canGoNext) Color.Black else Color.LightGray
                            )
                        }
                    }

                    // Tombol Ekspor Excel
                    Button(
                        onClick = { viewModel.exportDataToExcel(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Hijau Excel
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 14.sp)
                    }
                }
            }
        }

        // 2. Konten Scrollable (KPI, Grafik, List)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // A. Kartu Ringkasan (Merah)
            item {
                DashboardSummaryCard(
                    totalSales = formatRupiah(stats.totalPenjualanBulanIni),
                    growth = stats.salesGrowthMoM,
                    lastMonthSales = formatRupiah(stats.totalPenjualanBulanLalu)
                )
            }

            // B. Grid KPI
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Rata-rata Transaksi", formatRupiah(stats.avgTransactionValue), modifier = Modifier.weight(1f))
                    StatCard("Produk Terlaris", stats.bestSellingProduct?.first ?: "-", "(${stats.bestSellingProduct?.second ?: 0} Pcs)", modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Total Transaksi", "${stats.totalTransaksiBulanIni}", "Transaksi", modifier = Modifier.weight(1f))
                    // Placeholder agar layout grid tetap rapi
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // C. Grafik Harian
            item {
                Text(
                    "Grafik Harian ($displayMonth)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (chartData.isEmpty()) {
                        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (stats.totalPenjualanBulanIni == 0) Text("Data Kosong", color = Color.Gray)
                            else CircularProgressIndicator()
                        }
                    } else {
                        // Tampilkan grafik kustom
                        CustomBarChart(data = chartData)
                    }
                }
            }

            // D. List Transaksi
            item {
                Text(
                    "Riwayat Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (recentTransactions.isEmpty()) {
                item { Text("Tidak ada transaksi pada bulan ini.", color = Color.Gray) }
            } else {
                items(recentTransactions, key = { it.id }) { transaksi ->
                    TransactionItemCard(transaksi = transaksi)
                }
            }
        }
    }
}

// --- Custom Components ---

@Composable
private fun CustomBarChart(
    data: List<DailyChartEntry>,
    modifier: Modifier = Modifier
) {
    val maxSale = data.maxOfOrNull { it.yValue }?.coerceAtLeast(1f) ?: 1f

    // Warna Tren
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
        // 1. Kolom Label Y-Axis (Kiri)
        Column(
            modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yAxisLabels.forEach { value ->
                Text(
                    text = "${(value / 1000)}k", // Format ribuan
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Area Grafik Utama
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val maxHeight = maxHeight

                // === FIX HEIGHT: Sisakan ruang 40dp di atas untuk Tooltip agar tidak terpotong ===
                val availableBarHeight = maxHeight - 40.dp

                // Garis Grid Horizontal
                Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) { Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp) }
                }

                // Baris Batang Grafik
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { index, entry ->
                        // Hitung tinggi relatif
                        val heightFraction = (entry.yValue / maxSale).coerceIn(0f, 1f)

                        // Animasi
                        val animatedHeight by animateDpAsState(
                            targetValue = availableBarHeight * heightFraction,
                            animationSpec = tween(800, easing = FastOutSlowInEasing),
                            label = "bar"
                        )

                        // Logika Warna Tren
                        val barColor = if (index == 0) defaultColor else {
                            val prevValue = data[index - 1].yValue
                            if (entry.yValue > prevValue) upColor
                            else if (entry.yValue < prevValue) downColor
                            else defaultColor
                        }

                        // Kolom Batang Individual
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = if (selectedIndex == index) null else index },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // === TOOLTIP ===
                            if (selectedIndex == index) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp) // Jarak dari batang
                                        .wrapContentSize(unbounded = true) // <--- FIX UTAMA: Agar teks tidak gepeng ke bawah
                                        .zIndex(10f) // Pastikan di atas layer lain
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = formatRupiah(entry.yValue.toInt()),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // === BATANG ===
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(if (animatedHeight < 2.dp) 2.dp else animatedHeight) // Minimal 2dp agar terlihat
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(if (selectedIndex == index) barColor.copy(alpha = 0.8f) else barColor)
                            )
                        }
                    }
                }
            }

            // X-Axis (Tanggal) - Tampilkan per 5 hari jika data penuh
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                data.forEachIndexed { index, entry ->
                    if (data.size <= 10 || index % 5 == 0 || index == data.lastIndex) {
                        Text(
                            text = entry.date.format(DateTimeFormatter.ofPattern("dd")),
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Spacer invisible agar alignment bar tetap pas
                        Text(text = "", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// --- Dashboard Summary Card (Merah) ---
@Composable
fun DashboardSummaryCard(totalSales: String, growth: Float?, lastMonthSales: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF54525))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Penjualan", style = MaterialTheme.typography.titleMedium, color = Color.White)
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

// --- Stat Card (KPI) ---
@Composable
fun StatCard(title: String, value: String, subValue: String? = null, modifier: Modifier = Modifier) {
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
                if (subValue != null) Text(text = subValue, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

// --- Transaction Item Card ---
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
            Text(text = formatRupiah(transaksi.total_harga), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF54525))
        }
    }
}