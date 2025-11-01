package com.blazingbanana.spendtracklite

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.blazingbanana.spendtracklite.data.Expense
import com.blazingbanana.spendtracklite.data.SpendTrackDatabase
import com.blazingbanana.spendtracklite.databinding.ActivityAddExpenseBinding
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class TimestampPreset {
    NOW,
    TODAY,
    THIS_WEEK,
    CUSTOM
}

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private val expenseDao by lazy { SpendTrackDatabase.getInstance(this).expenseDao() }
    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.UK) }
    private val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.UK)
    }
    private val zoneId: ZoneId by lazy { ZoneId.systemDefault() }
    private val weekFields: WeekFields by lazy { WeekFields.of(Locale.getDefault()) }

    private var selectedPreset: TimestampPreset = TimestampPreset.NOW
    private var selectedTimestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSave.setOnClickListener { saveExpense() }

        binding.groupTimestampPreset.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) {
                if (group.checkedButtonIds.isEmpty()) {
                    selectedPreset = TimestampPreset.CUSTOM
                }
                return@addOnButtonCheckedListener
            }
            val preset = when (checkedId) {
                binding.buttonPresetNow.id -> TimestampPreset.NOW
                binding.buttonPresetToday.id -> TimestampPreset.TODAY
                binding.buttonPresetWeek.id -> TimestampPreset.THIS_WEEK
                else -> TimestampPreset.NOW
            }
            applyPreset(preset)
        }
        binding.groupTimestampPreset.check(binding.buttonPresetNow.id)
        binding.textTimestampValue.setOnClickListener { openCustomPicker() }

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

    private fun applyPreset(preset: TimestampPreset) {
        selectedPreset = preset
        selectedTimestamp = calculateTimestamp(preset)
        updateTimestampField(selectedTimestamp)
    }

    private fun updateTimestampField(epochMillis: Long) {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMillis),
            zoneId
        )
        binding.textTimestampValue.text = dateTimeFormatter.format(localDateTime)
    }

    private fun calculateTimestamp(preset: TimestampPreset): Long {
        return when (preset) {
            TimestampPreset.NOW -> System.currentTimeMillis()
            TimestampPreset.TODAY -> {
                ZonedDateTime.now(zoneId)
                    .toLocalDate()
                    .atTime(LocalTime.NOON)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
            TimestampPreset.THIS_WEEK -> {
                val startOfWeek = ZonedDateTime.now(zoneId)
                    .with(weekFields.dayOfWeek(), 1)
                    .toLocalDate()
                    .atTime(LocalTime.NOON)
                    .atZone(zoneId)
                startOfWeek.toInstant().toEpochMilli()
            }
            TimestampPreset.CUSTOM -> selectedTimestamp
        }
    }

    private fun openCustomPicker() {
        val currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(selectedTimestamp), zoneId)
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDateTime.of(year, month + 1, dayOfMonth, currentDateTime.hour, currentDateTime.minute)
                openTimePicker(pickedDate)
            },
            currentDateTime.year,
            currentDateTime.monthValue - 1,
            currentDateTime.dayOfMonth
        )
        datePicker.show()
    }

    private fun openTimePicker(pickedDate: LocalDateTime) {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val zoned = pickedDate
                    .withHour(hourOfDay)
                    .withMinute(minute)
                    .atZone(zoneId)
                selectedPreset = TimestampPreset.CUSTOM
                binding.groupTimestampPreset.clearChecked()
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
