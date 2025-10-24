package com.dam.financetracker.models

import java.time.Year

/**
 * Modelo que representa un punto en el grafico de tendencias
 * Agrupa transcciaciones por mes
 */

data class MonthlyData(
    val month: String,     //Nombre del mes (Ene, Feb,Mar, etc.)
    val monthNumber: Int,  //Numero del mes (1-12)
    val year: Int,         //Año
    val income: Float,     //Total de ingresos del mes
    val expense: Float,     //Total de gastos del mes
)

/**
 * Modelo que contiene todos los datos de tendencias para el gráfico
 */

data class TrendData(
    val monthlyDataList: List<MonthlyData> = emptyList(),
    val maxIncome: Float = 0f,  // Máximo ingreso (para escala del gráfico)
    val maxExpense: Float = 0f, // Máximo gastos (para escala del gráfico)
)
