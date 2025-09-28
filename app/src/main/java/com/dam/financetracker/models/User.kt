package com.dam.financetracker.models

data class User(
    val uid: String = "",
    val email: String = "",
    val businessId: String = "",  // ← Cambiar a minúscula
    val role: UserRole = UserRole.OWNER,
    val createdAt: Long = System.currentTimeMillis(),  // ← También corregir 'createAt'
    val isActive: Boolean = true
)

enum class UserRole {
    OWNER,      // Propietario - acceso total
    EMPLOYEE,   // Empleado autorizado - acceso limitado
    ACCOUNTANT  // Contador/Asesor - solo lectura y reportes
}