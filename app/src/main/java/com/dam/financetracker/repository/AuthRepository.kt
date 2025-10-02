package com.dam.financetracker.repository

import com.dam.financetracker.models.AuthResult
import com.dam.financetracker.models.Business
import com.dam.financetracker.models.User
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Registro de usuario y negocio (Escenario 1)
    suspend fun registerUserWithBusiness(
        email: String,
        password: String,
        businessData: Business
    ): AuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario")

            val businessId = firestore.collection("businesses").document().id
            val business = businessData.copy(id = businessId, ownerId = firebaseUser.uid)

            firestore.collection("businesses")
                .document(businessId)
                .set(business)
                .await()

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                businessId = businessId,
                role = UserRole.OWNER
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            firebaseUser.sendEmailVerification().await()


            AuthResult.Success(user)

        } catch (e: Exception) {
            AuthResult.Error(FirebaseUtils.getFirebaseErrorMessage(e.message ?: "Error desconocido"))
        }
    }

    // Login (Escenario 2)
    suspend fun loginUser(email: String, password: String): AuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error en autenticación")

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = try {
                userDoc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
            } catch (e: Exception) {
                // Si hay error en el mapeo, crear usuario con datos básicos
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    businessId = userDoc.getString("businessId") ?: "",
                    role = UserRole.OWNER,
                    isActive = true
                )
            }

            AuthResult.Success(user)

        } catch (e: Exception) {
            AuthResult.Error(FirebaseUtils.getFirebaseErrorMessage(e.message ?: "Error desconocido"))
        }
    }

    // Recuperar contraseña (Escenario 3)
    suspend fun resetPassword(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success(User())
        } catch (e: Exception) {
            AuthResult.Error(FirebaseUtils.getFirebaseErrorMessage(e.message ?: "Error desconocido"))
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null

        return try {
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            try {
                userDoc.toObject(User::class.java)
            } catch (e: Exception) {
                // Si hay error en el mapeo, crear usuario con datos básicos
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    businessId = userDoc.getString("businessId") ?: "",
                    role = UserRole.OWNER,
                    isActive = true
                )
            }
        } catch (e: Exception) {
            // Si hay error al obtener el documento, crear usuario básico con lo que tenemos
            User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                businessId = "",
                role = UserRole.OWNER,
                isActive = true
            )
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}