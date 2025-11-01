package com.blazingbanana.spendtracklite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blazingbanana.spendtracklite.data.Expense
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseAdapter(
    private val currencyFormat: NumberFormat
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback) {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "dd MMM yyyy HH:mm",
        Locale.UK
    ).withZone(ZoneId.systemDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense, dateFormatter, currencyFormat)
    }

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textExpense: TextView = itemView.findViewById(R.id.textExpense)

        fun bind(
            expense: Expense,
            dateFormatter: DateTimeFormatter,
            currencyFormat: NumberFormat
        ) {
            val timestamp = dateFormatter.format(Instant.ofEpochMilli(expense.timestamp))
            val amountText = currencyFormat.format(expense.amount)
            textExpense.text = itemView.context.getString(
                R.string.item_expense_format,
                timestamp,
                amountText,
                expense.description
            )
        }
    }

    private object ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean =
            oldItem == newItem
    }
}
