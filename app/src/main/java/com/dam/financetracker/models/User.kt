package com.dam.financetracker.models

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val businessId: String = "",
    val role: UserRole = UserRole.OWNER,
    val createdAt: Long = System.currentTimeMillis(),
    @field:PropertyName("active")
    val isActive: Boolean = true
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", "", UserRole.OWNER, System.currentTimeMillis(), true)
}

enum class UserRole {
    OWNER,      // Propietario - acceso total
    EMPLOYEE,   // Empleado autorizado - acceso limitado
    ACCOUNTANT  // Contador/Asesor - solo lectura y reportes
}