package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import android.content.Context
import com.kelompok2.aplikasi_kasir_geprek.ui.utils.ExcelHelper

// Data class untuk statistik
data class DashboardStats(
    val totalPenjualanBulanIni: Int = 0,
    val totalPenjualanBulanLalu: Int = 0,
    val salesGrowthMoM: Float? = null,
    val totalPenjualanHariIni: Int = 0,
    val avgTransactionValue: Int = 0,
    val bestSellingProduct: Pair<String, Int>? = null,
    val totalTransaksiBulanIni: Int = 0
)

// Data class untuk grafik (Sumbu X: Tanggal, Sumbu Y: Total Penjualan)
data class DailyChartEntry(
    val date: LocalDate,
    val yValue: Float
)

class MonitoringViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaksi>>(emptyList())
    val recentTransactions = _recentTransactions.asStateFlow()

    // Hanya satu sumber data grafik: 7 Hari Terakhir
    private val _chartData = MutableStateFlow<List<DailyChartEntry>>(emptyList())
    val chartData = _chartData.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()

            // Waktu 0: Awal 7 hari lalu (untuk grafik)
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val startOfLast7Days = Timestamp(cal.time)

            val todayCal = Calendar.getInstance()
            todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0)
            val startOfToday = Timestamp(todayCal.time)

            val monthCal = Calendar.getInstance()
            monthCal.set(Calendar.DAY_OF_MONTH, 1); monthCal.set(Calendar.HOUR_OF_DAY, 0); monthCal.set(Calendar.MINUTE, 0); monthCal.set(Calendar.SECOND, 0)
            val startOfMonth = Timestamp(monthCal.time)

            cal.time = monthCal.time
            cal.add(Calendar.MONTH, -1)
            val startOfLastMonth = Timestamp(cal.time)

            // Ambil data dari awal bulan lalu (untuk MoM) ATAU 7 hari lalu (mana yang lebih lama)
            // Agar aman, kita ambil dari startOfLastMonth karena itu pasti lebih lama dari 7 hari lalu
            firestore.collection("transaksi")
                .whereGreaterThanOrEqualTo("tanggal", startOfLastMonth)
                .orderBy("tanggal", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("MonitoringVM", "Listen failed.", error); return@addSnapshotListener
                    }

                    val allTransactions = snapshots?.toObjects(Transaksi::class.java) ?: emptyList()

                    val thisMonthTransactions = allTransactions.filter { it.tanggal.toDate().time >= startOfMonth.toDate().time }
                    val lastMonthTransactions = allTransactions.filter { it.tanggal.toDate().time < startOfMonth.toDate().time && it.tanggal.toDate().time >= startOfLastMonth.toDate().time}

                    // --- Hitung KPI ---
                    val totalBulanIni = thisMonthTransactions.sumOf { it.total_harga }
                    val totalBulanLalu = lastMonthTransactions.sumOf { it.total_harga }
                    val growth = if (totalBulanLalu > 0) {
                        ((totalBulanIni.toFloat() - totalBulanLalu.toFloat()) / totalBulanLalu.toFloat()) * 100
                    } else if (totalBulanIni > 0) {
                        100.0f
                    } else {
                        0.0f
                    }

                    val jumlahBulanIni = thisMonthTransactions.size
                    val atv = if (jumlahBulanIni > 0) totalBulanIni / jumlahBulanIni else 0

                    val productQuantityMap = mutableMapOf<String, Pair<String, Int>>()
                    thisMonthTransactions.forEach { trx ->
                        trx.items.forEach { item ->
                            val current = productQuantityMap[item.id_menu]
                            val newQty = (current?.second ?: 0) + item.qty
                            productQuantityMap[item.id_menu] = Pair(item.nama_menu, newQty)
                        }
                    }
                    val bestSeller = productQuantityMap.maxByOrNull { it.value.second }

                    val transactionsToday = thisMonthTransactions.filter { it.tanggal.toDate().time >= startOfToday.toDate().time }
                    val totalHariIni = transactionsToday.sumOf { it.total_harga }

                    _stats.value = DashboardStats(
                        totalPenjualanBulanIni = totalBulanIni,
                        totalPenjualanBulanLalu = totalBulanLalu,
                        salesGrowthMoM = growth,
                        totalPenjualanHariIni = totalHariIni,
                        avgTransactionValue = atv,
                        bestSellingProduct = bestSeller?.value,
                        totalTransaksiBulanIni = jumlahBulanIni
                    )

                    // --- Kalkulasi Data Grafik (7 Hari Terakhir) ---
                    val today = LocalDate.now(ZoneId.systemDefault())
                    // Ambil data transaksi yang relevan untuk grafik
                    val chartTransactions = allTransactions.filter { it.tanggal.toDate().time >= startOfLast7Days.toDate().time }

                    _chartData.value = (0..6).map { daysAgo ->
                        val date = today.minusDays(daysAgo.toLong())
                        val totalSalesOnDate = chartTransactions
                            .filter { it.tanggal.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date }
                            .sumOf { it.total_harga }
                        DailyChartEntry(date = date, yValue = totalSalesOnDate.toFloat())
                    }.reversed() // Urutkan dari terlama ke terbaru

                    _recentTransactions.value = thisMonthTransactions.take(20)
                }
        }
    }

    private val _selectedExportDate = MutableStateFlow(Calendar.getInstance())
    val selectedExportDate = _selectedExportDate.asStateFlow()

    fun setExportDate(year: Int, month: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(Calendar.YEAR, year)
        newCal.set(Calendar.MONTH, month)
        newCal.set(Calendar.DAY_OF_MONTH, 1)
        _selectedExportDate.value = newCal
    }

    fun exportDataToExcel(context: Context) {
        viewModelScope.launch {
            // 1. Ambil tanggal dari state yang dipilih
            val targetDate = _selectedExportDate.value

            // 2. Tentukan Awal Bulan Terpilih (Tgl 1 jam 00:00:00)
            val startCal = targetDate.clone() as Calendar
            startCal.set(Calendar.DAY_OF_MONTH, 1)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            val startTimestamp = Timestamp(startCal.time)

            // 3. Tentukan Akhir Bulan Terpilih (Awal bulan berikutnya)
            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.MONTH, 1)
            val endTimestamp = Timestamp(endCal.time)

            // 4. Query dengan Rentang Waktu Spesifik
            firestore.collection("transaksi")
                .whereGreaterThanOrEqualTo("tanggal", startTimestamp)
                .whereLessThan("tanggal", endTimestamp) // Ambil data SEBELUM bulan depan
                .orderBy("tanggal", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val transactions = documents.toObjects(Transaksi::class.java)
                    if (transactions.isNotEmpty()) {
                        // Panggil Helper Excel
                        ExcelHelper(context).exportToExcel(transactions)
                    } else {
                        // Opsional: Beri tahu jika data kosong
                        // Toast.makeText(context, "Tidak ada data di bulan ini", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Export", "Error exporting", e)
                }
        }
    }
}