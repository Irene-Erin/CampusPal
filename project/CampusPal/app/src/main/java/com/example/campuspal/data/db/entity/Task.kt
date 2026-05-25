package com.example.campuspal.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "tasks",
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
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val deadline: Date? = null,
    val priority: Int = 0,        // 0-3 (无/低/中/高)
    val isCompleted: Boolean = false,
    val courseId: Long? = null,   // 关联课程，可空
)
