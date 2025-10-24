package com.dam.financetracker.models

/**
 * Modelo donde esta las metricas financieras para un periodo especifico
 */

data class ReportData(
    val totalIncome: Double = 0.0,          //Total de ingreso
    val totalExpense: Double = 0.0,         //Total de gastos
    val balance: Double = 0.0,              //Balance(Ingreso - gastos)
    val incomePercentage: Double = 0.0,     //% cambio de ingresos vs periodo anterior
    val expensePercentage: Double = 0.0,    //% cambio de gastos vs periodo anterior
    val balancePercentage: Double = 0.0,    //% cambio de balance vs periodo anterior
)

/**
 * Enum para los periodos de tiempo de reporte
 */

enum class ReportPeriod{
    ONE_MONTH,       //(30 dias)
    THREE_MONTHS,   //(90 dias)
    ONE_YEAR,       //(365 dias)
}


