package com.dam.financetracker.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para los modelos de datos de reportes (HU-004)
 * Prueba ReportData, TrendData, MonthlyData y ReportPeriod
 */
class ReportDataTest {

    @Test
    fun `test ReportData - valores por defecto`() {
        // When
        val reportData = ReportData()

        // Then
        assertEquals(0.0, reportData.totalIncome, 0.01)
        assertEquals(0.0, reportData.totalExpense, 0.01)
        assertEquals(0.0, reportData.balance, 0.01)
        assertEquals(0.0, reportData.incomePercentage, 0.01)
        assertEquals(0.0, reportData.expensePercentage, 0.01)
        assertEquals(0.0, reportData.balancePercentage, 0.01)
    }

    @Test
    fun `test ReportData - con valores positivos`() {
        // When
        val reportData = ReportData(
            totalIncome = 5000.0,
            totalExpense = 3000.0,
            balance = 2000.0,
            incomePercentage = 15.5,
            expensePercentage = -5.0,
            balancePercentage = 25.0
        )

        // Then
        assertEquals(5000.0, reportData.totalIncome, 0.01)
        assertEquals(3000.0, reportData.totalExpense, 0.01)
        assertEquals(2000.0, reportData.balance, 0.01)
        assertEquals(15.5, reportData.incomePercentage, 0.01)
        assertEquals(-5.0, reportData.expensePercentage, 0.01)
        assertEquals(25.0, reportData.balancePercentage, 0.01)
    }

    @Test
    fun `test ReportData - con balance negativo`() {
        // When
        val reportData = ReportData(
            totalIncome = 1000.0,
            totalExpense = 1500.0,
            balance = -500.0,
            incomePercentage = -10.0,
            expensePercentage = 20.0,
            balancePercentage = -50.0
        )

        // Then
        assertTrue(reportData.balance < 0)
        assertTrue(reportData.totalExpense > reportData.totalIncome)
        assertEquals(-500.0, reportData.balance, 0.01)
    }

    @Test
    fun `test ReportData - balance debe ser ingresos menos gastos`() {
        // Given
        val income = 10000.0
        val expense = 6500.0
        val expectedBalance = income - expense

        // When
        val reportData = ReportData(
            totalIncome = income,
            totalExpense = expense,
            balance = expectedBalance
        )

        // Then
        assertEquals(expectedBalance, reportData.balance, 0.01)
        assertEquals(3500.0, reportData.balance, 0.01)
    }

    @Test
    fun `test ReportData - porcentajes pueden ser negativos`() {
        // When
        val reportData = ReportData(
            incomePercentage = -15.5,
            expensePercentage = -25.0,
            balancePercentage = -10.0
        )

        // Then
        assertTrue(reportData.incomePercentage < 0)
        assertTrue(reportData.expensePercentage < 0)
        assertTrue(reportData.balancePercentage < 0)
    }

    @Test
    fun `test ReportData - igualdad de data class`() {
        // Given
        val reportData1 = ReportData(
            totalIncome = 1000.0,
            totalExpense = 500.0,
            balance = 500.0
        )
        val reportData2 = ReportData(
            totalIncome = 1000.0,
            totalExpense = 500.0,
            balance = 500.0
        )

        // Then
        assertEquals(reportData1, reportData2)
        assertEquals(reportData1.hashCode(), reportData2.hashCode())
    }

    @Test
    fun `test ReportData - copy function`() {
        // Given
        val original = ReportData(
            totalIncome = 1000.0,
            totalExpense = 500.0,
            balance = 500.0
        )

        // When
        val modified = original.copy(totalIncome = 2000.0)

        // Then
        assertEquals(2000.0, modified.totalIncome, 0.01)
        assertEquals(500.0, modified.totalExpense, 0.01)
        assertNotEquals(original, modified)
    }

