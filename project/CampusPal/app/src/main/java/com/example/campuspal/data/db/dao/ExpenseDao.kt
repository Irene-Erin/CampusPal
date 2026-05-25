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

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'expense' AND timestamp BETWEEN :start AND :end")
    suspend fun getRawTotalExpense(start: Long, end: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'income' AND timestamp BETWEEN :start AND :end")
    suspend fun getRawTotalIncome(start: Long, end: Long): Double?

    // 图表查询 — 某天支出分类汇总
    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE type='expense' AND timestamp BETWEEN :dayStart AND :dayEnd GROUP BY category ORDER BY total DESC")
    suspend fun getCategorySumByDay(dayStart: Long, dayEnd: Long): List<CategorySum>

    // 图表查询 — 按天汇总支出
    @Query("SELECT SUM(amount) as total FROM expenses WHERE type='expense' AND timestamp BETWEEN :start AND :end GROUP BY CAST((timestamp/86400000) AS INTEGER) ORDER BY CAST((timestamp/86400000) AS INTEGER)")
    suspend fun getDailySumsBetween(start: Long, end: Long): List<Double>

    // 图表查询 — 按月汇总支出
    @Query("SELECT SUM(amount) as total FROM expenses WHERE type='expense' AND timestamp BETWEEN :start AND :end GROUP BY strftime('%Y-%m', timestamp/1000, 'unixepoch') ORDER BY strftime('%Y-%m', timestamp/1000, 'unixepoch')")
    suspend fun getMonthlySumsBetween(start: Long, end: Long): List<Double>
}

data class CategorySum(
    val category: String,
    val total: Double,
)

data class DailySum(
    val dayOfMonth: Int,
    val total: Double,
)

data class MonthlyBreakdown(
    val yearMonth: String,
    val expenseTotal: Double,
    val incomeTotal: Double,
)
