package com.example.campuspal.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.entity.Course
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val courses: List<Course> = emptyList(),
    val currentWeek: Int = 1,
    val selectedCourse: Course? = null,
    val showDetailDialog: Boolean = false,
    val showAddDialog: Boolean = false,
    val isLoading: Boolean = false,
)

class ScheduleViewModel(private val courseDao: CourseDao) : ViewModel() {

    private val _currentWeek = MutableStateFlow(1)
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    private val _showDetailDialog = MutableStateFlow(false)
    private val _showAddDialog = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<ScheduleUiState> = combine(
        _currentWeek,
        _selectedCourse,
        _showDetailDialog,
        _showAddDialog,
        _isLoading,
    ) { week, course, detail, add, loading ->
        ScheduleUiState(
            courses = emptyList(),
            currentWeek = week,
            selectedCourse = course,
            showDetailDialog = detail,
            showAddDialog = add,
            isLoading = loading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    // 按天和周加载课程
    fun getCoursesForDay(dayOfWeek: Int): StateFlow<List<Course>> {
        return courseDao.getCoursesForDayAndWeek(dayOfWeek, _currentWeek.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // 获取所有课程（用于重叠检测）
    fun getAllCourses(): StateFlow<List<Course>> {
        return courseDao.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setWeek(week: Int) {
        _currentWeek.value = week.coerceIn(1, 20)
    }

    fun nextWeek() {
        _currentWeek.value = (_currentWeek.value + 1).coerceIn(1, 20)
    }

    fun prevWeek() {
        _currentWeek.value = (_currentWeek.value - 1).coerceIn(1, 20)
    }

    fun showDetail(course: Course) {
        _selectedCourse.value = course
        _showDetailDialog.value = true
    }

    fun hideDetail() {
        _showDetailDialog.value = false
        _selectedCourse.value = null
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun addCourse(course: Course) {
        viewModelScope.launch {
            courseDao.insert(course)
            _showAddDialog.value = false
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseDao.delete(course)
            _showDetailDialog.value = false
            _selectedCourse.value = null
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            courseDao.update(course)
        }
    }

    class Factory(private val courseDao: CourseDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(courseDao) as T
        }
    }
}
