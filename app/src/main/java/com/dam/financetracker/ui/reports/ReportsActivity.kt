package com.dam.financetracker.ui.reports

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityReportsBinding
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.ui.settings.SettingsActivity
import com.dam.financetracker.ui.transaction.TransactionActivity
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.models.ReportPeriod
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportsViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupChart()
        observeViewModel()
        setupBottomNavigation()
    }

    private fun setupViews() {
        binding.apply {
            // Configurar toolbar
            toolbar.setNavigationOnClickListener {
                finish()
            }

            // Configurar filtros de período
            btnOneMonth.setOnClickListener {
                viewModel.changePeriod(ReportPeriod.ONE_MONTH)
                updatePeriodButtons(ReportPeriod.ONE_MONTH)
            }

            btnThreeMonths.setOnClickListener {
                viewModel.changePeriod(ReportPeriod.THREE_MONTHS)
                updatePeriodButtons(ReportPeriod.THREE_MONTHS)
            }

            btnOneYear.setOnClickListener {
                viewModel.changePeriod(ReportPeriod.ONE_YEAR)
                updatePeriodButtons(ReportPeriod.ONE_YEAR)
            }

            // Inicializar botón 1M como seleccionado
            updatePeriodButtons(ReportPeriod.ONE_MONTH)
        }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)  // Cambiar a true para poder hacer zoom
            setPinchZoom(true)     // Cambiar a true
            setDrawGridBackground(false)

            // Configurar eje X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
                labelRotationAngle = -45f  // NUEVO: Rotar etiquetas para que quepan
                labelCount = 5              // NUEVO: Máximo 5 etiquetas visibles
            }

            // Configurar eje Y izquierdo
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.GRAY
            }

            // Deshabilitar eje Y derecho
            axisRight.isEnabled = false

            // Configurar leyenda
            legend.textColor = Color.DKGRAY
            legend.textSize = 12f
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // Observar datos del reporte
            viewModel.reportData.collect { reportData ->
                updateMetricsCards(reportData)
            }
        }

        lifecycleScope.launch {
            // Observar datos del gráfico
            viewModel.trendData.collect { trendData ->
                updateChart(trendData)
            }
        }

        lifecycleScope.launch {
            // Observar estado de carga
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateMetricsCards(reportData: com.dam.financetracker.models.ReportData) {
        binding.apply {
            // Card de Ingresos Totales
            tvIncomeAmount.text = currencyFormat.format(reportData.totalIncome)
            tvIncomePercentage.text = formatPercentage(reportData.incomePercentage)
            tvIncomePercentage.setTextColor(getPercentageColor(reportData.incomePercentage))

            // Card de Gastos Totales
            tvExpenseAmount.text = currencyFormat.format(reportData.totalExpense)
            tvExpensePercentage.text = formatPercentage(reportData.expensePercentage)
            tvExpensePercentage.setTextColor(getPercentageColor(-reportData.expensePercentage)) // Negativo porque menos gasto es mejor

            // Card de Balance
            tvBalanceAmount.text = currencyFormat.format(reportData.balance)
            tvBalancePercentage.text = formatPercentage(reportData.balancePercentage)
            tvBalancePercentage.setTextColor(getPercentageColor(reportData.balancePercentage))
        }
    }

    private fun updateChart(trendData: com.dam.financetracker.models.TrendData) {
        if (trendData.monthlyDataList.isEmpty()) {
            binding.lineChart.clear()
            return
        }

        val incomeEntries = mutableListOf<Entry>()
        val expenseEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        trendData.monthlyDataList.forEachIndexed { index, monthlyData ->
            incomeEntries.add(Entry(index.toFloat(), monthlyData.income))
            expenseEntries.add(Entry(index.toFloat(), monthlyData.expense))
            labels.add(monthlyData.month)
        }

        // Dataset de ingresos (morado)
        val incomeDataSet = LineDataSet(incomeEntries, "Ingresos").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.primary_purple)
            setCircleColor(ContextCompat.getColor(this@ReportsActivity, R.color.primary_purple))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Dataset de gastos (verde)
        val expenseDataSet = LineDataSet(expenseEntries, "Gastos").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.success_color)
            setCircleColor(ContextCompat.getColor(this@ReportsActivity, R.color.success_color))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(incomeDataSet, expenseDataSet)

        binding.lineChart.apply {
            data = lineData

            // Configurar etiquetas del eje X - MEJORADO
            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return labels.getOrNull(value.toInt()) ?: ""
                    }
                }
                labelCount = labels.size.coerceAtMost(6)  // Máximo 6 etiquetas
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                textSize = 10f
            }

            animateX(1000)
            invalidate()
        }
    }

    private fun updatePeriodButtons(selectedPeriod: ReportPeriod) {
        binding.apply {
            // Resetear todos los botones a estado normal
            btnOneMonth.apply {
                strokeWidth = 2
                strokeColor = getColorStateList(R.color.primary_purple)
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            btnThreeMonths.apply {
                strokeWidth = 2
                strokeColor = getColorStateList(R.color.primary_purple)
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            btnOneYear.apply {
                strokeWidth = 2
                strokeColor = getColorStateList(R.color.primary_purple)
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            // Aplicar estilo al botón seleccionado
            when (selectedPeriod) {
                ReportPeriod.ONE_MONTH -> btnOneMonth.apply {
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    setTextColor(getColor(R.color.white))
                }
                ReportPeriod.THREE_MONTHS -> btnThreeMonths.apply {
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    setTextColor(getColor(R.color.white))
                }
                ReportPeriod.ONE_YEAR -> btnOneYear.apply {
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    setTextColor(getColor(R.color.white))
                }
            }
        }
    }

    private fun formatPercentage(percentage: Double): String {
        val sign = if (percentage >= 0) "↑" else "↓"
        return "$sign ${String.format("%.1f", kotlin.math.abs(percentage))}%"
    }

    private fun getPercentageColor(percentage: Double): Int {
        return if (percentage >= 0) {
            ContextCompat.getColor(this, R.color.success_color)
        } else {
            ContextCompat.getColor(this, R.color.error_color)
        }
    }

    private fun setupBottomNavigation() {
        // Acceder al BottomNavigationView a través del include
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)

        // Marcar el ítem de Reportes como seleccionado
        bottomNav?.selectedItemId = R.id.nav_reports

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    val intent = Intent(this, TransactionActivity::class.java)
                    intent.putExtra(TransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.INCOME.name)
                    startActivity(intent)
                    true
                }
                R.id.nav_reports -> {
                    // Ya estamos en Reportes
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Asegurar que Reportes quede seleccionado al volver
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_reports
    }
}