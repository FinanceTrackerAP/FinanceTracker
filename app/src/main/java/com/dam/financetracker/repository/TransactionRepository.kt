package com.dam.financetracker.repository

import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class TransactionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val BUSINESSES_COLLECTION = "businesses"
    }

    suspend fun createTransaction(transaction: Transaction): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))

            // Generar ID único para la transacción
            val transactionId = firestore.collection(TRANSACTIONS_COLLECTION).document().id

            val transactionData = transaction.copy(
                id = transactionId,
                userId = userId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .set(transactionData)
                .await()

            Result.success(transactionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar que el usuario puede editar esta transacción
            if (transaction.userId != userId) {
                return Result.failure(Exception("No tienes permisos para editar esta transacción"))
            }

            val updatedTransaction = transaction.copy(
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(TRANSACTIONS_COLLECTION)
                .document(transaction.id)
                .set(updatedTransaction)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar que la transacción existe y pertenece al usuario
            val transaction = getTransactionById(transactionId)
            if (transaction.userId != userId) {
                return Result.failure(Exception("No tienes permisos para eliminar esta transacción"))
            }

            // Soft delete - marcar como inactiva
            firestore.collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionById(transactionId: String): Transaction {
        return try {
            val document = firestore.collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .get()
                .await()

            document.toObject(Transaction::class.java) ?: Transaction()
        } catch (e: Exception) {
            Transaction()
        }
    }

    fun getTransactionsByBusiness(businessId: String): Flow<List<Transaction>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("isActive", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)
            }

            emit(transactions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getTransactionsByUser(userId: String): Flow<List<Transaction>> = flow {
        try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            // Solo permitir ver transacciones propias
            if (currentUserId != userId) {
                emit(emptyList())
                return@flow
            }

            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)
            }

            emit(transactions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getTransactionsByType(businessId: String, type: TransactionType): Flow<List<Transaction>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("type", type)
                .whereEqualTo("isActive", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)
            }

            emit(transactions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getBalanceByBusiness(businessId: String): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            var balance = 0.0

            snapshot.documents.forEach { document ->
                val transaction = document.toObject(Transaction::class.java)
                transaction?.let {
                    when (it.type) {
                        TransactionType.INCOME -> balance += it.amount
                        TransactionType.EXPENSE -> balance -= it.amount
                    }
                }
            }

            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByBusinessOnce(businessId: String): List<Transaction> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("businessId", businessId)
                .get()
                .await()

            // Filtrar y ordenar en el código para evitar necesitar índices compuestos
            snapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)
            }.filter { it.isActive }
                .sortedByDescending { it.date }
        } catch (e: Exception) {
            println("ERROR getTransactionsByBusinessOnce: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}
