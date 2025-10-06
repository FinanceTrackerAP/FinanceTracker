package com.dam.financetracker.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Transaction(
    val id: String = "",
    val businessId: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.INCOME,
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @field:PropertyName("active")
    val isActive: Boolean = true
) : Serializable {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", "", TransactionType.INCOME, 0.0, "", "", System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(), true)
}

enum class TransactionType {
    INCOME,     // Ingreso
    EXPENSE     // Gasto
}

data class TransactionCategory(
    val id: String = "",
    val name: String = "",
    val type: TransactionType = TransactionType.INCOME,
    val isDefault: Boolean = false,
    val userId: String = "" // <<--- ESTO ES VITAL

)

// Categorías por defecto para ingresos
object DefaultIncomeCategories {
    val categories = listOf(
        TransactionCategory("income_sales", "Ventas", TransactionType.INCOME, true),
        TransactionCategory("income_services", "Servicios", TransactionType.INCOME, true),
        TransactionCategory("income_other", "Otros ingresos", TransactionType.INCOME, true)
    )
}

// Categorías por defecto para gastos
object DefaultExpenseCategories {
    val categories = listOf(
        TransactionCategory("expense_operational", "Gastos operativos", TransactionType.EXPENSE, true),
        TransactionCategory("expense_supplies", "Materiales y suministros", TransactionType.EXPENSE, true),
        TransactionCategory("expense_marketing", "Marketing y publicidad", TransactionType.EXPENSE, true),
        TransactionCategory("expense_utilities", "Servicios públicos", TransactionType.EXPENSE, true),
        TransactionCategory("expense_rent", "Alquiler", TransactionType.EXPENSE, true),
        TransactionCategory("expense_other", "Otros gastos", TransactionType.EXPENSE, true)
    )
}
