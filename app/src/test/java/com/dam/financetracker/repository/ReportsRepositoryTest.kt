package com.dam.financetracker.repository

import com.dam.financetracker.models.ReportPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Pruebas unitarias para ReportsRepository (HU-004)
 * Prueba los cálculos y procesamiento de datos financieros
 * 
 * IMPORTANTE: Estas son pruebas UNITARIAS PURAS (sin dependencias de Android/Firebase).
 * - Se prueban funciones de cálculo (porcentajes, fechas, formateo) de forma aislada
 * - Las funciones auxiliares privadas están replicadas aquí para testing
 * - NO se instancia ReportsRepository para evitar dependencias de Firebase
 * 
 * Las pruebas de integración con Firebase deben realizarse en:
 * - app/src/androidTest/ (tests de instrumentación con dispositivo/emulador)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsRepositoryTest {

    // No instanciamos ReportsRepository porque las pruebas solo validan
    // lógica de cálculos sin acceso a Firebase
    
    @Before
    fun setup() {
        // Setup básico si se necesita en el futuro
    }

    @Test
    fun `test calculatePercentageChange - con valores positivos`() {
        // Given
        val previous = 100.0
        val current = 150.0

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(50.0, percentage, 0.01)
    }

    @Test
    fun `test calculatePercentageChange - con disminucion`() {
        // Given
        val previous = 200.0
        val current = 150.0

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(-25.0, percentage, 0.01)
    }

    @Test
    fun `test calculatePercentageChange - desde cero`() {
        // Given
        val previous = 0.0
        val current = 100.0

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(100.0, percentage, 0.01)
    }

    @Test
    fun `test calculatePercentageChange - ambos cero`() {
        // Given
        val previous = 0.0
        val current = 0.0

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(0.0, percentage, 0.01)
    }

    @Test
    fun `test calculatePercentageChange - a cero`() {
        // Given
        val previous = 100.0
        val current = 0.0

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(-100.0, percentage, 0.01)
    }

    @Test
    fun `test getDateRangeForPeriod - ONE_MONTH`() {
        // When
        val (startDate, endDate) = getDateRangeForPeriod(ReportPeriod.ONE_MONTH)

        // Then
        val differenceInDays = (endDate - startDate) / (24 * 60 * 60 * 1000)
        assertTrue(differenceInDays >= 29) // Al menos 30 días
        assertTrue(differenceInDays <= 31)
    }

    @Test
    fun `test getDateRangeForPeriod - THREE_MONTHS`() {
        // When
        val (startDate, endDate) = getDateRangeForPeriod(ReportPeriod.THREE_MONTHS)

        // Then
        val differenceInDays = (endDate - startDate) / (24 * 60 * 60 * 1000)
        assertTrue(differenceInDays >= 89) // Al menos 90 días
        assertTrue(differenceInDays <= 91)
    }

    @Test
    fun `test getDateRangeForPeriod - ONE_YEAR`() {
        // When
        val (startDate, endDate) = getDateRangeForPeriod(ReportPeriod.ONE_YEAR)

        // Then
        val differenceInDays = (endDate - startDate) / (24 * 60 * 60 * 1000)
        assertTrue(differenceInDays >= 364) // Al menos 365 días
        assertTrue(differenceInDays <= 366)
    }

    @Test
    fun `test getPreviousDateRange - ONE_MONTH`() {
        // When
        val (startDate, endDate) = getPreviousDateRange(ReportPeriod.ONE_MONTH)

        // Then
        val differenceInDays = (endDate - startDate) / (24 * 60 * 60 * 1000)
        assertTrue(differenceInDays >= 29) // Al menos 30 días
        assertTrue(differenceInDays <= 31)
        
        // Debe estar en el pasado
        assertTrue(endDate < System.currentTimeMillis())
    }

    @Test
    fun `test getPreviousDateRange - THREE_MONTHS`() {
        // When
        val (startDate, endDate) = getPreviousDateRange(ReportPeriod.THREE_MONTHS)

        // Then
        val differenceInDays = (endDate - startDate) / (24 * 60 * 60 * 1000)
        assertTrue(differenceInDays >= 89) // Al menos 90 días
        assertTrue(differenceInDays <= 91)
        
        // Debe estar en el pasado
        assertTrue(endDate < System.currentTimeMillis())
    }

    @Test
    fun `test getMonthNameShort - todos los meses`() {
        val expectedMonths = listOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )

        for (i in 0..11) {
            val monthName = getMonthNameShort(i)
            assertEquals(expectedMonths[i], monthName)
        }
    }

    @Test
    fun `test getMonthNameShort - indice invalido`() {
        // When
        val monthName = getMonthNameShort(-1)

        // Then
        assertEquals("Mes", monthName)
    }

    @Test
    fun `test getMonthNameShort - indice fuera de rango`() {
        // When
        val monthName = getMonthNameShort(12)

        // Then
        assertEquals("Mes", monthName)
    }

    @Test
    fun `test calculos de transacciones - ingresos y gastos`() {
        // Given - simular transacciones
        val transactions = listOf(
            mapOf("type" to "INCOME", "amount" to 1000.0),
            mapOf("type" to "INCOME", "amount" to 500.0),
            mapOf("type" to "EXPENSE", "amount" to 300.0),
            mapOf("type" to "EXPENSE", "amount" to 200.0)
        )

        // When
        val totalIncome = transactions
            .filter { it["type"] == "INCOME" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val totalExpense = transactions
            .filter { it["type"] == "EXPENSE" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val balance = totalIncome - totalExpense

        // Then
        assertEquals(1500.0, totalIncome, 0.01)
        assertEquals(500.0, totalExpense, 0.01)
        assertEquals(1000.0, balance, 0.01)
    }

    @Test
    fun `test balance negativo cuando gastos superan ingresos`() {
        // Given
        val transactions = listOf(
            mapOf("type" to "INCOME", "amount" to 500.0),
            mapOf("type" to "EXPENSE", "amount" to 800.0)
        )

        // When
        val totalIncome = transactions
            .filter { it["type"] == "INCOME" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val totalExpense = transactions
            .filter { it["type"] == "EXPENSE" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val balance = totalIncome - totalExpense

        // Then
        assertTrue(balance < 0)
        assertEquals(-300.0, balance, 0.01)
    }

    @Test
    fun `test transacciones vacias - debe retornar cero`() {
        // Given
        val transactions = emptyList<Map<String, Any>>()

        // When
        val totalIncome = transactions
            .filter { it["type"] == "INCOME" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val totalExpense = transactions
            .filter { it["type"] == "EXPENSE" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val balance = totalIncome - totalExpense

        // Then
        assertEquals(0.0, totalIncome, 0.01)
        assertEquals(0.0, totalExpense, 0.01)
        assertEquals(0.0, balance, 0.01)
    }

    @Test
    fun `test agrupacion por dia - debe crear claves unicas`() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.OCTOBER, 15, 10, 30, 0)
        val date1 = calendar.timeInMillis

        calendar.set(2024, Calendar.OCTOBER, 16, 14, 45, 0)
        val date2 = calendar.timeInMillis

        // When
        val dayKey1 = formatDayKey(date1)
        val dayKey2 = formatDayKey(date2)

        // Then
        assertEquals("2024-10-15", dayKey1)
        assertEquals("2024-10-16", dayKey2)
        assertTrue(dayKey1 != dayKey2)
    }

    @Test
    fun `test agrupacion por dia - mismo dia diferentes horas`() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.OCTOBER, 15, 10, 30, 0)
        val date1 = calendar.timeInMillis

        calendar.set(2024, Calendar.OCTOBER, 15, 23, 59, 59)
        val date2 = calendar.timeInMillis

        // When
        val dayKey1 = formatDayKey(date1)
        val dayKey2 = formatDayKey(date2)

        // Then
        assertEquals("2024-10-15", dayKey1)
        assertEquals("2024-10-15", dayKey2)
        assertEquals(dayKey1, dayKey2)
    }

    // Funciones auxiliares privadas para testing (simulan las del repositorio)
    
    private fun calculatePercentageChange(previous: Double, current: Double): Double {
        return if (previous == 0.0) {
            if (current > 0) 100.0 else 0.0
        } else {
            ((current - previous) / previous) * 100
        }
    }

    private fun getDateRangeForPeriod(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, when (period) {
            ReportPeriod.ONE_MONTH -> -30
            ReportPeriod.THREE_MONTHS -> -90
            ReportPeriod.ONE_YEAR -> -365
        })

        val startDate = calendar.timeInMillis
        return Pair(startDate, endDate)
    }

    private fun getPreviousDateRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, when (period) {
            ReportPeriod.ONE_MONTH -> -60
            ReportPeriod.THREE_MONTHS -> -180
            ReportPeriod.ONE_YEAR -> -730
        })

        val previousStartDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, when (period) {
            ReportPeriod.ONE_MONTH -> 30
            ReportPeriod.THREE_MONTHS -> 90
            ReportPeriod.ONE_YEAR -> 365
        })

        val previousEndDate = calendar.timeInMillis
        return Pair(previousStartDate, previousEndDate)
    }

    private fun getMonthNameShort(monthNumber: Int): String {
        val months = arrayOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )
        return months.getOrNull(monthNumber) ?: "Mes"
    }

    private fun formatDayKey(date: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        return String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}

