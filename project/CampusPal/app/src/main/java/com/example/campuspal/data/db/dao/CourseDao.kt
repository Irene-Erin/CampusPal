package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long

    @Update
    suspend fun update(course: Course)

    @Delete
    suspend fun delete(course: Course)

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startTime")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startTime")
    suspend fun getAllCoursesOnce(): List<Course>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek ORDER BY startTime")
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek AND :currentWeek BETWEEN startWeek AND endWeek AND (weekType = 'every' OR (weekType = 'odd' AND :currentWeek % 2 = 1) OR (weekType = 'even' AND :currentWeek % 2 = 0)) ORDER BY startTime")
    fun getCoursesForDayAndWeek(dayOfWeek: Int, currentWeek: Int): Flow<List<Course>>

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
