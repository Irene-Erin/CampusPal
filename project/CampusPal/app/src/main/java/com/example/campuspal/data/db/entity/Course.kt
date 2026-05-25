package com.example.campuspal.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int,          // 1-7 (周一至周日)
    val startTime: String,       // "HH:mm"
    val endTime: String,
    val startWeek: Int = 1,
    val endWeek: Int = 16,
    val weekType: String = "every", // "every", "odd", "even"
    val color: Int = 0xFF4A90D9.toInt(),
)
