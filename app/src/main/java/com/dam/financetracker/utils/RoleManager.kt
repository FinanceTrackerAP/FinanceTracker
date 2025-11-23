package com.dam.financetracker.utils

import com.dam.financetracker.models.UserRole

/**
 * HU-007 y HU-008: Gestor de permisos según el rol del usuario
 * 
 * OWNER (Propietario): Acceso total
 * EMPLOYEE (Empleado): Solo puede ver Inicio y Transacciones, SOLO registra INGRESOS (ventas), NO gastos
 * ACCOUNTANT (Contador): Solo puede ver Transacciones (lectura) y Reportes, no puede crear transacciones ni ver Ajustes
 */
object RoleManager {
    
    // ==========================================
    // HU-007: Permisos Vista Empleado
    // ==========================================
    
    /**
     * Verifica si el usuario puede ver la pantalla de Reportes
     */
    fun canAccessReports(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> false  // HU-007: Empleado NO puede ver reportes
            UserRole.ACCOUNTANT -> true // HU-008: Contador SÍ puede ver reportes
        }
    }
    
    /**
     * Verifica si el usuario puede ver la pantalla de Ajustes/Configuración
     */
    fun canAccessSettings(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> false   // HU-007: Empleado NO puede ver ajustes
            UserRole.ACCOUNTANT -> false // HU-008: Contador NO puede ver ajustes
        }
    }
    
    /**
     * Verifica si el usuario puede crear transacciones
     */
    fun canCreateTransactions(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> true   // HU-007: Empleado SÍ puede crear (solo ingresos)
            UserRole.ACCOUNTANT -> false // HU-008: Contador NO puede crear transacciones
        }
    }
    
    /**
     * Verifica si el usuario puede registrar GASTOS
     * HU-007: Empleado SOLO puede registrar INGRESOS (ventas), NO puede registrar gastos
     */
    fun canCreateExpenses(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> false  // HU-007: Empleado SOLO registra INGRESOS, NO gastos
            UserRole.ACCOUNTANT -> false // HU-008: Contador no puede crear nada
        }
    }
    
    /**
     * Verifica si el usuario puede editar/eliminar transacciones
     */
    fun canEditTransactions(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> true   // HU-007: Empleado puede editar sus propias transacciones
            UserRole.ACCOUNTANT -> false // HU-008: Contador solo consulta
        }
    }
    
    /**
     * Verifica si el usuario puede exportar reportes (PDF/CSV)
     */
    fun canExportReports(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> false  // HU-007: Empleado NO puede exportar
            UserRole.ACCOUNTANT -> true // HU-008: Contador SÍ puede exportar
        }
    }
    
    /**
     * Verifica si el usuario puede ver el Dashboard/Inicio
     * HU-008: Contador NO necesita ver el Dashboard (solo consulta datos)
     */
    fun canAccessDashboard(role: UserRole): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.EMPLOYEE -> true
            UserRole.ACCOUNTANT -> false  // HU-008: Contador solo ve 3 pantallas
        }
    }
    
    /**
     * Verifica si el usuario puede ver las transacciones
     */
    fun canAccessTransactions(role: UserRole): Boolean {
        // Todos pueden ver transacciones
        return true
    }
    
    /**
     * Obtiene el nombre descriptivo del rol
     */
    fun getRoleName(role: UserRole): String {
        return when (role) {
            UserRole.OWNER -> "Propietario"
            UserRole.EMPLOYEE -> "Empleado"
            UserRole.ACCOUNTANT -> "Contador"
        }
    }
    
    /**
     * Obtiene una descripción de los permisos del rol
     */
    fun getRoleDescription(role: UserRole): String {
        return when (role) {
            UserRole.OWNER -> "Acceso total al sistema"
            UserRole.EMPLOYEE -> "Solo puede registrar ventas e ingresos"
            UserRole.ACCOUNTANT -> "Puede consultar reportes y exportar datos"
        }
    }
}

