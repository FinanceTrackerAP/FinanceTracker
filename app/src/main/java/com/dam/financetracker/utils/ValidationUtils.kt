package com.dam.financetracker.utils

import android.util.Patterns
import com.dam.financetracker.models.ValidationResult

object ValidationUtils {

    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "El email es requerido")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult(false, "Formato de email inválido")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "La contraseña es requerida")
            password.length < 6 -> ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
            else -> ValidationResult(true)
        }
    }

    fun validateBusinessName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "El nombre del negocio es requerido")
            name.length < 2 -> ValidationResult(false, "El nombre debe tener al menos 2 caracteres")
            else -> ValidationResult(true)
        }
    }

    fun validateRUC(ruc: String): ValidationResult {
        return when {
            ruc.isBlank() -> ValidationResult(false, "El RUC es requerido")
            ruc.length != 11 -> ValidationResult(false, "El RUC debe tener 11 dígitos")
            !ruc.all { it.isDigit() } -> ValidationResult(false, "El RUC debe contener solo números")
            else -> ValidationResult(true)
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult(false, "El teléfono es requerido")
            phone.length < 9 -> ValidationResult(false, "El teléfono debe tener al menos 9 dígitos")
            !phone.all { it.isDigit() } -> ValidationResult(false, "El teléfono debe contener solo números")
            else -> ValidationResult(true)
        }
    }

    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Confirma tu contraseña")
            password != confirmPassword -> ValidationResult(false, "Las contraseñas no coinciden")
            else -> ValidationResult(true)
        }
    }

    // Validaciones para transacciones
    fun validateAmount(amount: String): ValidationResult {
        return when {
            amount.isBlank() -> ValidationResult(false, "El monto es requerido")
            amount.toDoubleOrNull() == null -> ValidationResult(false, "Ingresa un monto válido")
            amount.toDouble() <= 0 -> ValidationResult(false, "El monto debe ser mayor a 0")
            amount.toDouble() > 999999999.99 -> ValidationResult(false, "El monto es demasiado grande")
            else -> ValidationResult(true)
        }
    }

    fun validateTransactionDescription(description: String): ValidationResult {
        return when {
            description.isBlank() -> ValidationResult(false, "La descripción es requerida")
            description.length < 3 -> ValidationResult(false, "La descripción debe tener al menos 3 caracteres")
            description.length > 200 -> ValidationResult(false, "La descripción es demasiado larga (máximo 200 caracteres)")
            else -> ValidationResult(true)
        }
    }

    fun validateCategory(category: String): ValidationResult {
        return when {
            category.isBlank() -> ValidationResult(false, "La categoría es requerida")
            category.length < 2 -> ValidationResult(false, "La categoría debe tener al menos 2 caracteres")
            else -> ValidationResult(true)
        }
    }
}