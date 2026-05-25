package com.example.campuspal.ui.schedule

import app.cash.turbine.test
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.entity.Course
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ScheduleViewModelTest {

    private fun createViewModel(): ScheduleViewModel {
        val dao = object : CourseDao {
            override fun getAllCourses() = flowOf(emptyList())
            override fun getCoursesByDay(dayOfWeek: Int) = flowOf(emptyList())
            override suspend fun getCourseById(id: Long) = null
            override fun getCoursesForDayAndWeek(dayOfWeek: Int, currentWeek: Int) = flowOf(emptyList())
            override suspend fun insert(course: Course) = 1L
            override suspend fun update(course: Course) {}
            override suspend fun delete(course: Course) {}
            override suspend fun deleteById(id: Long) {}
        }
        return ScheduleViewModel(dao)
    }

    @Test
    fun `initial state has week 1 and no dialogs`() = runTest {
        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.currentWeek)
            assertFalse(state.showAddDialog)
            assertFalse(state.showDetailDialog)
            assertNull(state.selectedCourse)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `nextWeek increments current week`() = runTest {
        val vm = createViewModel()
        vm.nextWeek()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.currentWeek)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showAddDialog sets flag to true`() = runTest {
        val vm = createViewModel()
        vm.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.showAddDialog)

            vm.showAddDialog()
            val updated = awaitItem()
            assertTrue(updated.showAddDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `hideAddDialog resets flag`() = runTest {
        val vm = createViewModel()
        vm.showAddDialog()
        vm.uiState.test {
            // 跳过初始状态
            val withDialog = awaitItem()
            assertTrue(withDialog.showAddDialog)

            vm.hideAddDialog()
            val withoutDialog = awaitItem()
            assertFalse(withoutDialog.showAddDialog)
            cancelAndConsumeRemainingEvents()
        }
    }
}
