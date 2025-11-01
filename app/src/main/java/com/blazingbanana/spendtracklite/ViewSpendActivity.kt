package com.blazingbanana.spendtracklite

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.blazingbanana.spendtracklite.data.Expense
import com.blazingbanana.spendtracklite.data.SpendTrackDatabase
import com.blazingbanana.spendtracklite.databinding.ActivityViewSpendBinding
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ViewSpendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewSpendBinding
    private val expenseDao by lazy { SpendTrackDatabase.getInstance(this).expenseDao() }
    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.UK) }
    private val exportPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                performExport()
            } else {
                Toast.makeText(
                    this,
                    R.string.toast_export_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val adapter by lazy { ExpenseAdapter(currencyFormatter) }
    private var latestExpenses: List<Expense> = emptyList()
    private var selectedDays: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewSpendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerExpenses.layoutManager = LinearLayoutManager(this)
        binding.recyclerExpenses.adapter = adapter

        binding.buttonExport.setOnClickListener { exportExpenses() }

        binding.sliderDays.value = selectedDays.toFloat()
        binding.sliderDays.setLabelFormatter { value ->
            val days = value.roundToInt().coerceIn(1, 31)
            resources.getQuantityString(R.plurals.view_total_range, days, days)
        }
        binding.sliderDays.addOnChangeListener { _, value, _ ->
            val newDays = value.roundToInt().coerceIn(1, 31)
            if (newDays != selectedDays) {
                selectedDays = newDays
                updateTotalSection()
            }
        }
        binding.textSliderHint.text = getString(R.string.view_slider_hint, 31)
        updateTotalSection()

        observeExpenses()
    }

    private fun observeExpenses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                expenseDao.observeAllExpenses().collect { expenses ->
                    latestExpenses = expenses
                    adapter.submitList(expenses)
                    binding.textEmpty.isVisible = expenses.isEmpty()
                    updateTotalSection()
                }
            }
        }
    }

    private fun updateTotalSection() {
        val days = selectedDays
        binding.textTotalRange.text =
            resources.getQuantityString(R.plurals.view_total_range, days, days)
        val total = calculateTotalForDays(days)
        binding.textTotal.text = getString(
            R.string.view_total_format,
            currencyFormatter.format(total)
        )
    }

    private fun calculateTotalForDays(days: Int): Double {
        if (days <= 0 || latestExpenses.isEmpty()) return 0.0
        val zone = ZoneId.systemDefault()
        val startOfRange = ZonedDateTime.now(zone)
            .minusDays((days - 1).toLong())
            .toLocalDate()
            .atStartOfDay(zone)
        val startMillis = startOfRange.toInstant().toEpochMilli()
        val endMillis = System.currentTimeMillis()
        return latestExpenses.asSequence()
            .filter { it.timestamp in startMillis..endMillis }
            .sumOf { it.amount }
    }

    private fun exportExpenses() {
        if (latestExpenses.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_expenses_to_export, Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            performExport()
        }
    }

    private fun performExport() {
        val expensesSnapshot = latestExpenses
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                CsvExporter.exportExpenses(this@ViewSpendActivity, expensesSnapshot)
            }
            val messageRes = if (success) {
                R.string.toast_export_success
            } else {
                R.string.toast_export_failed
            }
            Toast.makeText(this@ViewSpendActivity, messageRes, Toast.LENGTH_SHORT).show()
        }
    }
}
