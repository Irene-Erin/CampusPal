package com.example.campuspal.data.repository

import com.example.campuspal.data.db.dao.TaskDao
import com.example.campuspal.data.db.entity.Task
import kotlinx.coroutines.flow.Flow
import java.util.*

class TaskRepository(private val dao: TaskDao) {

    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()

    fun getUncompletedTasks(): Flow<List<Task>> = dao.getUncompletedTasks()

    fun getCompletedTasks(): Flow<List<Task>> = dao.getCompletedTasks()

    fun getTasksByCourse(courseId: Long): Flow<List<Task>> = dao.getTasksByCourse(courseId)

    fun getTasksByPriority(priority: Int): Flow<List<Task>> = dao.getTasksByPriority(priority)

    fun getTasksByDeadlineDate(date: Date): Flow<List<Task>> = dao.getTasksByDeadlineDate(date)

    fun getUrgentTasks(date: Date): Flow<List<Task>> = dao.getUrgentTasks(date)

    suspend fun getTaskById(id: Long): Task? = dao.getTaskById(id)

    suspend fun insert(task: Task): Long = dao.insert(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun delete(task: Task) = dao.delete(task)

    suspend fun setCompleted(id: Long, completed: Boolean) = dao.setCompleted(id, completed)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
