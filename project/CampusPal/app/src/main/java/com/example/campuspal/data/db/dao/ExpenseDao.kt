package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY timestamp DESC")
    fun getExpensesByType(type: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :startOfMonth AND :endOfMonth ORDER BY timestamp DESC")
    fun getExpensesByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'expense' AND timestamp BETWEEN :start AND :end")
    fun getTotalExpenseBetween(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'income' AND timestamp BETWEEN :start AND :end")
    fun getTotalIncomeBetween(start: Long, end: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'expense' AND category = :category AND timestamp BETWEEN :start AND :end")
    fun getCategoryExpenseBetween(category: String, start: Long, end: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE type = 'expense' AND timestamp BETWEEN :start AND :end GROUP BY category ORDER BY total DESC")
    fun getCategorySumBetween(start: Long, end: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class CategorySum(
    val category: String,
    val total: Double,
)
