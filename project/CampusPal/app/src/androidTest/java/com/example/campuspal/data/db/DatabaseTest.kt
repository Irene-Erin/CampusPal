package com.example.campuspal.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspal.data.db.dao.*
import com.example.campuspal.data.db.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var courseDao: CourseDao
    private lateinit var taskDao: TaskDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        courseDao = db.courseDao()
        taskDao = db.taskDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ========== CourseDao 测试 ==========

    @Test
    fun insertAndGetTodayCourse() = runBlocking {
        val course = Course(
            name = "高等数学",
            teacher = "张老师",
            location = "教学楼A101",
            dayOfWeek = 1, // 周一
            startTime = "08:00",
            endTime = "09:40",
            startWeek = 1,
            endWeek = 16,
            weekType = "every",
        )
        courseDao.insert(course)

        val courses = courseDao.getCoursesByDay(1).first()
        assertEquals(1, courses.size)
        assertEquals("高等数学", courses[0].name)
        assertEquals("教学楼A101", courses[0].location)
    }

    @Test
    fun insertAndDeleteCourse() = runBlocking {
        val course = Course(name = "测试课", dayOfWeek = 3, startTime = "10:00", endTime = "11:40")
        val id = courseDao.insert(course)
        assertTrue(id > 0)

        courseDao.deleteById(id)
        val courses = courseDao.getAllCourses().first()
        assertTrue(courses.isEmpty())
    }

    @Test
    fun filterByWeekType() = runBlocking {
        courseDao.insert(Course(name = "单周课", dayOfWeek = 1, startTime = "08:00", endTime = "08:50", weekType = "odd"))
        courseDao.insert(Course(name = "双周课", dayOfWeek = 1, startTime = "09:00", endTime = "09:50", weekType = "even"))
        courseDao.insert(Course(name = "每周课", dayOfWeek = 1, startTime = "10:00", endTime = "10:50", weekType = "every"))

        // 第3周（单周）：应该有单周课 + 每周课
        val oddWeekCourses = courseDao.getCoursesForDayAndWeek(1, 3).first()
        assertEquals(2, oddWeekCourses.size)

        // 第4周（双周）：应该有双周课 + 每周课
        val evenWeekCourses = courseDao.getCoursesForDayAndWeek(1, 4).first()
        assertEquals(2, evenWeekCourses.size)
    }

    // ========== TaskDao 测试 ==========

    @Test
    fun insertAndGetByPriority() = runBlocking {
        taskDao.insert(Task(title = "低优先", priority = 1))
        taskDao.insert(Task(title = "高优先", priority = 3))
        taskDao.insert(Task(title = "中优先", priority = 2))

        val highPriority = taskDao.getTasksByPriority(3).first()
        assertEquals(1, highPriority.size)
        assertEquals("高优先", highPriority[0].title)
    }

    @Test
    fun getUncompletedTasks() = runBlocking {
        taskDao.insert(Task(title = "未完成1", isCompleted = false))
        taskDao.insert(Task(title = "已完成", isCompleted = true))
        taskDao.insert(Task(title = "未完成2", isCompleted = false))

        val uncompleted = taskDao.getUncompletedTasks().first()
        assertEquals(2, uncompleted.size)
    }

    @Test
    fun toggleTaskCompleted() = runBlocking {
        val id = taskDao.insert(Task(title = "测试任务", isCompleted = false))
        taskDao.setCompleted(id, true)

        val task = taskDao.getTaskById(id)
        assertNotNull(task)
        assertTrue(task!!.isCompleted)
    }

    // ========== ExpenseDao 测试 ==========

    @Test
    fun insertAndGetByMonth() = runBlocking {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 15)
        val midMonth = cal.timeInMillis

        expenseDao.insert(Expense(amount = 50.0, category = "饮食", type = "expense", timestamp = midMonth))
        expenseDao.insert(Expense(amount = 200.0, category = "购物", type = "expense", timestamp = midMonth))
        expenseDao.insert(Expense(amount = 1000.0, category = "工资", type = "income", timestamp = midMonth))

        // 本月范围
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val monthEnd = cal.timeInMillis

        // 本月总支出
        val totalExpense = expenseDao.getTotalExpenseBetween(monthStart, monthEnd).first()
        assertEquals(250.0, totalExpense!!, 0.01)

        // 分类汇总
        val catSum = expenseDao.getCategorySumBetween(monthStart, monthEnd).first()
        assertEquals(2, catSum.size)
    }

    @Test
    fun expenseTypeFilter() = runBlocking {
        expenseDao.insert(Expense(amount = 30.0, category = "交通", type = "expense"))
        expenseDao.insert(Expense(amount = 500.0, category = "兼职", type = "income"))

        val expenses = expenseDao.getExpensesByType("expense").first()
        assertEquals(1, expenses.size)
        assertEquals("交通", expenses[0].category)

        val incomes = expenseDao.getExpensesByType("income").first()
        assertEquals(1, incomes.size)
        assertEquals("兼职", incomes[0].category)
    }
}
