package com.blazingbanana.spendtracklite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesOnce(): List<Expense>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE timestamp BETWEEN :startMillis AND :endMillis")
    suspend fun getTotalBetween(startMillis: Long, endMillis: Long): Double
}
