package com.dam.financetracker.models

data class Business(
    val id: String = "",
    val name: String = "",
    val businessType: String = "",
    val ruc: String = "",
    val address: String = "",
    val phone: String = "",
    val ownerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
