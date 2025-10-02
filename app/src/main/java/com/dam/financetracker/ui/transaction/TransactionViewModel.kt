package com.dam.financetracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.models.ValidationResult
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.repository.TransactionRepository
import com.dam.financetracker.utils.ValidationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel : ViewModel() {
    
    private val transactionRepository = TransactionRepository()
    private val authRepository = AuthRepository()
    
    // Estados del formulario
    private val _transactionType = MutableStateFlow(TransactionType.INCOME)
    val transactionType: StateFlow<TransactionType> = _transactionType.asStateFlow()
    
    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    
    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()
    
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()
    
    // Estados de validación
    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError.asStateFlow()
    
    private val _descriptionError = MutableStateFlow<String?>(null)
    val descriptionError: StateFlow<String?> = _descriptionError.asStateFlow()
    
    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError: StateFlow<String?> = _categoryError.asStateFlow()
    
    // Estados de UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _operationResult = MutableSharedFlow<Result<String>>()
    val operationResult: SharedFlow<Result<String>> = _operationResult.asSharedFlow()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()
    
    // Transacción en edición
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    // Job para manejar la suscripción a transacciones
    private var transactionsJob: kotlinx.coroutines.Job? = null
    
    init {
        // Cargar transacciones de forma segura
        safeLoadTransactions()
    }
    
    fun setTransactionType(type: TransactionType) {
        _transactionType.value = type
        clearErrors()
    }
    
    fun setAmount(amount: String) {
        _amount.value = amount
        validateAmount()
    }
    
    fun setDescription(description: String) {
        _description.value = description
        validateDescription()
    }
    
    fun setCategory(category: String) {
        _category.value = category
        validateCategory()
    }
    
    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }
    
    fun loadTransactionForEdit(transaction: Transaction) {
        _editingTransaction.value = transaction
        _isEditMode.value = true
        
        // Cargar datos en el formulario
        _transactionType.value = transaction.type
        _amount.value = transaction.amount.toString()
        _description.value = transaction.description
        _category.value = transaction.category
        _selectedDate.value = transaction.date
        
        clearErrors()
    }
    
    fun clearEditMode() {
        _editingTransaction.value = null
        _isEditMode.value = false
        // No llamar clearForm() aquí para evitar ciclo infinito
    }
    
    fun saveTransaction() {
        if (!validateForm()) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    _operationResult.emit(Result.failure(Exception("Usuario no autenticado")))
                    _isLoading.value = false
                    return@launch
                }
                
                val amountDouble = _amount.value.toDoubleOrNull() ?: 0.0
                
                if (_isEditMode.value) {
                    // Actualizar transacción existente
                    val updatedTransaction = _editingTransaction.value?.copy(
                        type = _transactionType.value,
                        amount = amountDouble,
                        description = _description.value.trim(),
                        category = _category.value.trim(),
                        date = _selectedDate.value
                    )
                    
                    if (updatedTransaction != null) {
                        val result = transactionRepository.updateTransaction(updatedTransaction)
                        if (result.isSuccess) {
                            _operationResult.emit(Result.success("Transacción actualizada correctamente"))
                            clearForm()
                            safeLoadTransactions()
                        } else {
                            val errorMessage = result.exceptionOrNull()?.message ?: "Error al actualizar"
                            _operationResult.emit(Result.failure(Exception(errorMessage)))
                        }
                    }
                } else {
                    // Crear nueva transacción
                    // Si el usuario no tiene businessId, usar su uid como businessId por defecto
                    val businessId = if (user.businessId.isNotEmpty()) {
                        user.businessId
                    } else {
                        user.uid
                    }
                    
                    println("DEBUG: Creando transacción - UserId: ${user.uid}, BusinessId: $businessId")
                    
                    val transaction = Transaction(
                        businessId = businessId,
                        type = _transactionType.value,
                        amount = amountDouble,
                        description = _description.value.trim(),
                        category = _category.value.trim(),
                        date = _selectedDate.value
                    )
                    
                    println("DEBUG: Transaction creada: $transaction")
                    
                    val result = transactionRepository.createTransaction(transaction)
                    
                    println("DEBUG: Resultado de createTransaction: ${result.isSuccess}")
                    if (result.isSuccess) {
                        println("DEBUG: ✅ Transacción guardada exitosamente")
                        _operationResult.emit(Result.success("Transacción registrada correctamente"))
                        clearForm()
                        safeLoadTransactions()
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Error al guardar"
                        println("DEBUG: ❌ Error al guardar: $errorMessage")
                        _operationResult.emit(Result.failure(Exception(errorMessage)))
                    }
                }
                
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido"
                println("DEBUG: ❌ Exception en saveTransaction: $errorMessage")
                e.printStackTrace()
                _operationResult.emit(Result.failure(Exception(errorMessage)))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = transactionRepository.deleteTransaction(transactionId)
                if (result.isSuccess) {
                    _operationResult.emit(Result.success("Transacción eliminada correctamente"))
                    safeLoadTransactions()
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Error al eliminar"
                    _operationResult.emit(Result.failure(Exception(errorMessage)))
                }
                
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido"
                _operationResult.emit(Result.failure(Exception(errorMessage)))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadTransactions() {
        // Método obsoleto - usar safeLoadTransactions() en su lugar
    }
    
    private fun safeLoadTransactions() {
        // Cancelar job anterior si existe
        transactionsJob?.cancel()
        
        transactionsJob = viewModelScope.launch {
            try {
                println("DEBUG Dashboard: Cargando transacciones...")
                val user = authRepository.getCurrentUser()
                println("DEBUG Dashboard: Usuario obtenido: ${user?.email}, businessId: ${user?.businessId}")
                
                if (user != null) {
                    // Si el usuario no tiene businessId, usar su uid como businessId por defecto
                    val businessId = if (user.businessId.isNotEmpty()) {
                        user.businessId
                    } else {
                        user.uid
                    }
                    
                    println("DEBUG Dashboard: BusinessId a usar: $businessId")
                    
                    // Cargar una sola vez sin collect para evitar crashes
                    val transactions = transactionRepository.getTransactionsByBusinessOnce(businessId)
                    println("DEBUG Dashboard: Transacciones cargadas: ${transactions.size}")
                    transactions.forEach { 
                        println("DEBUG Dashboard: - ${it.description}: ${it.amount} (${it.type})")
                    }
                    
                    _transactions.value = transactions
                    
                    // Calcular balance directamente de las transacciones cargadas
                    var balance = 0.0
                    transactions.forEach { transaction ->
                        when (transaction.type) {
                            TransactionType.INCOME -> balance += transaction.amount
                            TransactionType.EXPENSE -> balance -= transaction.amount
                        }
                    }
                    _balance.value = balance
                    
                    println("DEBUG Dashboard: Balance calculado: ${_balance.value}")
                } else {
                    println("DEBUG Dashboard: Usuario es null")
                }
            } catch (e: Exception) {
                println("DEBUG Dashboard: Error al cargar transacciones: ${e.message}")
                e.printStackTrace()
                // Handle error silently
                _transactions.value = emptyList()
                _balance.value = 0.0
            }
        }
    }
    
    fun refreshTransactions() {
        safeLoadTransactions()
    }
    
    override fun onCleared() {
        super.onCleared()
        transactionsJob?.cancel()
    }
    
    private suspend fun calculateBalance(businessId: String) {
        try {
            val result = transactionRepository.getBalanceByBusiness(businessId)
            if (result.isSuccess) {
                _balance.value = result.getOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            _balance.value = 0.0
        }
    }
    
    private fun validateForm(): Boolean {
        val isAmountValid = validateAmount()
        val isDescriptionValid = validateDescription()
        val isCategoryValid = validateCategory()
        
        return isAmountValid && isDescriptionValid && isCategoryValid
    }
    
    private fun validateAmount(): Boolean {
        val validation = ValidationUtils.validateAmount(_amount.value)
        _amountError.value = if (validation.isValid) null else validation.errorMessage
        return validation.isValid
    }
    
    private fun validateDescription(): Boolean {
        val validation = ValidationUtils.validateTransactionDescription(_description.value)
        _descriptionError.value = if (validation.isValid) null else validation.errorMessage
        return validation.isValid
    }
    
    private fun validateCategory(): Boolean {
        val validation = ValidationUtils.validateCategory(_category.value)
        _categoryError.value = if (validation.isValid) null else validation.errorMessage
        return validation.isValid
    }
    
    private fun clearErrors() {
        _amountError.value = null
        _descriptionError.value = null
        _categoryError.value = null
    }
    
    private fun clearForm() {
        _amount.value = ""
        _description.value = ""
        _category.value = ""
        _selectedDate.value = System.currentTimeMillis()
        clearErrors()
        // Limpiar modo de edición sin llamar clearForm() de nuevo
        _editingTransaction.value = null
        _isEditMode.value = false
    }
}

