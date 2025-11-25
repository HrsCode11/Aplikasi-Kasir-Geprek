// Lokasi: app/src/main/java/com/kelompok2/aplikasi_kasir_geprek/ui/monitoring/MonitoringViewModel.kt

package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.ui.transaksi.Transaksi // Pastikan import Transaksi benar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate // Import untuk data grafik
import java.time.ZoneId // Import untuk data grafik
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

// Enum untuk melacak pilihan waktu grafik
enum class ChartTimeRange {
    WEEK, MONTH
}

// Data class untuk menampung semua statistik KPI
data class DashboardStats(
    val totalPenjualanBulanIni: Int = 0,
    val totalPenjualanBulanLalu: Int = 0,
    val salesGrowthMoM: Float? = null, // Pertumbuhan MoM (Month-over-Month)
    val totalPenjualanHariIni: Int = 0,
    val avgTransactionValue: Int = 0, // Rata-rata Nilai Transaksi
    val bestSellingProduct: Pair<String, Int>? = null, // Nama Produk, Qty
    val totalTransaksiBulanIni: Int = 0
)

// Data class untuk entri di grafik (dibuat sederhana)
data class DailyChartEntry(
    val date: LocalDate,
    val yValue: Float // Total penjualan
)

class MonitoringViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    // State untuk semua statistik KPI
    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    // State untuk daftar transaksi terbaru (dibatasi 20)
    private val _recentTransactions = MutableStateFlow<List<Transaksi>>(emptyList())
    val recentTransactions = _recentTransactions.asStateFlow()

    // State untuk melacak pilihan (7 hari / 30 hari)
    private val _selectedTimeRange = MutableStateFlow(ChartTimeRange.WEEK)
    val selectedTimeRange = _selectedTimeRange.asStateFlow()

    // State untuk data grafik 7 hari
    private val _chartDataWeek = MutableStateFlow<List<DailyChartEntry>>(emptyList())
    val chartDataWeek = _chartDataWeek.asStateFlow()

    // State untuk data grafik 30 hari
    private val _chartDataMonth = MutableStateFlow<List<DailyChartEntry>>(emptyList())
    val chartDataMonth = _chartDataMonth.asStateFlow()

    init {
        loadDashboardData()
    }

    // Fungsi ini dipanggil dari UI untuk mengubah rentang grafik
    fun setChartTimeRange(range: ChartTimeRange) {
        _selectedTimeRange.value = range
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // --- Tentukan Rentang Waktu ---
            val cal = Calendar.getInstance()

            // Waktu 0: Awal 30 hari lalu (untuk data grafik 30 hari)
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val startOfLast30Days = Timestamp(cal.time)

            // Waktu 1: Awal hari ini (00:00:00)
            val todayCal = Calendar.getInstance()
            todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0)
            val startOfToday = Timestamp(todayCal.time)

            // Waktu 2: Awal bulan ini (Tanggal 1, 00:00:00)
            val monthCal = Calendar.getInstance()
            monthCal.set(Calendar.DAY_OF_MONTH, 1); monthCal.set(Calendar.HOUR_OF_DAY, 0); monthCal.set(Calendar.MINUTE, 0); monthCal.set(Calendar.SECOND, 0)
            val startOfMonth = Timestamp(monthCal.time)

            // Waktu 3: Awal bulan lalu (Tanggal 1 bulan lalu, 00:00:00)
            cal.time = monthCal.time // Mulai dari awal bulan ini
            cal.add(Calendar.MONTH, -1)
            val startOfLastMonth = Timestamp(cal.time)

            // --- Ambil Data Transaksi (dari awal bulan lalu untuk perbandingan MoM) ---
            firestore.collection("transaksi")
                .whereGreaterThanOrEqualTo("tanggal", startOfLastMonth)
                .orderBy("tanggal", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("MonitoringVM", "Listen failed.", error); return@addSnapshotListener
                    }

                    val allTransactions = snapshots?.toObjects(Transaksi::class.java) ?: emptyList()

                    // Pisahkan data bulan ini dan bulan lalu
                    val thisMonthTransactions = allTransactions.filter { it.tanggal.toDate().time >= startOfMonth.toDate().time }
                    val lastMonthTransactions = allTransactions.filter { it.tanggal.toDate().time < startOfMonth.toDate().time && it.tanggal.toDate().time >= startOfLastMonth.toDate().time}

                    // --- Hitung Semua Metrik KPI ---

                    val totalBulanIni = thisMonthTransactions.sumOf { it.total_harga }
                    val totalBulanLalu = lastMonthTransactions.sumOf { it.total_harga }
                    val jumlahBulanIni = thisMonthTransactions.size
                    val transactionsToday = thisMonthTransactions.filter { it.tanggal.toDate().time >= startOfToday.toDate().time }
                    val totalHariIni = transactionsToday.sumOf { it.total_harga }

                    // Pertumbuhan MoM (%)
                    val growth = if (totalBulanLalu > 0) {
                        ((totalBulanIni.toFloat() - totalBulanLalu.toFloat()) / totalBulanLalu.toFloat()) * 100
                    } else if (totalBulanIni > 0) {
                        100.0f // Tumbuh 100% jika bulan lalu 0
                    } else {
                        0.0f // 0 jika keduanya 0
                    }

                    // Rata-rata Nilai Transaksi (ATV) Bulan Ini
                    val atv = if (jumlahBulanIni > 0) totalBulanIni / jumlahBulanIni else 0

                    // Produk Terlaris (Bulan Ini)
                    val productQuantityMap = mutableMapOf<String, Pair<String, Int>>() // Map<IDMenu, Pair<Nama, Qty>>
                    thisMonthTransactions.forEach { trx ->
                        trx.items.forEach { item ->
                            val current = productQuantityMap[item.id_menu]
                            val newQty = (current?.second ?: 0) + item.qty
                            productQuantityMap[item.id_menu] = Pair(item.nama_menu, newQty)
                        }
                    }
                    val bestSeller = productQuantityMap.maxByOrNull { it.value.second }

                    // Update State Statistik Utama
                    _stats.value = DashboardStats(
                        totalPenjualanBulanIni = totalBulanIni,
                        totalPenjualanBulanLalu = totalBulanLalu,
                        salesGrowthMoM = growth,
                        totalPenjualanHariIni = totalHariIni,
                        avgTransactionValue = atv,
                        bestSellingProduct = bestSeller?.value,
                        totalTransaksiBulanIni = jumlahBulanIni
                    )

                    // --- 4. Kalkulasi Data Grafik ---
                    val today = LocalDate.now(ZoneId.systemDefault())
                    // Ambil data transaksi 30 hari terakhir
                    val last30DaysTransactions = allTransactions.filter { it.tanggal.toDate().time >= startOfLast30Days.toDate().time }

                    // Hitung data untuk 7 hari terakhir
                    _chartDataWeek.value = (0..6).map { daysAgo ->
                        val date = today.minusDays(daysAgo.toLong())
                        val totalSalesOnDate = last30DaysTransactions
                            .filter { it.tanggal.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date }
                            .sumOf { it.total_harga }
                        DailyChartEntry(date = date, yValue = totalSalesOnDate.toFloat())
                    }.reversed() // Balik urutan agar dari terlama ke terbaru

                    // Hitung data untuk 30 hari terakhir
                    _chartDataMonth.value = (0..29).map { daysAgo ->
                        val date = today.minusDays(daysAgo.toLong())
                        val totalSalesOnDate = last30DaysTransactions
                            .filter { it.tanggal.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date }
                            .sumOf { it.total_harga }
                        DailyChartEntry(date = date, yValue = totalSalesOnDate.toFloat())
                    }.reversed() // Balik urutan

                    // --- 5. Update Daftar Transaksi Terbaru (dibatasi 20) ---
                    _recentTransactions.value = thisMonthTransactions.take(20)
                }
        }
    }
}