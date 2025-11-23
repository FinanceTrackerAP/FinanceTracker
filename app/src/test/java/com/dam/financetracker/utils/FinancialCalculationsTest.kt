package com.dam.financetracker.utils

import com.dam.financetracker.models.MonthlyData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

/**
 * Pruebas unitarias para cálculos financieros (HU-004)
 * Prueba los cálculos de métricas, porcentajes, formatos y agregaciones
 */
class FinancialCalculationsTest {

    @Test
    fun `test calculo de porcentaje de cambio - incremento`() {
        // Given
        val previousValue = 1000.0
        val currentValue = 1250.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(25.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de porcentaje de cambio - decremento`() {
        // Given
        val previousValue = 1000.0
        val currentValue = 750.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(-25.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de porcentaje de cambio - sin cambio`() {
        // Given
        val previousValue = 1000.0
        val currentValue = 1000.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(0.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de porcentaje de cambio - desde cero`() {
        // Given
        val previousValue = 0.0
        val currentValue = 500.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(100.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de porcentaje de cambio - a cero`() {
        // Given
        val previousValue = 500.0
        val currentValue = 0.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(-100.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de porcentaje de cambio - ambos cero`() {
        // Given
        val previousValue = 0.0
        val currentValue = 0.0

        // When
        val percentage = calculatePercentageChange(previousValue, currentValue)

        // Then
        assertEquals(0.0, percentage, 0.01)
    }

    @Test
    fun `test calculo de balance - ingresos mayores que gastos`() {
        // Given
        val income = 5000.0
        val expense = 3000.0

        // When
        val balance = calculateBalance(income, expense)

        // Then
        assertEquals(2000.0, balance, 0.01)
        assertTrue(balance > 0)
    }

    @Test
    fun `test calculo de balance - gastos mayores que ingresos`() {
        // Given
        val income = 2000.0
        val expense = 3500.0

        // When
        val balance = calculateBalance(income, expense)

        // Then
        assertEquals(-1500.0, balance, 0.01)
        assertTrue(balance < 0)
    }

    @Test
    fun `test calculo de balance - iguales`() {
        // Given
        val income = 1000.0
        val expense = 1000.0

        // When
        val balance = calculateBalance(income, expense)

        // Then
        assertEquals(0.0, balance, 0.01)
    }

    @Test
    fun `test agregacion de ingresos - lista de transacciones`() {
        // Given
        val transactions = listOf(
            mapOf("type" to "INCOME", "amount" to 1000.0),
            mapOf("type" to "INCOME", "amount" to 1500.0),
            mapOf("type" to "INCOME", "amount" to 500.0)
        )

        // When
        val totalIncome = aggregateIncome(transactions)

        // Then
        assertEquals(3000.0, totalIncome, 0.01)
    }

    @Test
    fun `test agregacion de gastos - lista de transacciones`() {
        // Given
        val transactions = listOf(
            mapOf("type" to "EXPENSE", "amount" to 500.0),
            mapOf("type" to "EXPENSE", "amount" to 800.0),
            mapOf("type" to "EXPENSE", "amount" to 200.0)
        )

        // When
        val totalExpense = aggregateExpense(transactions)

        // Then
        assertEquals(1500.0, totalExpense, 0.01)
    }

    @Test
    fun `test agregacion mixta - ingresos y gastos`() {
        // Given
        val transactions = listOf(
            mapOf("type" to "INCOME", "amount" to 1000.0),
            mapOf("type" to "EXPENSE", "amount" to 300.0),
            mapOf("type" to "INCOME", "amount" to 500.0),
            mapOf("type" to "EXPENSE", "amount" to 200.0)
        )

        // When
        val totalIncome = aggregateIncome(transactions)
        val totalExpense = aggregateExpense(transactions)
        val balance = calculateBalance(totalIncome, totalExpense)

        // Then
        assertEquals(1500.0, totalIncome, 0.01)
        assertEquals(500.0, totalExpense, 0.01)
        assertEquals(1000.0, balance, 0.01)
    }

    @Test
    fun `test formato de moneda - valores positivos`() {
        // Given
        val amount = 1234.56

        // When
        val formatted = formatCurrency(amount)

        // Then
        assertTrue(formatted.contains("1"))
        assertTrue(formatted.contains("234"))
        assertTrue(formatted.contains("56"))
    }

    @Test
    fun `test formato de moneda - valores negativos`() {
        // Given
        val amount = -500.75

        // When
        val formatted = formatCurrency(amount)

        // Then
        assertTrue(formatted.contains("500"))
        assertTrue(formatted.contains("75"))
    }

    @Test
    fun `test formato de moneda - cero`() {
        // Given
        val amount = 0.0

        // When
        val formatted = formatCurrency(amount)

        // Then
        assertTrue(formatted.contains("0"))
    }

    @Test
    fun `test formato de porcentaje - positivo con flecha arriba`() {
        // Given
        val percentage = 15.5

        // When
        val formatted = formatPercentage(percentage)

        // Then
        assertTrue(formatted.contains("↑"))
        assertTrue(formatted.contains("15.5"))
        assertTrue(formatted.contains("%"))
    }

    @Test
    fun `test formato de porcentaje - negativo con flecha abajo`() {
        // Given
        val percentage = -10.2

        // When
        val formatted = formatPercentage(percentage)

        // Then
        assertTrue(formatted.contains("↓"))
        assertTrue(formatted.contains("10.2"))
        assertTrue(formatted.contains("%"))
    }

    @Test
    fun `test formato de porcentaje - cero`() {
        // Given
        val percentage = 0.0

        // When
        val formatted = formatPercentage(percentage)

        // Then
        assertTrue(formatted.contains("0.0"))
        assertTrue(formatted.contains("%"))
    }

    @Test
    fun `test calculo de maximos en tendencias`() {
        // Given
        val monthlyDataList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 2500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 1500f, 1200f)
        )

        // When
        val maxIncome = monthlyDataList.maxOf { it.income }
        val maxExpense = monthlyDataList.maxOf { it.expense }

        // Then
        assertEquals(2500f, maxIncome, 0.01f)
        assertEquals(1200f, maxExpense, 0.01f)
    }

    @Test
    fun `test calculo de promedios - ingresos`() {
        // Given
        val monthlyDataList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 1100f, 600f)
        )

        // When
        val avgIncome = monthlyDataList.map { it.income }.average()

        // Then
        assertEquals(1200.0, avgIncome, 0.01)
    }

    @Test
    fun `test calculo de promedios - gastos`() {
        // Given
        val monthlyDataList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 1100f, 700f)
        )

        // When
        val avgExpense = monthlyDataList.map { it.expense }.average()

        // Then
        assertEquals(666.67, avgExpense, 0.01)
    }

    @Test
    fun `test calculo de totales acumulados`() {
        // Given
        val monthlyDataList = listOf(
            MonthlyData("1 Oct", 10, 2024, 1000f, 500f),
            MonthlyData("2 Oct", 10, 2024, 1500f, 800f),
            MonthlyData("3 Oct", 10, 2024, 1100f, 700f)
        )

        // When
        val totalIncome = monthlyDataList.sumOf { it.income.toDouble() }
        val totalExpense = monthlyDataList.sumOf { it.expense.toDouble() }

        // Then
        assertEquals(3600.0, totalIncome, 0.01)
        assertEquals(2000.0, totalExpense, 0.01)
    }

    @Test
    fun `test ratio de gastos sobre ingresos`() {
        // Given
        val income = 1000.0
        val expense = 750.0

        // When
        val ratio = calculateExpenseRatio(income, expense)

        // Then
        assertEquals(75.0, ratio, 0.01)  // 75% de los ingresos se gastaron
    }

    @Test
    fun `test ratio de gastos sobre ingresos - gastos mayores`() {
        // Given
        val income = 1000.0
        val expense = 1200.0

        // When
        val ratio = calculateExpenseRatio(income, expense)

        // Then
        assertEquals(120.0, ratio, 0.01)  // 120% - gastó más de lo que ingresó
    }

    @Test
    fun `test ratio de gastos sobre ingresos - ingresos cero`() {
        // Given
        val income = 0.0
        val expense = 500.0

        // When
        val ratio = calculateExpenseRatio(income, expense)

        // Then
        assertEquals(0.0, ratio, 0.01)  // No hay ingresos para calcular ratio
    }

    @Test
    fun `test validacion de balance positivo`() {
        // Given
        val balance = 1500.0

        // When
        val isPositive = isBalancePositive(balance)

        // Then
        assertTrue(isPositive)
    }

    @Test
    fun `test validacion de balance negativo`() {
        // Given
        val balance = -500.0

        // When
        val isPositive = isBalancePositive(balance)

        // Then
        assertFalse(isPositive)
    }

    @Test
    fun `test validacion de balance cero`() {
        // Given
        val balance = 0.0

        // When
        val isPositive = isBalancePositive(balance)

        // Then
        assertFalse(isPositive)  // Cero no es positivo
    }

    @Test
    fun `test redondeo de moneda a 2 decimales`() {
        // Given
        val amount = 123.456789

        // When
        val rounded = roundToTwoDecimals(amount)

        // Then
        assertEquals(123.46, rounded, 0.001)
    }

    @Test
    fun `test porcentaje de cambio con precision`() {
        // Given
        val previous = 100.0
        val current = 133.33

        // When
        val percentage = calculatePercentageChange(previous, current)

        // Then
        assertEquals(33.33, percentage, 0.01)
    }

    @Test
    fun `test suma segura con valores null`() {
        // Given
        val values = listOf<Number?>(100, null, 200, null, 300)

        // When
        val total = values.sumOf { (it as? Number)?.toDouble() ?: 0.0 }

        // Then
        assertEquals(600.0, total, 0.01)
    }

    // Funciones auxiliares para testing

    private fun calculatePercentageChange(previous: Double, current: Double): Double {
        return if (previous == 0.0) {
            if (current > 0) 100.0 else 0.0
        } else {
            ((current - previous) / previous) * 100
        }
    }

    private fun calculateBalance(income: Double, expense: Double): Double {
        return income - expense
    }

    private fun aggregateIncome(transactions: List<Map<String, Any>>): Double {
        return transactions
            .filter { it["type"] == "INCOME" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }

    private fun aggregateExpense(transactions: List<Map<String, Any>>): Double {
        return transactions
            .filter { it["type"] == "EXPENSE" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        return format.format(amount)
    }

    private fun formatPercentage(percentage: Double): String {
        val sign = if (percentage >= 0) "↑" else "↓"
        return "$sign ${String.format("%.1f", kotlin.math.abs(percentage))}%"
    }

    private fun calculateExpenseRatio(income: Double, expense: Double): Double {
        return if (income == 0.0) {
            0.0
        } else {
            (expense / income) * 100
        }
    }

    private fun isBalancePositive(balance: Double): Boolean {
        return balance > 0
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return kotlin.math.round(value * 100) / 100
    }
}

