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
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private val expenseDao by lazy { SpendTrackDatabase.getInstance(this).expenseDao() }
    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.UK) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSave.setOnClickListener { saveExpense() }

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
            timestamp = System.currentTimeMillis()
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
}
