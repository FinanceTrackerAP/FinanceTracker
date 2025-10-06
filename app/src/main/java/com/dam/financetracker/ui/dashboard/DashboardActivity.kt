package com.dam.financetracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam.financetracker.databinding.ActivityDashboardBinding
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.settings.SettingsActivity   // <-- NUEVO
import com.dam.financetracker.ui.transaction.TransactionActivity
import com.dam.financetracker.ui.transaction.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authRepository = AuthRepository()

    // Usar TransactionViewModel para conectar con
    private val transactionViewModel: TransactionViewModel by viewModels()

    private lateinit var transactionAdapter: TransactionAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // En modo local, configurar un email de demo si no hay uno guardado
        setupDemoUserIfNeeded()

        setupRecyclerView()
        loadUserInfo()
        setupViews()
        setupObservers()

        // Cargar transacciones al iniciar
        transactionViewModel.refreshTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            openTransactionForEdit(transaction)
        }

        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    binding.tvWelcome.text = "Bienvenido"
                    binding.tvEmail.text = user.email
                    // Guardar el email en SharedPreferences para uso futuro
                    saveUserEmail(user.email)
                } else {
                    // Mostrar email por defecto
                    binding.tvWelcome.text = "Bienvenido"
                    val savedEmail = getSavedUserEmail()
                    binding.tvEmail.text = savedEmail
                }
            } catch (e: Exception) {
                // En caso de error, mostrar el email guardado o uno por defecto
                binding.tvWelcome.text = "Bienvenido"
                val savedEmail = getSavedUserEmail()
                binding.tvEmail.text = savedEmail
            }
        }
    }

    private fun saveUserEmail(email: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("user_email", email)
            .apply()
    }

    private fun getSavedUserEmail(): String {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("user_email", "demo@financetracker.com") ?: "demo@financetracker.com"
    }

    private fun setupDemoUserIfNeeded() {
        // Si no hay email guardado, configurar uno de demo para testing local
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.contains("user_email")) {
            saveUserEmail("demo@financetracker.com")
        }
    }

    private fun setupViews() {
        // NUEVO: botón de configuración en el header (ImageButton)
        binding.btnSettings.setOnClickListener {
            // Navega a la pantalla de Configuración / Perfil
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnAddIncome.setOnClickListener {
            openTransactionActivity(TransactionType.INCOME)
        }

        binding.btnAddExpense.setOnClickListener {
            openTransactionActivity(TransactionType.EXPENSE)
        }

        binding.btnAddFirstTransaction.setOnClickListener {
            openTransactionActivity(TransactionType.INCOME)
        }

        binding.tvViewAll.setOnClickListener {
            // TODO: Abrir actividad con todas las transacciones
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            transactionViewModel.transactions.collect { transactions ->
                // Mostrar solo las últimas 5 en el dashboard
                transactionAdapter.submitList(transactions.take(5))

                if (transactions.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvTransactions.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvTransactions.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            transactionViewModel.balance.collect { balance ->
                binding.tvBalance.text = currencyFormat.format(balance)
            }
        }
    }

    private fun openTransactionActivity(type: TransactionType) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION_TYPE, type.name)
        startActivity(intent)
    }

    private fun openTransactionForEdit(transaction: Transaction) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recargar transacciones cuando se vuelve a esta actividad
        transactionViewModel.refreshTransactions()
    }

    // Opcional: si decides cerrar sesión desde SettingsActivity y vuelves aquí,
    // puedes llamar a este helper para redirigir al login.
    private fun goToLoginAndFinish() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}