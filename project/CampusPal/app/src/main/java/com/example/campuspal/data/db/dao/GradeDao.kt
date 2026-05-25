package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.Grade
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: Grade): Long

    @Update
    suspend fun update(grade: Grade)

    @Delete
    suspend fun delete(grade: Grade)

    @Query("SELECT * FROM grades ORDER BY semester DESC, id DESC")
    fun getAllGrades(): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE semester = :semester ORDER BY id DESC")
    fun getGradesBySemester(semester: String): Flow<List<Grade>>

    @Query("SELECT DISTINCT semester FROM grades ORDER BY semester DESC")
    fun getAllSemesters(): Flow<List<String>>

    @Query("SELECT * FROM grades WHERE id = :id")
    suspend fun getGradeById(id: Long): Grade?

    @Query("DELETE FROM grades WHERE id = :id")
    suspend fun deleteById(id: Long)
}
