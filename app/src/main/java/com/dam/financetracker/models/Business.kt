package com.dam.financetracker.models

import com.google.firebase.firestore.PropertyName

data class Business(
    val id: String = "",
    val name: String = "",
    val businessType: String = "",
    val ruc: String = "",
    val address: String = "",
    val phone: String = "",
    val ownerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @field:PropertyName("active")
    val isActive: Boolean = true
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", "", "", "", "", "", System.currentTimeMillis(), true)
}
