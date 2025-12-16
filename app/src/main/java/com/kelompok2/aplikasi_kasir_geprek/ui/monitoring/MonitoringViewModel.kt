package com.kelompok2.aplikasi_kasir_geprek.ui.monitoring

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kelompok2.aplikasi_kasir_geprek.data.model.Transaksi
import com.kelompok2.aplikasi_kasir_geprek.ui.utils.ExcelHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

data class DashboardStats(
    val totalPenjualanBulanIni: Int = 0,
    val totalPenjualanBulanLalu: Int = 0,
    val salesGrowthMoM: Float? = null, // Growth Month-over-Month dalam %
    val totalPenjualanHariIni: Int = 0,
    val avgTransactionValue: Int = 0,
    val bestSellingProduct: Pair<String, Int>? = null,
    val totalTransaksiBulanIni: Int = 0
)

/**
 * Model data tunggal untuk grafik batang.
 */
data class DailyChartEntry(
    val date: LocalDate,
    val yValue: Float
)

class MonitoringViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    // --- StateFlow (UI State) ---

    // Statistik Utama (Total, Growth, KPI)
    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()

    // Daftar transaksi yang sesuai dengan bulan terpilih
    private val _recentTransactions = MutableStateFlow<List<Transaksi>>(emptyList())
    val recentTransactions = _recentTransactions.asStateFlow()

    // Data untuk grafik harian (1 bulan penuh)
    private val _chartData = MutableStateFlow<List<DailyChartEntry>>(emptyList())
    val chartData = _chartData.asStateFlow()

    // Bulan yang sedang dipilih user (Default: Bulan Ini)
    private val _selectedExportDate = MutableStateFlow(Calendar.getInstance())
    val selectedExportDate = _selectedExportDate.asStateFlow()

    // Listener Firestore (disimpan agar bisa dicabut/remove saat ganti bulan)
    private var snapshotListener: ListenerRegistration? = null

    init {
        // Load data bulan saat ini ketika ViewModel dibuat
        loadDataForSelectedMonth()
    }

    /**
     * Mengubah bulan laporan. Dipanggil saat user menekan tombol panah < atau >.
     * Otomatis memicu reload data.
     */
    fun setExportDate(year: Int, month: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(Calendar.YEAR, year)
        newCal.set(Calendar.MONTH, month)
        newCal.set(Calendar.DAY_OF_MONTH, 1)
        _selectedExportDate.value = newCal

        loadDataForSelectedMonth()
    }

    /**
     * Logika utama: Mengambil data dari Firestore berdasarkan bulan yang dipilih.
     * Kita mengambil range 2 bulan (Bulan Lalu s/d Bulan Ini) untuk menghitung pertumbuhan (Growth).
     */
    private fun loadDataForSelectedMonth() {
        // 1. Bersihkan listener lama untuk mencegah memory leak atau data ganda
        snapshotListener?.remove()

        val selectedCal = _selectedExportDate.value.clone() as Calendar

        // A. Tentukan Batas Awal Bulan Terpilih (Tgl 1, 00:00:00)
        selectedCal.set(Calendar.DAY_OF_MONTH, 1)
        selectedCal.set(Calendar.HOUR_OF_DAY, 0); selectedCal.set(Calendar.MINUTE, 0); selectedCal.set(Calendar.SECOND, 0); selectedCal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = Timestamp(selectedCal.time)

        // B. Tentukan Batas Akhir Bulan Terpilih (Awal bulan depannya)
        val nextMonthCal = selectedCal.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)
        val endOfMonth = Timestamp(nextMonthCal.time)

        // C. Tentukan Awal Bulan LALU (Untuk komparasi Growth MoM)
        val lastMonthCal = selectedCal.clone() as Calendar
        lastMonthCal.add(Calendar.MONTH, -1)
        val startOfLastMonth = Timestamp(lastMonthCal.time)

        // 2. Query Firestore
        // Kita ambil data mulai dari "Awal Bulan Lalu" sampai "Akhir Bulan Ini"
        snapshotListener = firestore.collection("transaksi")
            .whereGreaterThanOrEqualTo("tanggal", startOfLastMonth)
            .whereLessThan("tanggal", endOfMonth)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("MonitoringVM", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val allFetched = snapshots?.toObjects(Transaksi::class.java) ?: emptyList()

                // 3. Pisahkan Data di Memory (Bulan Terpilih vs Bulan Sebelumnya)
                val selectedMonthTrx = allFetched.filter { it.tanggal.toDate().time >= startOfMonth.toDate().time }
                val previousMonthTrx = allFetched.filter { it.tanggal.toDate().time < startOfMonth.toDate().time }

                // --- KALKULASI STATISTIK ---
                val totalSelected = selectedMonthTrx.sumOf { it.total_harga }
                val totalPrev = previousMonthTrx.sumOf { it.total_harga }

                // Hitung % Pertumbuhan (Growth)
                val growth = if (totalPrev > 0) {
                    ((totalSelected.toFloat() - totalPrev.toFloat()) / totalPrev.toFloat()) * 100
                } else if (totalSelected > 0) {
                    100.0f // Naik 100% jika bulan lalu 0
                } else {
                    0.0f
                }

                val countSelected = selectedMonthTrx.size
                val atv = if (countSelected > 0) totalSelected / countSelected else 0

                // Cari Produk Terlaris
                val productMap = mutableMapOf<String, Pair<String, Int>>()
                selectedMonthTrx.forEach { trx ->
                    trx.items.forEach { item ->
                        val current = productMap[item.id_menu]
                        val newQty = (current?.second ?: 0) + item.qty
                        productMap[item.id_menu] = Pair(item.nama_menu, newQty)
                    }
                }
                val bestSeller = productMap.maxByOrNull { it.value.second }

                // Update State Dashboard
                _stats.value = DashboardStats(
                    totalPenjualanBulanIni = totalSelected,
                    totalPenjualanBulanLalu = totalPrev,
                    salesGrowthMoM = growth,
                    totalPenjualanHariIni = 0, // Bisa disesuaikan jika butuh data harian spesifik
                    avgTransactionValue = atv,
                    bestSellingProduct = bestSeller?.value,
                    totalTransaksiBulanIni = countSelected
                )

                // --- KALKULASI GRAFIK (Full 1 Bulan) ---
                val daysInMonth = selectedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val chartEntries = (1..daysInMonth).map { day ->
                    // Buat LocalDate untuk tanggal tersebut
                    val dateToCheck = LocalDate.of(
                        selectedCal.get(Calendar.YEAR),
                        selectedCal.get(Calendar.MONTH) + 1, // Calendar 0-11, LocalDate 1-12
                        day
                    )

                    // Sum total harga di tanggal tersebut
                    val sumDay = selectedMonthTrx
                        .filter {
                            val trxDate = it.tanggal.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                            trxDate == dateToCheck
                        }
                        .sumOf { it.total_harga }

                    DailyChartEntry(dateToCheck, sumDay.toFloat())
                }
                _chartData.value = chartEntries

                // --- UPDATE LIST TRANSAKSI ---
                // Data ini otomatis sesuai dengan bulan yang dipilih
                _recentTransactions.value = selectedMonthTrx
            }
    }

    /**
     * Ekspor data ke Excel.
     * Menggunakan data _recentTransactions yang SUDAH terfilter berdasarkan bulan yang dipilih.
     */
    fun exportDataToExcel(context: Context) {
        viewModelScope.launch {
            val currentList = _recentTransactions.value
            if (currentList.isNotEmpty()) {
                ExcelHelper(context).exportToExcel(currentList)
            } else {
                // Opsional: Handle jika data kosong
                Log.d("Export", "Data kosong, tidak ada yang diekspor")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove() // Bersihkan listener
    }
}