    @Test
    fun `test MonthlyData - construccion correcta`() {
        // When
        val monthlyData = MonthlyData(
            month = "Oct",
            monthNumber = 10,
            year = 2024,
            income = 1500f,
            expense = 800f
        )

        // Then
        assertEquals("Oct", monthlyData.month)
        assertEquals(10, monthlyData.monthNumber)
        assertEquals(2024, monthlyData.year)
        assertEquals(1500f, monthlyData.income, 0.01f)
        assertEquals(800f, monthlyData.expense, 0.01f)
    }

    @Test
    fun `test MonthlyData - con valores cero`() {
        // When
        val monthlyData = MonthlyData(
            month = "Ene",
            monthNumber = 1,
            year = 2024,
            income = 0f,
            expense = 0f
        )

        // Then
        assertEquals(0f, monthlyData.income, 0.01f)
        assertEquals(0f, monthlyData.expense, 0.01f)
    }

    @Test
    fun `test MonthlyData - income mayor que expense`() {
        // When
        val monthlyData = MonthlyData(
            month = "Oct",
            monthNumber = 10,
            year = 2024,
            income = 2000f,
            expense = 500f
        )

        // Then
        assertTrue(monthlyData.income > monthlyData.expense)
    }

    @Test
    fun `test MonthlyData - expense mayor que income`() {
        // When
        val monthlyData = MonthlyData(
            month = "Oct",
            monthNumber = 10,
            year = 2024,
            income = 500f,
            expense = 1200f
        )

        // Then
        assertTrue(monthlyData.expense > monthlyData.income)
    }

    @Test
    fun `test TrendData - valores por defecto`() {
        // When
        val trendData = TrendData()

        // Then
        assertTrue(trendData.monthlyDataList.isEmpty())
        assertEquals(0f, trendData.maxIncome, 0.01f)
        assertEquals(0f, trendData.maxExpense, 0.01f)
    }

