package com.ulinoyaped.attendance.data

data class Student(
    val id: String,
    val name: String,
    val studentNumber: String = "",
)

data class ClassGroup(
    val id: String,
    val name: String,
    val students: List<Student> = emptyList(),
)

enum class AttendanceStatus(val label: String) {
    UNMARKED("未点"),
    PRESENT("到"),
    LATE("迟到"),
    LEAVE("请假"),
    ABSENT("缺勤"),
}

data class AttendanceEntry(
    val studentId: String,
    val studentName: String,
    val studentNumber: String,
    val status: AttendanceStatus,
    val reason: String = "",
)

data class AttendanceSession(
    val id: String,
    val classId: String,
    val createdAt: Long,
    val entries: List<AttendanceEntry>,
)

data class ImportedStudent(
    val name: String,
    val studentNumber: String,
)
