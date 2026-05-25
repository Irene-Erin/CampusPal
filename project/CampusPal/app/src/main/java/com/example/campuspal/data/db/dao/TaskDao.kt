package com.example.campuspal.data.db.dao

import androidx.room.*
import com.example.campuspal.data.db.entity.Task
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks ORDER BY priority DESC, deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, deadline ASC")
    fun getUncompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY deadline DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY priority DESC, deadline ASC")
    fun getTasksByCourse(courseId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY deadline ASC")
    fun getTasksByPriority(priority: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline IS NOT NULL AND date(deadline / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch') ORDER BY priority DESC")
    fun getTasksByDeadlineDate(date: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND (deadline IS NOT NULL AND deadline <= :date OR deadline IS NULL) ORDER BY priority DESC, deadline ASC LIMIT 5")
    fun getUrgentTasks(date: Date): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
