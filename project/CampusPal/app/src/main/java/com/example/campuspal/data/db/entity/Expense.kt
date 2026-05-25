package com.example.campuspal.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,         // 分类：饮食、交通、购物、娱乐、学习、其他
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "expense", // "income" 或 "expense"
)
