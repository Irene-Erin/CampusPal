package com.example.campuspal.data.repository

import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.entity.Course
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val dao: CourseDao) {

    fun getAllCourses(): Flow<List<Course>> = dao.getAllCourses()

    fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>> = dao.getCoursesByDay(dayOfWeek)

    fun getCoursesForDayAndWeek(dayOfWeek: Int, currentWeek: Int): Flow<List<Course>> =
        dao.getCoursesForDayAndWeek(dayOfWeek, currentWeek)

    suspend fun getCourseById(id: Long): Course? = dao.getCourseById(id)

    suspend fun insert(course: Course): Long = dao.insert(course)

    suspend fun update(course: Course) = dao.update(course)

    suspend fun delete(course: Course) = dao.delete(course)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
