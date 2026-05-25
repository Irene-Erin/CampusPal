package com.example.campuspal.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("courseId")],
)
data class Grade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null,
    val courseName: String,       // 冗余字段，课程删除后仍可显示
    val score: Double,            // 百分制分数
    val gradeLevel: String = "",  // 五级制：A/B/C/D/F
    val credits: Double = 2.0,   // 学分
    val semester: String = "",    // 如 "2024-2025-1"
)
