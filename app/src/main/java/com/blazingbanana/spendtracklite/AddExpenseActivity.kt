package com.blazingbanana.spendtracklite

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.blazingbanana.spendtracklite.data.Expense
import com.blazingbanana.spendtracklite.data.SpendTrackDatabase
import com.blazingbanana.spendtracklite.databinding.ActivityAddExpenseBinding
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private val expenseDao by lazy { SpendTrackDatabase.getInstance(this).expenseDao() }
    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.UK) }
    private val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.UK)
    }
    private var selectedTimestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSave.setOnClickListener { saveExpense() }

        binding.inputTimestamp.setOnClickListener { openDateTimePicker() }

        updateTimestampField(selectedTimestamp)

        binding.inputAmount.doAfterTextChanged {
            binding.layoutAmount.error = null
        }
        binding.inputDescription.doAfterTextChanged {
            binding.layoutDescription.error = null
        }
    }

    private fun saveExpense() {
        binding.layoutAmount.error = null
        binding.layoutDescription.error = null

        val amountText = binding.inputAmount.text?.toString()?.trim().orEmpty()
        val amount = amountText.replace(',', '.').toDoubleOrNull()
        val description = binding.inputDescription.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (amount == null || amount <= 0.0) {
            binding.layoutAmount.error = getString(R.string.error_invalid_amount)
            hasError = true
        }

        if (description.isEmpty()) {
            binding.layoutDescription.error = getString(R.string.error_empty_description)
            hasError = true
        }

        if (hasError) {
            return
        }

        val expense = Expense(
            amount = amount!!,
            description = description,
            timestamp = selectedTimestamp
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                expenseDao.insert(expense)
            }
            Toast.makeText(
                this@AddExpenseActivity,
                getString(R.string.toast_expense_saved, currencyFormatter.format(amount)),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun updateTimestampField(epochMillis: Long) {
        val localDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault()
        )
        binding.inputTimestamp.setText(dateTimeFormatter.format(localDateTime))
    }

    private fun openDateTimePicker() {
        val currentDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(selectedTimestamp),
            ZoneId.systemDefault()
        )
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDateTime.of(
                    year,
                    month + 1,
                    dayOfMonth,
                    currentDateTime.hour,
                    currentDateTime.minute
                )
                openTimePicker(pickedDate)
            },
            currentDateTime.year,
            currentDateTime.monthValue - 1,
            currentDateTime.dayOfMonth
        )
        datePicker.show()
    }

    private fun openTimePicker(pickedDate: LocalDateTime) {
        val timePicker = android.app.TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val zoned = pickedDate
                    .withHour(hourOfDay)
                    .withMinute(minute)
                    .atZone(ZoneId.systemDefault())
                selectedTimestamp = zoned.toInstant().toEpochMilli()
                updateTimestampField(selectedTimestamp)
            },
            pickedDate.hour,
            pickedDate.minute,
            true
        )
        timePicker.show()
    }
}
