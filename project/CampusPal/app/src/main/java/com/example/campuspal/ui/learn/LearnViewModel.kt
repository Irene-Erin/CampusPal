package com.example.campuspal.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.dao.ExamDao
import com.example.campuspal.data.db.dao.StudySessionDao
import com.example.campuspal.data.db.dao.StudyStat
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Exam
import com.example.campuspal.data.db.entity.StudySession
import com.example.campuspal.worker.ExamReminderWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

enum class TimerState { IDLE, RUNNING, PAUSED }

data class LearnUiState(
    val exams: List<Exam> = emptyList(),
    val courses: List<Course> = emptyList(),
    val studyStats: List<StudyStat> = emptyList(),
    val totalStudyMinutes: Int = 0,
    val timerState: TimerState = TimerState.IDLE,
    val isWorkSession: Boolean = true,
    val selectedCourseId: Long? = null,
)

class LearnViewModel(
    private val examDao: ExamDao,
    private val studySessionDao: StudySessionDao,
    private val courseDao: CourseDao,
) : ViewModel() {

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    private val _isWorkSession = MutableStateFlow(true)
    private val _selectedCourseId = MutableStateFlow<Long?>(null)

    // 计时器 — 独立 StateFlow，脱离 combine 链，确保即时 UI 更新
    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(25 * 60)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _workDuration = MutableStateFlow(25)
    val workDuration: StateFlow<Int> = _workDuration.asStateFlow()

    val breakDuration: Int = 5

    // 对话框 — 独立 StateFlow，脱离 combine 链
    private val _showAddExamDialog = MutableStateFlow(false)
    val showAddExamDialog: StateFlow<Boolean> = _showAddExamDialog.asStateFlow()

    private val _showCoursePicker = MutableStateFlow(false)
    val showCoursePicker: StateFlow<Boolean> = _showCoursePicker.asStateFlow()

    private var timerJob: Job? = null

    private val now = System.currentTimeMillis()
    private val weekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }

    // 简化 combine 链 — 仅含数据流，不含 UI 交互状态
    val uiState: StateFlow<LearnUiState> = combine(
        examDao.getUpcomingExams(now),
        courseDao.getAllCourses(),
        studySessionDao.getStudyStatsBetween(weekStart, System.currentTimeMillis()),
        studySessionDao.getTotalMinutesBetween(weekStart, System.currentTimeMillis()),
        _timerState,
    ) { exams, courses, stats, totalMin, timer ->
        mutableListOf<Any?>(exams, courses, stats, totalMin, timer)
    }.combine(_isWorkSession) { list, isWork ->
        list.apply { add(isWork) }
    }.combine(_selectedCourseId) { list, courseId ->
        LearnUiState(
            exams = list[0] as List<Exam>,
            courses = list[1] as List<Course>,
            studyStats = list[2] as List<StudyStat>,
            totalStudyMinutes = (list[3] as Int?) ?: 0,
            timerState = list[4] as TimerState,
            isWorkSession = list[5] as Boolean,
            selectedCourseId = courseId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LearnUiState())

    // 番茄钟控制
    fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return
        _timerState.value = TimerState.RUNNING

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            // 计时结束
            if (_isWorkSession.value) {
                studySessionDao.insert(
                    StudySession(
                        courseId = _selectedCourseId.value,
                        durationMinutes = _workDuration.value,
                    )
                )
                _isWorkSession.value = false
                _remainingSeconds.value = breakDuration * 60
                _totalSeconds.value = breakDuration * 60
            } else {
                _isWorkSession.value = true
                _remainingSeconds.value = _workDuration.value * 60
                _totalSeconds.value = _workDuration.value * 60
            }
            _timerState.value = TimerState.IDLE
        }
    }

    fun pauseTimer() {
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _isWorkSession.value = true
        _remainingSeconds.value = _workDuration.value * 60
        _totalSeconds.value = _workDuration.value * 60
    }

    fun setWorkDuration(minutes: Int) {
        _workDuration.value = minutes
        if (_timerState.value == TimerState.IDLE && _isWorkSession.value) {
            _remainingSeconds.value = minutes * 60
            _totalSeconds.value = minutes * 60
        }
    }

    fun setSelectedCourse(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun showAddExam() { _showAddExamDialog.value = true }
    fun hideAddExam() { _showAddExamDialog.value = false }

    fun addExam(exam: Exam) {
        viewModelScope.launch {
            examDao.insert(exam)
            _showAddExamDialog.value = false
        }
    }

    fun togglePin(exam: Exam) {
        viewModelScope.launch {
            examDao.update(exam.copy(isPinned = !exam.isPinned))
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            examDao.delete(exam)
        }
    }

    class Factory(
        private val examDao: ExamDao,
        private val studySessionDao: StudySessionDao,
        private val courseDao: CourseDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LearnViewModel(examDao, studySessionDao, courseDao) as T
        }
    }
}
