package com.example.campuspal.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
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
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val courseId: Long? = null,
    val examDate: Long,          // 时间戳
    val isPinned: Boolean = false,
)
