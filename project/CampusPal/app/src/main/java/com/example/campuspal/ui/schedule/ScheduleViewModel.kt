package com.example.campuspal.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val courses: List<Course> = emptyList(),
    val currentWeek: Int = 1,
    val selectedCourse: Course? = null,
    val showDetailDialog: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingCourse: Course? = null,
    val conflictWarning: String? = null,
    val pendingCourse: Course? = null,
    val addDayOfWeek: Int? = null,
    val addStartSlot: Int? = null,
    val isLoading: Boolean = false,
)

class ScheduleViewModel(
    private val courseDao: CourseDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _currentWeek = MutableStateFlow(1)
    private val _selectedCourse = MutableStateFlow<Course?>(null)
    private val _showDetailDialog = MutableStateFlow(false)
    private val _showAddDialog = MutableStateFlow(false)
    private val _editingCourse = MutableStateFlow<Course?>(null)
    private val _conflictWarning = MutableStateFlow<String?>(null)
    private val _pendingCourse = MutableStateFlow<Course?>(null)
    private val _addDayOfWeek = MutableStateFlow<Int?>(null)
    private val _addStartSlot = MutableStateFlow<Int?>(null)
    private val _isLoading = MutableStateFlow(false)

    // 真实当前周数
    private val realCurrentWeek: StateFlow<Int> = settingsDataStore.semesterStart.map { ms ->
        WeekCalculator.calculateCurrentWeek(ms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    init {
        viewModelScope.launch {
            realCurrentWeek.collect { w -> if (_currentWeek.value == 1) _currentWeek.value = w }
        }
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        _currentWeek,
        _selectedCourse,
        _showDetailDialog,
    ) { week, course, detail ->
        mutableListOf<Any?>(week, course, detail)
    }.combine(_showAddDialog) { list, add ->
        list.apply { add(add) }
    }.combine(_editingCourse) { list, editing ->
        list.apply { add(editing) }
    }.combine(_conflictWarning) { list, conflict ->
        list.apply { add(conflict) }
    }.combine(_isLoading) { list, loading ->
        list.apply { add(loading) }
    }.combine(_addDayOfWeek) { list, day ->
        list.apply { add(day) }
    }.combine(_addStartSlot) { list, slot ->
        ScheduleUiState(
            currentWeek = list[0] as Int,
            selectedCourse = list[1] as Course?,
            showDetailDialog = list[2] as Boolean,
            showAddDialog = list[3] as Boolean,
            editingCourse = list[4] as Course?,
            conflictWarning = list[5] as String?,
            pendingCourse = _pendingCourse.value,
            isLoading = list[6] as Boolean,
            addDayOfWeek = list[7] as Int?,
            addStartSlot = slot,
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

    fun showAddDialog(dayOfWeek: Int? = null, startSlot: Int? = null) {
        _showAddDialog.value = true
        _addDayOfWeek.value = dayOfWeek?.let { if (it in 1..7) it else null }
        _addStartSlot.value = startSlot?.let { if (it in 0..13) it else null }
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _addDayOfWeek.value = null
        _addStartSlot.value = null
    }

    fun addCourse(course: Course) {
        viewModelScope.launch {
            val conflict = findConflictingCourse(course)
            if (conflict != null) {
                _conflictWarning.value = "与「${conflict.name}」（${conflict.startTime}-${conflict.endTime}）时间冲突，是否继续添加？"
                _pendingCourse.value = course
            } else {
                courseDao.insert(course)
                _showAddDialog.value = false
            }
        }
    }

    // 强制添加（忽略冲突）
    fun forceAddCourse() {
        viewModelScope.launch {
            val course = _pendingCourse.value ?: return@launch
            courseDao.insert(course)
            _showAddDialog.value = false
            _editingCourse.value?.let { courseDao.delete(it) }
            _editingCourse.value = null
            clearConflict()
        }
    }

    fun clearConflict() {
        _conflictWarning.value = null
        _pendingCourse.value = null
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
            _editingCourse.value = null
        }
    }

    // 检查课程时间冲突（同一天、时间段重叠）
    suspend fun findConflictingCourse(course: Course): Course? {
        val allCourses = courseDao.getAllCoursesOnce()
        return allCourses.find { existing ->
            existing.id != course.id
                    && existing.dayOfWeek == course.dayOfWeek
                    && WeekCalculator.isTimeOverlap(course.startTime, course.endTime, existing.startTime, existing.endTime)
        }
    }

    fun editCourse(course: Course) {
        _editingCourse.value = course
        _showDetailDialog.value = false
    }

    fun hideEditDialog() {
        _editingCourse.value = null
    }

    fun jumpToWeek(week: Int) {
        _currentWeek.value = week.coerceIn(1, 20)
    }

    fun jumpToToday() {
        _currentWeek.value = realCurrentWeek.value
    }

    class Factory(
        private val courseDao: CourseDao,
        private val settingsDataStore: SettingsDataStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(courseDao, settingsDataStore) as T
        }
    }
}
