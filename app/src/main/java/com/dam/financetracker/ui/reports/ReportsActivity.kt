package com.dam.financetracker.ui.reports

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityReportsBinding
import com.dam.financetracker.models.ReportPeriod
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.ui.settings.SettingsActivity
import com.dam.financetracker.ui.transaction.TransactionActivity
import com.dam.financetracker.ui.transaction.TransactionHistoryActivity
import com.dam.financetracker.utils.PdfGenerator
import com.dam.financetracker.utils.RoleManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportsViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // HU-007 y HU-008: Control de permisos
    private val authRepository = AuthRepository()
    private var currentUserRole: UserRole = UserRole.OWNER

    // Variables temporales para el Rango de Fechas Personalizado
    private var rangeStartDate: Long? = null
    private var rangeEndDate: Long? = null

    // Launcher para la solicitud de permisos de escritura (necesario antes de Android 13)
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, generar PDF
                generatePdf()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado. No se puede guardar el PDF.", Toast.LENGTH_LONG).show()
            }
        }

    // Launcher para la solicitud de permisos de escritura para CSV
    private val requestPermissionCsvLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, generar CSV
                generateCsv()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado. No se puede guardar el CSV.", Toast.LENGTH_LONG).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // HU-007 y HU-008: Obtener rol del usuario
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            currentUserRole = user?.role ?: UserRole.OWNER
            
            setupViews()
            setupChart()
            observeViewModel()
            setupBottomNavigation()
            applyRoleBasedExportPermissions()
            
            // Actualizar el menú después de obtener el rol
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reports, menu)
        return true
    }
    
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Solo mostrar el botón de logout para el rol ACCOUNTANT
        menu?.findItem(R.id.action_logout)?.isVisible = (currentUserRole == UserRole.ACCOUNTANT)
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Cerrar sesión
                authRepository.signOut()
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, com.dam.financetracker.ui.auth.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupViews() {
        binding.apply {
            setSupportActionBar(toolbar)
            toolbar.setNavigationOnClickListener { finish() }

            // Configurar filtros de período (1M, 3M, 1Y)
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

            // NUEVO: Listener para Exportar a PDF (HU-006.2)
            btnExportPdf.setOnClickListener { 
                // HU-007 y HU-008: Verificar permisos de exportación
                if (RoleManager.canExportReports(currentUserRole)) {
                    exportReportToPdf()
                } else {
                    Toast.makeText(this@ReportsActivity, "No tienes permisos para exportar reportes", Toast.LENGTH_SHORT).show()
                }
            }

            // NUEVO: Listener para Exportar a CSV
            btnExportCsv.setOnClickListener {
                // HU-007 y HU-008: Verificar permisos de exportación
                if (RoleManager.canExportReports(currentUserRole)) {
                    exportReportToCsv()
                } else {
                    Toast.makeText(this@ReportsActivity, "No tienes permisos para exportar reportes", Toast.LENGTH_SHORT).show()
                }
            }

            // NUEVO: Listener para selector de Rango de Fechas Personalizado
            btnSelectDateRange.setOnClickListener {
                // LLAMADA A LA LÓGICA DE SELECCIÓN DE RANGO DE DOS PASOS
                showDateRangePicker()
            }

            // NUEVO: Listener para Generar Reporte (para rangos personalizados)
            btnGenerateReport.setOnClickListener {
                Toast.makeText(this@ReportsActivity, "Generando reporte para período personalizado...", Toast.LENGTH_SHORT).show()
            }


            updatePeriodButtons(ReportPeriod.ONE_MONTH)
        }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
                labelRotationAngle = -45f
                labelCount = 5
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.GRAY
            }
            axisRight.isEnabled = false
            legend.textColor = Color.DKGRAY
            legend.textSize = 12f
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.reportData.collect { reportData -> updateMetricsCards(reportData) }
        }
        lifecycleScope.launch {
            viewModel.trendData.collect { trendData -> updateChart(trendData) }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * [CORREGIDO] Actualiza las métricas y aplica la lógica de color para Ingresos/Gastos.
     */
    private fun updateMetricsCards(reportData: com.dam.financetracker.models.ReportData) {
        binding.apply {
            // Card 1: Ingresos Totales (Positivo = Bueno, Negativo = Malo)
            tvIncomeAmount.text = currencyFormat.format(reportData.totalIncome)
            tvIncomePercentage.text = formatPercentage(reportData.incomePercentage)
            tvIncomePercentage.setTextColor(getPositiveColor(reportData.incomePercentage))

            // Card 2: Gastos Totales (Negativo = Bueno, Positivo = Malo)
            tvExpenseAmount.text = currencyFormat.format(reportData.totalExpense)
            tvExpensePercentage.text = formatPercentage(reportData.expensePercentage)
            tvExpensePercentage.setTextColor(getNegativeColor(reportData.expensePercentage)) // Usa la lógica inversa

            // Card 3: Balance (Positivo = Bueno, Negativo = Malo)
            tvBalanceAmount.text = currencyFormat.format(reportData.balance)
            tvBalancePercentage.text = formatPercentage(reportData.balancePercentage)
            tvBalancePercentage.setTextColor(getPositiveColor(reportData.balancePercentage))
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
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Dataset de gastos (verde)
        val expenseDataSet = LineDataSet(expenseEntries, "Gastos").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.success_color)
            setCircleColor(ContextCompat.getColor(this@ReportsActivity, R.color.success_color))
            lineWidth = 3f
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(incomeDataSet, expenseDataSet)

        binding.lineChart.apply {
            data = lineData
            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String { return labels.getOrNull(value.toInt()) ?: "" }
                }
                labelCount = labels.size.coerceAtMost(6)
            }
            animateX(1000)
            invalidate()
        }
    }

    /**
     * [CRÍTICO - REINCORPORADO] Lógica para actualizar el estilo de los botones de período.
     * Esta función causaba el error "Unresolved reference".
     */
    private fun updatePeriodButtons(selectedPeriod: ReportPeriod) {
        binding.apply {
            // Resetear todos los botones a estado normal
            btnOneMonth.apply {
                @Suppress("DEPRECATION")
                strokeWidth = 2
                @Suppress("DEPRECATION")
                strokeColor = getColorStateList(R.color.primary_purple)
                @Suppress("DEPRECATION")
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            btnThreeMonths.apply {
                @Suppress("DEPRECATION")
                strokeWidth = 2
                @Suppress("DEPRECATION")
                strokeColor = getColorStateList(R.color.primary_purple)
                @Suppress("DEPRECATION")
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            btnOneYear.apply {
                @Suppress("DEPRECATION")
                strokeWidth = 2
                @Suppress("DEPRECATION")
                strokeColor = getColorStateList(R.color.primary_purple)
                @Suppress("DEPRECATION")
                setTextColor(getColor(R.color.primary_purple))
                backgroundTintList = null
            }

            // Aplicar estilo al botón seleccionado
            when (selectedPeriod) {
                ReportPeriod.ONE_MONTH -> btnOneMonth.apply {
                    @Suppress("DEPRECATION")
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    @Suppress("DEPRECATION")
                    setTextColor(getColor(R.color.white))
                }
                ReportPeriod.THREE_MONTHS -> btnThreeMonths.apply {
                    @Suppress("DEPRECATION")
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    @Suppress("DEPRECATION")
                    setTextColor(getColor(R.color.white))
                }
                ReportPeriod.ONE_YEAR -> btnOneYear.apply {
                    @Suppress("DEPRECATION")
                    backgroundTintList = getColorStateList(R.color.primary_purple)
                    @Suppress("DEPRECATION")
                    setTextColor(getColor(R.color.white))
                }
            }
        }
    }

    /**
     * [NUEVA FUNCIÓN] Implementa un selector de rango de fechas encadenando dos DatePickers.
     */
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()

        // 1. Mostrar selector para FECHA DE INICIO
        val startDatePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            // Guarda la fecha de inicio
            rangeStartDate = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }.timeInMillis

            // 2. Mostrar selector para FECHA DE FIN inmediatamente después
            val endDatePicker = DatePickerDialog(this, { _, yearEnd, monthEnd, dayOfMonthEnd ->
                // Guarda la fecha de fin
                rangeEndDate = Calendar.getInstance().apply { set(yearEnd, monthEnd, dayOfMonthEnd, 23, 59, 59) }.timeInMillis

                // 3. Actualizar UI
                val startText = dateFormat.format(Date(rangeStartDate!!))
                val endText = dateFormat.format(Date(rangeEndDate!!))
                binding.btnSelectDateRange.text = "$startText - $endText"

                Toast.makeText(this, "Rango Seleccionado.", Toast.LENGTH_SHORT).show()

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            // Asegura que la fecha de fin sea posterior o igual a la de inicio
            rangeStartDate?.let { endDatePicker.datePicker.minDate = it }
            endDatePicker.setTitle("Seleccionar Fecha de Término")
            endDatePicker.show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        startDatePicker.setTitle("Seleccionar Fecha de Inicio")
        startDatePicker.show()
    }

    /**
     * Lógica principal para Exportar a PDF (HU-006.2)
     */
    private fun exportReportToPdf() {
        val reportData = viewModel.reportData.value
        val trendData = viewModel.trendData.value

        if (reportData.totalIncome == 0.0 && trendData.monthlyDataList.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            generatePdf()
        }
    }

    /**
     * Lógica principal para Exportar a CSV (Nueva función)
     */
    private fun exportReportToCsv() {
        val reportData = viewModel.reportData.value
        val trendData = viewModel.trendData.value

        if (reportData.totalIncome == 0.0 && trendData.monthlyDataList.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionCsvLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            generateCsv()
        }
    }

    /**
     * Función que ejecuta la generación real del PDF llamando a la clase utilitaria.
     */
    private fun generatePdf() {
        val nestedScrollView = binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.contentScrollView)
        if (nestedScrollView != null) {
            PdfGenerator.generatePdfFromView(
                this,
                nestedScrollView,
                "ReporteFinanciero_${viewModel.selectedPeriod.value.name}"
            )
        } else {
            Toast.makeText(this, "Error: No se pudo encontrar la vista de contenido para exportar.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Función que genera el contenido CSV y lo guarda, y luego intenta abrirlo.
     */
    private fun generateCsv() {
        lifecycleScope.launch {
            try {
                // LLAMADA AL MÉTODO PÚBLICO DEL VIEWMODEL
                val csvContent = viewModel.getCsvForCurrentPeriod()

                if (csvContent.isBlank()) {
                    Toast.makeText(this@ReportsActivity, "No se encontraron transacciones para exportar.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val fileName = "Transacciones_${viewModel.selectedPeriod.value.name}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"

                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }

                Toast.makeText(this@ReportsActivity, "CSV guardado exitosamente en: Downloads/$fileName", Toast.LENGTH_LONG).show()

                // SOLUCIÓN PARA ABRIR CSV: Lanzar Intent
                val fileUri = Uri.fromFile(file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "text/csv")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Error al guardar CSV: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    /**
     * [CORREGIDO - CRÍTICO] Limita el porcentaje a 100% (o -100%) para la visualización.
     */
    private fun formatPercentage(percentage: Double): String {
        val rawAbsValue = abs(percentage)

        // Limitar el valor mostrado a 100.0 si es mayor.
        val displayedValue = if (rawAbsValue > 100.0) 100.0 else rawAbsValue

        val sign = if (percentage >= 0) "↑" else "↓"

        // Usamos Locale.US para asegurar el punto decimal y evitar el uso de la coma.
        return String.format(Locale.US, "%s %.1f%%", sign, displayedValue)
    }

    // [CORREGIDO] Determina el color para un valor positivo (Ingresos/Balance).
    private fun getPositiveColor(percentage: Double): Int {
        return if (percentage >= 0) {
            ContextCompat.getColor(this, R.color.success_color)
        } else {
            ContextCompat.getColor(this, R.color.error_color)
        }
    }

    // [CORREGIDO] Determina el color para un valor negativo (Gastos).
    private fun getNegativeColor(percentage: Double): Int {
        return if (percentage <= 0) { // Menos gasto (negativo) es BUENO (verde)
            ContextCompat.getColor(this, R.color.success_color)
        } else { // Más gasto (positivo) es MALO (rojo)
            ContextCompat.getColor(this, R.color.error_color)
        }
    }


    private fun setupBottomNavigation() {
        // Acceder al BottomNavigationView a través del include
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)

        // HU-007 y HU-008: Ocultar opciones según el rol
        val menu = bottomNav?.menu
        menu?.findItem(R.id.nav_home)?.isVisible = RoleManager.canAccessDashboard(currentUserRole)
        menu?.findItem(R.id.nav_reports)?.isVisible = RoleManager.canAccessReports(currentUserRole)
        menu?.findItem(R.id.nav_settings)?.isVisible = RoleManager.canAccessSettings(currentUserRole)

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
                    // Si puede crear, abrir formulario; si no, abrir historial
                    if (RoleManager.canCreateTransactions(currentUserRole)) {
                        val intent = Intent(this, TransactionActivity::class.java)
                        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.INCOME.name)
                        startActivity(intent)
                    } else {
                        startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    }
                    finish()
                    true
                }
                R.id.nav_reports -> {
                    // Ya estamos en Reportes
                    true
                }
                R.id.nav_settings -> {
                    if (RoleManager.canAccessSettings(currentUserRole)) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    }
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
    
    /**
     * HU-007 y HU-008: Aplica permisos de exportación según el rol
     * EMPLOYEE (Empleado): No puede acceder a reportes (esta pantalla no debería abrirse)
     * ACCOUNTANT (Contador): SÍ puede exportar (PDF y CSV)
     * OWNER (Propietario): SÍ puede exportar
     */
    private fun applyRoleBasedExportPermissions() {
        val canExport = RoleManager.canExportReports(currentUserRole)
        
        binding.apply {
            // HU-007: Ocultar botones de exportación para empleados
            btnExportPdf.visibility = if (canExport) View.VISIBLE else View.GONE
            btnExportCsv.visibility = if (canExport) View.VISIBLE else View.GONE
            
            // Mostrar mensaje si es empleado (no debería estar aquí)
            if (currentUserRole == UserRole.EMPLOYEE) {
                Toast.makeText(
                    this@ReportsActivity,
                    "Como empleado no tienes acceso a reportes",
                    Toast.LENGTH_LONG
                ).show()
                finish() // Cerrar la actividad
            }
        }
    }
}
