package com.example.campuspal.data.repository

import com.example.campuspal.data.db.dao.CategorySum
import com.example.campuspal.data.db.dao.ExpenseDao
import com.example.campuspal.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    fun getExpensesByType(type: String): Flow<List<Expense>> = dao.getExpensesByType(type)

    fun getExpensesByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Expense>> =
        dao.getExpensesByMonth(startOfMonth, endOfMonth)

    fun getTotalExpenseBetween(start: Long, end: Long): Flow<Double?> =
        dao.getTotalExpenseBetween(start, end)

    fun getTotalIncomeBetween(start: Long, end: Long): Flow<Double?> =
        dao.getTotalIncomeBetween(start, end)

    fun getCategoryExpenseBetween(category: String, start: Long, end: Long): Flow<Double?> =
        dao.getCategoryExpenseBetween(category, start, end)

    fun getCategorySumBetween(start: Long, end: Long): Flow<List<CategorySum>> =
        dao.getCategorySumBetween(start, end)

    suspend fun getExpenseById(id: Long): Expense? = dao.getExpenseById(id)

    suspend fun insert(expense: Expense): Long = dao.insert(expense)

    suspend fun update(expense: Expense) = dao.update(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
