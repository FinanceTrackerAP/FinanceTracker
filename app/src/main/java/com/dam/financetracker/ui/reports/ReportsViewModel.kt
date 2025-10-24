package com.dam.financetracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dam.financetracker.models.ReportData
import com.dam.financetracker.models.ReportPeriod
import com.dam.financetracker.models.TrendData
import com.dam.financetracker.repository.ReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val reportsRepository = ReportsRepository()

    // Estados para los datos del reporte
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData

    // Estados para los datos del gráfico
    private val _trendData = MutableStateFlow(TrendData())
    val trendData: StateFlow<TrendData> = _trendData

    // Estado del período seleccionado
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.ONE_MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Cargar datos iniciales con período de 1 mes
        loadReportData(ReportPeriod.ONE_MONTH)
    }

    /**
     * Carga los datos del reporte para el período especificado
     */
    fun loadReportData(period: ReportPeriod) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _selectedPeriod.value = period

                // Cargar datos de métricas y tendencias en paralelo
                val reportData = reportsRepository.getMonthlyReport(period)
                val trendData = reportsRepository.getTrendData(period)

                _reportData.value = reportData
                _trendData.value = trendData

            } catch (e: Exception) {
                _error.value = "Error al cargar datos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cambia el período del reporte
     */
    fun changePeriod(period: ReportPeriod) {
        if (_selectedPeriod.value != period) {
            loadReportData(period)
        }
    }

    /**
     * Refresca los datos del reporte actual
     */
    fun refreshData() {
        loadReportData(_selectedPeriod.value)
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _error.value = null
    }
}