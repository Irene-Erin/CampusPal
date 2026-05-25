package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession): Long

    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getSessionsBetween(start: Long, end: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE courseId = :courseId ORDER BY date DESC")
    fun getSessionsByCourse(courseId: Long): Flow<List<StudySession>>

    @Query("SELECT courseId, SUM(durationMinutes) as total FROM study_sessions WHERE date BETWEEN :start AND :end GROUP BY courseId ORDER BY total DESC")
    fun getStudyStatsBetween(start: Long, end: Long): Flow<List<StudyStat>>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE date BETWEEN :start AND :end")
    fun getTotalMinutesBetween(start: Long, end: Long): Flow<Int?>

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class StudyStat(
    val courseId: Long?,
    val total: Int,
)
