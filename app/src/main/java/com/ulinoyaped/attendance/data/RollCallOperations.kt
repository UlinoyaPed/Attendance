package com.ulinoyaped.attendance.data

/** Transient UI state only; persisted entries keep the existing snapshot format. */
data class AttendanceMark(val status: AttendanceStatus, val reason: String = "")

fun AttendanceStatus.isException(): Boolean = this != AttendanceStatus.UNMARKED && this != AttendanceStatus.PRESENT

enum class RollCallFilter(val label: String, val status: AttendanceStatus? = null) {
    ALL("全部"), UNMARKED("未处理"), MARKED("已处理"), EXCEPTIONS("异常"),
    LATE("迟到", AttendanceStatus.LATE), LEAVE("请假", AttendanceStatus.LEAVE),
    ABSENT("缺勤", AttendanceStatus.ABSENT), EXEMPT("不参与", AttendanceStatus.EXEMPT);

    fun matches(mark: AttendanceMark?): Boolean = when (this) {
        ALL -> true
        UNMARKED -> mark == null || mark.status == AttendanceStatus.UNMARKED
        MARKED -> mark != null && mark.status != AttendanceStatus.UNMARKED
        EXCEPTIONS -> mark?.status?.isException() == true
        else -> mark?.status == status
    }
}

fun filterRollCallStudents(
    students: List<Student>, marks: Map<String, AttendanceMark>, query: String, filter: RollCallFilter,
): List<Student> {
    val search = query.trim()
    return students.filter {
        (search.isEmpty() || it.name.contains(search, ignoreCase = true) || it.studentNumber.contains(search, ignoreCase = true)) &&
            filter.matches(marks[it.id])
    }
}

/** Defaults to filling blanks. Existing status AND reason are protected together. */
fun batchAttendanceMarks(
    before: Map<String, AttendanceMark>, studentIds: Set<String>, mark: AttendanceMark, overwrite: Boolean = false,
): Map<String, AttendanceMark> = before.toMutableMap().apply {
    studentIds.forEach { id ->
        if (overwrite || before[id] == null || before[id]?.status == AttendanceStatus.UNMARKED) {
            if (mark.status == AttendanceStatus.UNMARKED) remove(id)
            else put(id, mark.copy(reason = mark.reason.takeIf { mark.status.isException() }.orEmpty()))
        }
    }
}

/** Reason-only edits never create a status or attach a reason to PRESENT. */
fun batchAttendanceReasons(
    before: Map<String, AttendanceMark>, studentIds: Set<String>, reason: String,
): Map<String, AttendanceMark> = before.mapValues { (id, mark) ->
    if (id in studentIds && mark.status.isException()) mark.copy(reason = reason) else mark
}
