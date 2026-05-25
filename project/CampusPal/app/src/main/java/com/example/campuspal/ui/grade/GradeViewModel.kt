package com.example.campuspal.ui.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.campuspal.data.db.dao.CourseDao
import com.example.campuspal.data.db.dao.GradeDao
import com.example.campuspal.data.db.entity.Course
import com.example.campuspal.data.db.entity.Grade
import com.example.campuspal.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GradeUiState(
    val grades: List<Grade> = emptyList(),
    val courses: List<Course> = emptyList(),
    val semesters: List<String> = emptyList(),
    val selectedSemester: String? = null,
    val gpaStandard: String = "4.0",
    val overallGpa: Double = 0.0,
    val semesterGpas: Map<String, Double> = emptyMap(),
    val showAddDialog: Boolean = false,
)

class GradeViewModel(
    private val gradeDao: GradeDao,
    private val courseDao: CourseDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _selectedSemester = MutableStateFlow<String?>(null)
    private val _showAddDialog = MutableStateFlow(false)

    private val allGrades = gradeDao.getAllGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<GradeUiState> = combine(
        allGrades,
        courseDao.getAllCourses(),
        gradeDao.getAllSemesters(),
        _selectedSemester,
        settingsDataStore.gpaStandard,
    ) { grades, courses, semesters, selectedSem, standard ->
        arrayOf(grades, courses, semesters, selectedSem, standard)
    }.combine(_showAddDialog) { arr, showAdd ->
        val grades = arr[0] as List<Grade>
        val courses = arr[1] as List<Course>
        val semesters = arr[2] as List<String>
        val selectedSem = arr[3] as String?
        val standard = arr[4] as String

        val filtered = if (selectedSem != null) grades.filter { it.semester == selectedSem } else grades

        // 计算总体 GPA
        val overallGpa = calculateGpa(grades, standard)

        // 计算每学期 GPA
        val semesterGpas = grades.groupBy { it.semester }.mapValues { (_, semesterGrades) ->
            calculateGpa(semesterGrades, standard)
        }

        GradeUiState(
            grades = filtered,
            courses = courses,
            semesters = semesters,
            selectedSemester = selectedSem,
            gpaStandard = standard,
            overallGpa = overallGpa,
            semesterGpas = semesterGpas,
            showAddDialog = showAdd,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GradeUiState())

    fun selectSemester(semester: String?) {
        _selectedSemester.value = semester
    }

    fun setGpaStandard(standard: String) {
        viewModelScope.launch { settingsDataStore.setGpaStandard(standard) }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun addGrade(grade: Grade) {
        viewModelScope.launch {
            gradeDao.insert(grade)
            _showAddDialog.value = false
        }
    }

    fun deleteGrade(grade: Grade) {
        viewModelScope.launch { gradeDao.delete(grade) }
    }

    class Factory(
        private val gradeDao: GradeDao,
        private val courseDao: CourseDao,
        private val settingsDataStore: SettingsDataStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GradeViewModel(gradeDao, courseDao, settingsDataStore) as T
        }
    }
}

fun calculateGpa(grades: List<com.example.campuspal.data.db.entity.Grade>, standard: String): Double {
    if (grades.isEmpty()) return 0.0

    var totalPoints = 0.0
    var totalCredits = 0.0

    for (grade in grades) {
        val gp = scoreToGradePoint(grade.score, standard)
        totalPoints += gp * grade.credits
        totalCredits += grade.credits
    }

    return if (totalCredits > 0) {
        Math.round(totalPoints / totalCredits * 100.0) / 100.0
    } else 0.0
}

fun scoreToGradePoint(score: Double, standard: String): Double {
    return if (standard == "5.0") {
        when {
            score >= 90 -> 5.0
            score >= 85 -> 4.5
            score >= 80 -> 4.0
            score >= 75 -> 3.5
            score >= 70 -> 3.0
            score >= 65 -> 2.5
            score >= 60 -> 2.0
            else -> 0.0
        }
    } else {
        when {
            score >= 90 -> 4.0
            score >= 85 -> 3.7
            score >= 82 -> 3.3
            score >= 78 -> 3.0
            score >= 75 -> 2.7
            score >= 72 -> 2.3
            score >= 68 -> 2.0
            score >= 64 -> 1.5
            score >= 60 -> 1.0
            else -> 0.0
        }
    }
}

fun scoreToGradeLevel(score: Double): String {
    return when {
        score >= 90 -> "A"
        score >= 80 -> "B"
        score >= 70 -> "C"
        score >= 60 -> "D"
        else -> "F"
    }
}
