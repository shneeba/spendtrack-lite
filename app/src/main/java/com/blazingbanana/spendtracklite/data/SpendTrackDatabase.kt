package com.blazingbanana.spendtracklite.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Expense::class],
    version = 1,
    exportSchema = false
)
abstract class SpendTrackDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: SpendTrackDatabase? = null

        fun getInstance(context: Context): SpendTrackDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SpendTrackDatabase {
            return Room.databaseBuilder(
                context,
                SpendTrackDatabase::class.java,
                "spendtrack.db"
            ).build()
        }
    }
}
