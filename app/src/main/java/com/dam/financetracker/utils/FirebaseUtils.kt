package com.dam.financetracker.utils

object FirebaseUtils {

    fun getFirebaseErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "El formato del email es inválido"
            "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada"
            "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este email"
            "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con este email"
            "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Error de conexión. Verifica tu internet"
            "The email address is already in use by another account." -> "Ya existe una cuenta con este email"
            "The password is invalid or the user does not have a password." -> "Contraseña incorrecta"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "No existe una cuenta con este email"
            "The email address is badly formatted." -> "El formato del email es inválido"
            "Password should be at least 6 characters" -> "La contraseña debe tener al menos 6 caracteres"
            else -> "Error: ${errorCode.take(100)}"
        }
    }
}