    @Test
    fun `test TrendData - con lista de datos`() {
        // Given
        val monthlyList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 2000f, 1200f)
        )

        // When
        val trendData = TrendData(
            monthlyDataList = monthlyList,
            maxIncome = 2000f,
            maxExpense = 1200f
        )

        // Then
        assertEquals(3, trendData.monthlyDataList.size)
        assertEquals(2000f, trendData.maxIncome, 0.01f)
        assertEquals(1200f, trendData.maxExpense, 0.01f)
    }

    @Test
    fun `test TrendData - maxIncome debe ser el mayor de la lista`() {
        // Given
        val monthlyList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 2500f, 800f),  // <- Mayor income
            MonthlyData("3 Oct", 10, 2024, 1500f, 1200f)
        )

        // When
        val maxIncome = monthlyList.maxOf { it.income }
        val trendData = TrendData(
            monthlyDataList = monthlyList,
            maxIncome = maxIncome
        )

        // Then
        assertEquals(2500f, trendData.maxIncome, 0.01f)
    }

    @Test
    fun `test TrendData - maxExpense debe ser el mayor de la lista`() {
        // Given
        val monthlyList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 2000f, 1500f)  // <- Mayor expense
        )

        // When
        val maxExpense = monthlyList.maxOf { it.expense }
        val trendData = TrendData(
            monthlyDataList = monthlyList,
            maxExpense = maxExpense
        )

        // Then
        assertEquals(1500f, trendData.maxExpense, 0.01f)
    }

    @Test
    fun `test TrendData - lista ordenada por fecha`() {
        // Given
        val monthlyList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 2000f, 1200f)
        )

        // When
        val trendData = TrendData(monthlyDataList = monthlyList)

        // Then
        assertEquals("1 Oct", trendData.monthlyDataList[0].month)
        assertEquals("2 Oct", trendData.monthlyDataList[1].month)
        assertEquals("3 Oct", trendData.monthlyDataList[2].month)
    }

    @Test
    fun `test ReportPeriod - valores del enum`() {
        // Then
        assertEquals(3, ReportPeriod.values().size)
        assertTrue(ReportPeriod.values().contains(ReportPeriod.ONE_MONTH))
        assertTrue(ReportPeriod.values().contains(ReportPeriod.THREE_MONTHS))
        assertTrue(ReportPeriod.values().contains(ReportPeriod.ONE_YEAR))
    }

    @Test
    fun `test ReportPeriod - valueOf`() {
        // When
        val period1 = ReportPeriod.valueOf("ONE_MONTH")
        val period2 = ReportPeriod.valueOf("THREE_MONTHS")
        val period3 = ReportPeriod.valueOf("ONE_YEAR")

        // Then
        assertEquals(ReportPeriod.ONE_MONTH, period1)
        assertEquals(ReportPeriod.THREE_MONTHS, period2)
        assertEquals(ReportPeriod.ONE_YEAR, period3)
    }

    @Test
    fun `test ReportPeriod - comparacion`() {
        // When
        val period1 = ReportPeriod.ONE_MONTH
        val period2 = ReportPeriod.ONE_MONTH

        // Then
        assertEquals(period1, period2)
    }

    @Test
    fun `test ReportPeriod - diferentes periodos`() {
        // When
        val period1 = ReportPeriod.ONE_MONTH
        val period2 = ReportPeriod.THREE_MONTHS

        // Then
        assertNotEquals(period1, period2)
    }

    @Test
    fun `test TrendData - lista vacia no debe causar error en maxIncome`() {
        // Given
        val emptyList = emptyList<MonthlyData>()

        // When
        val maxIncome = emptyList.maxOfOrNull { it.income } ?: 0f
        val trendData = TrendData(
            monthlyDataList = emptyList,
            maxIncome = maxIncome
        )

        // Then
        assertEquals(0f, trendData.maxIncome, 0.01f)
    }

    @Test
    fun `test TrendData - lista vacia no debe causar error en maxExpense`() {
        // Given
        val emptyList = emptyList<MonthlyData>()

        // When
        val maxExpense = emptyList.maxOfOrNull { it.expense } ?: 0f
        val trendData = TrendData(
            monthlyDataList = emptyList,
            maxExpense = maxExpense
        )

        // Then
        assertEquals(0f, trendData.maxExpense, 0.01f)
    }

    @Test
    fun `test MonthlyData - mes numero valido entre 1 y 12`() {
        // When
        val monthlyData = MonthlyData(
            month = "Dic",
            monthNumber = 12,
            year = 2024,
            income = 1000f,
            expense = 500f
        )

        // Then
        assertTrue(monthlyData.monthNumber in 1..12)
    }

    @Test
    fun `test ReportData - porcentajes extremos`() {
        // When
        val reportData = ReportData(
            incomePercentage = 1000.0,  // 1000% de incremento
            expensePercentage = -99.9,   // 99.9% de reducción
            balancePercentage = 500.0    // 500% de mejora
        )

        // Then
        assertTrue(reportData.incomePercentage > 100.0)
        assertTrue(reportData.expensePercentage < 0.0)
        assertTrue(reportData.balancePercentage > 100.0)
    }

    @Test
    fun `test TrendData - calculo de balance implícito por día`() {
        // Given
        val monthlyData = MonthlyData(
            month = "1 Oct",
            monthNumber = 10,
            year = 2024,
            income = 2000f,
            expense = 1200f
        )

        // When
        val dailyBalance = monthlyData.income - monthlyData.expense

        // Then
        assertEquals(800f, dailyBalance, 0.01f)
        assertTrue(dailyBalance > 0)
    }

    @Test
    fun `test TrendData - multiples dias con diferentes balances`() {
        // Given
        val monthlyList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),   // Balance: +500
            MonthlyData("2 Oct", 10, 2024, 800f, 900f),     // Balance: -100
            MonthlyData("3 Oct", 10, 2024, 1500f, 600f)    // Balance: +900
        )

        // When
        val balances = monthlyList.map { it.income - it.expense }

        // Then
        assertEquals(500f, balances[0], 0.01f)
        assertEquals(-100f, balances[1], 0.01f)
        assertEquals(900f, balances[2], 0.01f)
    }
}

