package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: Exam): Long

    @Update
    suspend fun update(exam: Exam)

    @Delete
    suspend fun delete(exam: Exam)

    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE examDate > :now ORDER BY isPinned DESC, examDate ASC")
    fun getUpcomingExams(now: Long): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Long): Exam?

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: Long)
}
