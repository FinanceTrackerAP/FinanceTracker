package com.dam.financetracker.repository

import com.dam.financetracker.models.MonthlyData
import com.dam.financetracker.models.ReportData
import com.dam.financetracker.models.ReportPeriod
import com.dam.financetracker.models.TrendData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class ReportsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Obtiene el reporte financiero para el período especificado
     */
    suspend fun getMonthlyReport(period: ReportPeriod): ReportData {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return ReportData()

            // Calcular fechas del período actual
            val (startDate, endDate) = getDateRangeForPeriod(period)

            // Obtener transacciones del período actual
            val currentTransactions = getTransactionsForPeriod(currentUserId, startDate, endDate)

            // Calcular totales del período actual
            val currentIncome = currentTransactions
                .filter { it["type"] == "INCOME" }
                .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

            val currentExpense = currentTransactions
                .filter { it["type"] == "EXPENSE" }
                .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

            val currentBalance = currentIncome - currentExpense

            // Calcular fechas del período anterior (para comparación)
            val (previousStartDate, previousEndDate) = getPreviousDateRange(period)

            // Obtener transacciones del período anterior
            val previousTransactions = getTransactionsForPeriod(currentUserId, previousStartDate, previousEndDate)

            // Calcular totales del período anterior
            val previousIncome = previousTransactions
                .filter { it["type"] == "INCOME" }
                .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

            val previousExpense = previousTransactions
                .filter { it["type"] == "EXPENSE" }
                .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

            val previousBalance = previousIncome - previousExpense

            // Calcular porcentajes de cambio
            val incomePercentage = calculatePercentageChange(previousIncome, currentIncome)
            val expensePercentage = calculatePercentageChange(previousExpense, currentExpense)
            val balancePercentage = calculatePercentageChange(previousBalance, currentBalance)

            ReportData(
                totalIncome = currentIncome,
                totalExpense = currentExpense,
                balance = currentBalance,
                incomePercentage = incomePercentage,
                expensePercentage = expensePercentage,
                balancePercentage = balancePercentage
            )

        } catch (e: Exception) {
            ReportData()
        }
    }

    /**
     * Obtiene los datos de tendencias agrupados por día
     */
    suspend fun getTrendData(period: ReportPeriod): TrendData {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return TrendData()

            val (startDate, endDate) = getDateRangeForPeriod(period)
            val transactions = getTransactionsForPeriod(currentUserId, startDate, endDate)

            // Agrupar transacciones por DÍA (en lugar de mes)
            val dailyMap = mutableMapOf<String, Pair<Float, Float>>() // día -> (income, expense)

            transactions.forEach { transaction ->
                val date = (transaction["date"] as? Long) ?: return@forEach
                val calendar = Calendar.getInstance().apply { timeInMillis = date }

                // Crear clave única por día: "año-mes-día"
                val dayKey = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                val amount = (transaction["amount"] as? Number)?.toFloat() ?: 0f
                val type = transaction["type"] as? String

                val current = dailyMap[dayKey] ?: Pair(0f, 0f)
                dailyMap[dayKey] = when (type) {
                    "INCOME" -> Pair(current.first + amount, current.second)
                    "EXPENSE" -> Pair(current.first, current.second + amount)
                    else -> current
                }
            }

            // Convertir a lista de MonthlyData ordenada por fecha
            val dailyDataList = dailyMap.entries
                .sortedBy { it.key } // Ordenar por la clave "yyyy-MM-dd"
                .map { (key, values) ->
                    val parts = key.split("-")
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()

                    // Formato de etiqueta: "6 Oct" o "13 Oct"
                    val monthName = getMonthNameShort(month - 1)
                    val dayLabel = "$day $monthName"

                    MonthlyData(
                        month = dayLabel,
                        monthNumber = month,
                        year = year,
                        income = values.first,
                        expense = values.second
                    )
                }

            // Calcular máximos para la escala del gráfico
            val maxIncome = dailyDataList.maxOfOrNull { it.income } ?: 0f
            val maxExpense = dailyDataList.maxOfOrNull { it.expense } ?: 0f

            TrendData(
                monthlyDataList = dailyDataList,
                maxIncome = maxIncome,
                maxExpense = maxExpense
            )

        } catch (e: Exception) {
            android.util.Log.e("ReportsRepository", "Error in getTrendData: ${e.message}")
            TrendData()
        }
    }

    /**
     * Convierte número de mes a nombre abreviado de 3 letras
     */
    private fun getMonthNameShort(monthNumber: Int): String {
        val months = arrayOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )
        return months.getOrNull(monthNumber) ?: "Mes"
    }

    /**
     * Obtiene transacciones de Firestore para un rango de fechas
     */
    private suspend fun getTransactionsForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Map<String, Any>> {
        return try {
            // Obtener TODAS las transacciones del usuario (sin filtro de userId/businessId primero)
            val snapshot = firestore.collection("transactions")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            // Filtrar en memoria por userId o businessId
            val allTransactions = snapshot.documents.mapNotNull { it.data }

            // Obtener businessId del usuario
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val businessId = userDoc.getString("businessId")

            // Filtrar transacciones que pertenezcan al usuario
            allTransactions.filter { transaction ->
                val transUserId = transaction["userId"] as? String
                val transBusinessId = transaction["businessId"] as? String
                transUserId == userId || transBusinessId == businessId
            }

        } catch (e: Exception) {
            android.util.Log.e("ReportsRepository", "Error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Calcula el porcentaje de cambio entre dos valores
     */
    private fun calculatePercentageChange(previous: Double, current: Double): Double {
        return if (previous == 0.0) {
            if (current > 0) 100.0 else 0.0
        } else {
            ((current - previous) / previous) * 100
        }
    }

    /**
     * Obtiene el rango de fechas para el período especificado
     */
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

    /**
     * Obtiene el rango de fechas del período anterior (para comparación)
     */
    private fun getPreviousDateRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Calcular fecha de inicio del período anterior
        calendar.add(Calendar.DAY_OF_YEAR, when (period) {
            ReportPeriod.ONE_MONTH -> -60  // De -60 a -30 días
            ReportPeriod.THREE_MONTHS -> -180 // De -180 a -90 días
            ReportPeriod.ONE_YEAR -> -730 // De -730 a -365 días
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

}