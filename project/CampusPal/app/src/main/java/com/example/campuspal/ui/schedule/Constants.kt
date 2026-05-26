package com.example.campuspal.ui.schedule

data class SectionTime(
    val section: Int,
    val startTime: String, // "HH:mm"
    val endTime: String,
)

object ScheduleConstants {
    val DEFAULT_SECTION_TIMES = listOf(
        SectionTime(1, "08:00", "08:50"),
        SectionTime(2, "09:00", "09:50"),
        SectionTime(3, "10:10", "11:00"),
        SectionTime(4, "11:10", "12:00"),
        SectionTime(5, "14:00", "14:50"),
        SectionTime(6, "15:00", "15:50"),
        SectionTime(7, "16:10", "17:00"),
        SectionTime(8, "17:10", "18:00"),
        SectionTime(9, "19:00", "19:50"),
        SectionTime(10, "20:00", "20:50"),
    )

    const val DEFAULT_TOTAL_WEEKS = 20

    fun getSectionTime(section: Int): SectionTime? =
        DEFAULT_SECTION_TIMES.find { it.section == section }

    fun getSectionLabel(section: Int): String {
        val st = getSectionTime(section) ?: return "第${section}节"
        return "第${section}节\n${st.startTime}"
    }
}
