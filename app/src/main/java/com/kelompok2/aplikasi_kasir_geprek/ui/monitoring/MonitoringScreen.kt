package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok2.aplikasi_kasir_geprek.ui.transaksi.Transaksi
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    viewModel: MonitoringViewModel = viewModel()
) {
    // === STATE ===
    val stats by viewModel.stats.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val chartDataWeek by viewModel.chartDataWeek.collectAsState()
    val chartDataMonth by viewModel.chartDataMonth.collectAsState()

    // === UI ===
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
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

        // --- 3. Kartu Grafik Tren (Ditingkatkan) ---
        item {
            Text(
                "Tren Penjualan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Tombol Toggle 7 Hari / 30 Hari
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                SegmentedButton(
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                    selected = selectedRange == ChartTimeRange.WEEK,
                    onClick = { viewModel.setChartTimeRange(ChartTimeRange.WEEK) },
                    label = { Text("7 Hari") }
                )
                SegmentedButton(
                    shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                    selected = selectedRange == ChartTimeRange.MONTH,
                    onClick = { viewModel.setChartTimeRange(ChartTimeRange.MONTH) },
                    label = { Text("30 Hari") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Grafik Batang Ditingkatkan ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp) // Naikkan sedikit tinggi untuk y-axis + labels
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Crossfade(targetState = selectedRange, label = "ChartCrossfade") { range ->
                        val data = if (range == ChartTimeRange.WEEK) chartDataWeek else chartDataMonth
                        if (data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            // Panggil Composable grafik yang lebih baik
                            EnhancedBarChart(
                                data = data,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                    // Ringkasan kecil di bawah grafik
                    Divider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val total = if (selectedRange == ChartTimeRange.WEEK) chartDataWeek.sumOf { it.yValue.toInt() } else chartDataMonth.sumOf { it.yValue.toInt() }
                        Text("Total: ${formatRupiah(total)}",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Unit: IDR", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        // --- 4. Daftar Transaksi Terbaru ---
        item {
            Text(
                "Riwayat Transaksi Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        if (recentTransactions.isEmpty() && stats.totalPenjualanBulanIni == 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Memuat data...", color = Color.Gray)
                    }
                }
            }
        } else if (recentTransactions.isEmpty()) {
            item { Text("Belum ada transaksi bulan ini.", color = Color.Gray) }
        } else {
            items(recentTransactions, key = { it.id }) { transaksi ->
                TransactionItemCard(transaksi = transaksi)
            }
        }
    }
}

// --- EnhancedBarChart (perbaikan: tidak ada panggilan @Composable di dalam Canvas) ---
@Composable
private fun EnhancedBarChart(
    data: List<DailyChartEntry>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    // pastikan semua yValue sebagai Float (jika model Anda Int, gunakan .toFloat() waktu mengisi data)
    val maxValue = data.maxOf { it.yValue }.coerceAtLeast(1f)
    val tickCount = 3
    val tickStep = ceil(maxValue / tickCount).toFloat()
    val yMaxAdjusted = (tickStep * tickCount).coerceAtLeast(maxValue)

    // Ambil warna di luar Canvas (scope Composable)
    val primary = MaterialTheme.colorScheme.primary
    val gradientColors = listOf(primary.copy(alpha = 0.95f), primary.copy(alpha = 0.6f))
    val highlightColor = primary.copy(alpha = 0.25f)
    val gridLineColor = Color(0xFFE6E6E6)

    // Selected bar index (untuk menampilkan tooltip/label)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Hitung animated fractions di scope Composable (tidak boleh dipanggil di dalam Canvas)
    val animatedFractions: List<Float> = data.map { entry ->
        val normalized = (entry.yValue / yMaxAdjusted).coerceIn(0f, 1f)
        animateFloatAsState(
            targetValue = normalized,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        ).value
    }

    // density (boleh dipanggil dari Composable scope)
    val density = LocalDensity.current

    Row(modifier = modifier.fillMaxHeight()) {
        // Left: y-axis labels
        Column(
            modifier = Modifier.width(56.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatRupiah(yMaxAdjusted.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = formatRupiah((yMaxAdjusted / 2).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 28.dp.toPx()

                val usableWidth = canvasWidth - leftPadding - rightPadding
                val usableHeight = canvasHeight - topPadding - bottomPadding

                val barCount = data.size
                val spacing = max(6.dp.toPx(), usableWidth * 0.04f)
                val totalSpacing = spacing * (barCount + 1)
                val barWidth = ((usableWidth - totalSpacing) / barCount).coerceAtLeast(6.dp.toPx())

                // draw horizontal grid lines
                for (i in 0..tickCount) {
                    val y = topPadding + usableHeight * (i.toFloat() / tickCount)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // draw bars using animatedFractions
                data.forEachIndexed { index, _ ->
                    val animatedFraction = animatedFractions.getOrNull(index) ?: 0f

                    val xStart = leftPadding + spacing + index * (barWidth + spacing)
                    val barHeight = animatedFraction * usableHeight
                    val top = topPadding + (usableHeight - barHeight)
                    val rect = Rect(xStart, top, xStart + barWidth, top + barHeight)

                    val gradient = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = rect.top,
                        endY = rect.bottom
                    )

                    val cornerRadius = 6.dp.toPx()
                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )

                    if (selectedIndex == index) {
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(rect.left - 4.dp.toPx(), rect.top - 4.dp.toPx()),
                            size = Size(rect.width + 8.dp.toPx(), rect.height + 8.dp.toPx()),
                            cornerRadius = CornerRadius(cornerRadius + 4.dp.toPx(), cornerRadius + 4.dp.toPx())
                        )
                    }
                }
            }

            // Overlay interactive clickable bars + x-axis labels + optional tooltip
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val barCount = data.size
                val weightPer = 1f / barCount
                data.forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier
                            .weight(weightPer)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedIndex == index) {
                            Text(
                                text = formatRupiah(entry.yValue.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // invisible clickable area
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = entry.date.format(DateTimeFormatter.ofPattern("dd/MM")),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Kartu & Item lainnya (tetap, dengan sedikit penyesuaian tampilan) ---
@Composable
fun DashboardSummaryCard(
    totalSales: String,
    growth: Float?,
    lastMonthSales: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Penjualan Bulan Ini", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totalSales, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
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
            Spacer(modifier = Modifier.height(6.dp))
            Text("Bulan Lalu: $lastMonthSales", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

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
