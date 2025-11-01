package com.blazingbanana.spendtracklite

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blazingbanana.spendtracklite.data.SpendTrackDatabase
import com.blazingbanana.spendtracklite.databinding.ActivityHomeBinding
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val expenseDao by lazy { SpendTrackDatabase.getInstance(this).expenseDao() }
    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.UK) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.buttonViewSpend.setOnClickListener {
            startActivity(Intent(this, ViewSpendActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTodayTotal()
    }

    private fun refreshTodayTotal() {
        lifecycleScope.launch {
            val totalToday = withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val startOfDay: ZonedDateTime = LocalDate.now(zone).atStartOfDay(zone)
                val endOfDay = startOfDay.plusDays(1).minusNanos(1)
                expenseDao.getTotalBetween(
                    startOfDay.toInstant().toEpochMilli(),
                    endOfDay.toInstant().toEpochMilli()
                )
            }
            binding.textTodayTotal.text = getString(
                R.string.home_today_total_format,
                currencyFormatter.format(totalToday)
            )
        }
    }
}
