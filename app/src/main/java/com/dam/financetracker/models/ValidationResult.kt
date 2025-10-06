package com.dam.financetracker.models

data class ValidationResult (
    val isValid: Boolean,
    val errorMessage: String = ""
)
