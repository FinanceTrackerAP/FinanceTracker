package com.dam.financetracker.repository

import com.dam.financetracker.models.DefaultExpenseCategories
import com.dam.financetracker.models.DefaultIncomeCategories
import com.dam.financetracker.models.TransactionCategory
import com.dam.financetracker.models.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val CATEGORIES_COLLECTION = "categories"

    // Obtiene categorías personalizadas del usuario
    private suspend fun getCustomCategories(): List<TransactionCategory> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore.collection(CATEGORIES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(TransactionCategory::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Crea una nueva categoría personalizada (HU-003: 1)
    suspend fun createCategory(category: TransactionCategory): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        val categoryId = firestore.collection(CATEGORIES_COLLECTION).document().id

        val newCategory = category.copy(
            id = categoryId,
            userId = userId,
            isDefault = false,
            // La lógica de subir la imagen se implementaría aquí. Por ahora, solo es el registro de texto.
        )

        return try {
            firestore.collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .set(newCategory)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Combina categorías por defecto y personalizadas (HU-003: 2)
    suspend fun getAllCategories(type: TransactionType): List<TransactionCategory> {
        val defaultCategories = when (type) {
            TransactionType.INCOME -> DefaultIncomeCategories.categories
            TransactionType.EXPENSE -> DefaultExpenseCategories.categories
        }

        val customCategories = getCustomCategories().filter { it.type == type }

        return defaultCategories + customCategories
    }
}