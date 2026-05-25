package com.example.campuspal.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.dao.TaskDao
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class TodoFilter {
    ALL, TODAY, HIGH_PRIORITY, UNCOMPLETED, COMPLETED
}

data class TodoUiState(
    val tasks: List<Task> = emptyList(),
    val courses: List<Course> = emptyList(),
    val filter: TodoFilter = TodoFilter.ALL,
    val selectedCourseId: Long? = null,
    val showAddDialog: Boolean = false,
    val isRefreshing: Boolean = false,
)

class TodoViewModel(private val taskDao: TaskDao) : ViewModel() {

    private val _filter = MutableStateFlow(TodoFilter.ALL)
    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)

    private val _allTasks = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TodoUiState> = combine(
        _allTasks,
        _filter,
        _selectedCourseId,
        _showAddDialog,
        _isRefreshing,
    ) { tasks, filter, courseId, showAdd, refreshing ->
        val filtered = when (filter) {
            TodoFilter.ALL -> tasks
            TodoFilter.TODAY -> {
                val today = Calendar.getInstance()
                tasks.filter { task ->
                    task.deadline?.let {
                        val taskCal = Calendar.getInstance().apply { time = it }
                        taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                        taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    } ?: false
                }
            }
            TodoFilter.HIGH_PRIORITY -> tasks.filter { it.priority >= 2 }
            TodoFilter.UNCOMPLETED -> tasks.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }.let { list ->
            if (courseId != null) list.filter { it.courseId == courseId } else list
        }

        TodoUiState(
            tasks = filtered,
            courses = emptyList(),
            filter = filter,
            selectedCourseId = courseId,
            showAddDialog = showAdd,
            isRefreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

    fun setFilter(filter: TodoFilter) {
        _filter.value = filter
    }

    fun setCourseFilter(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            taskDao.setCompleted(task.id, !task.isCompleted)
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insert(task)
            _showAddDialog.value = false
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.delete(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    class Factory(private val taskDao: TaskDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoViewModel(taskDao) as T
        }
    }
}
