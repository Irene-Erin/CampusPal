package com.example.campuspal.ui.todo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.dao.TaskDao
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Task
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class TodoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(tasks: List<Task> = emptyList()): TodoViewModel {
        val dao = object : TaskDao {
            override fun getAllTasks() = flowOf(tasks)
            override fun getUncompletedTasks() = flowOf(tasks.filter { !it.isCompleted })
            override fun getCompletedTasks() = flowOf(tasks.filter { it.isCompleted })
            override fun getTasksByCourse(courseId: Long) = flowOf(tasks.filter { it.courseId == courseId })
            override fun getTasksByPriority(priority: Int) = flowOf(tasks.filter { it.priority == priority })
            override fun getTasksByDeadlineDate(date: java.util.Date) = flowOf(tasks)
            override fun getUrgentTasks(date: java.util.Date) = flowOf(tasks)
            override suspend fun getTaskById(id: Long) = tasks.firstOrNull { it.id == id }
            override suspend fun insert(task: Task) = task.id
            override suspend fun update(task: Task) {}
            override suspend fun delete(task: Task) {}
            override suspend fun setCompleted(id: Long, completed: Boolean) {}
            override suspend fun deleteById(id: Long) {}
        }
        return TodoViewModel(dao)
    }

    @Test
    fun `show empty state when no tasks`() {
        val vm = createViewModel()
        composeTestRule.setContent { TodoScreen(vm) }

        composeTestRule.onNodeWithText("暂无待办事项").assertExists()
    }

    @Test
    fun `show tasks in list`() {
        val tasks = listOf(
            Task(id = 1, title = "完成作业", priority = 2),
            Task(id = 2, title = "复习考试", priority = 3),
        )
        val vm = createViewModel(tasks)
        composeTestRule.setContent { TodoScreen(vm) }

        composeTestRule.onNodeWithText("完成作业").assertExists()
        composeTestRule.onNodeWithText("复习考试").assertExists()
    }

    @Test
    fun `filter chips are present`() {
        val vm = createViewModel()
        composeTestRule.setContent { TodoScreen(vm) }

        composeTestRule.onNodeWithText("全部").assertExists()
        composeTestRule.onNodeWithText("今日").assertExists()
        composeTestRule.onNodeWithText("未完成").assertExists()
    }

    @Test
    fun `FAB is visible`() {
        val vm = createViewModel()
        composeTestRule.setContent { TodoScreen(vm) }

        composeTestRule.onNodeWithContentDescription("添加待办").assertExists()
    }
}
