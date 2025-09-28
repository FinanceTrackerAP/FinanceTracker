package com.dam.financetracker.ui.auth

import androidx.lifecycle.ViewModel
import com.dam.financetracker.models.AuthResult
import com.dam.financetracker.models.Business
import com.dam.financetracker.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // CAMBIAR ESTA LÍNEA:
    // De: AuthResult.Loading
    // A: AuthResult.Error("") - estado neutro
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Error(""))
    val authResult: StateFlow<AuthResult> = _authResult

    suspend fun loginUser(email: String, password: String) {
        _authResult.value = AuthResult.Loading
        val result = authRepository.loginUser(email, password)
        _authResult.value = result
    }

    suspend fun registerUser(email: String, password: String, businessData: Business) {
        _authResult.value = AuthResult.Loading
        val result = authRepository.registerUserWithBusiness(email, password, businessData)
        _authResult.value = result
    }

    suspend fun resetPassword(email: String) {
        val result = authRepository.resetPassword(email)
        // No cambiar el estado principal para reset password
    }
}