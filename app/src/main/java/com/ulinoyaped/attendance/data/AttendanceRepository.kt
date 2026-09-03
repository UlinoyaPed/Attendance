package com.ulinoyaped.attendance.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AttendanceRepository(context: Context) {
    private val preferences = context.getSharedPreferences("attendance_data", Context.MODE_PRIVATE)

    private val _classes = MutableStateFlow(loadClasses())
    val classes: StateFlow<List<ClassGroup>> = _classes.asStateFlow()

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<AttendanceSession>> = _sessions.asStateFlow()

    fun addClass(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        _classes.value = _classes.value + ClassGroup(newId(), cleanName)
        saveClasses()
    }

    fun deleteClass(classId: String) {
        _classes.value = _classes.value.filterNot { it.id == classId }
        _sessions.value = _sessions.value.filterNot { it.classId == classId }
        saveClasses()
        saveSessions()
    }

    fun addStudent(classId: String, name: String, studentNumber: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        updateClass(classId) { group ->
            group.copy(
                students = group.students + Student(
                    id = newId(),
                    name = cleanName,
                    studentNumber = studentNumber.trim(),
                ),
            )
        }
    }

    fun removeStudent(classId: String, studentId: String) {
        updateClass(classId) { group ->
            group.copy(students = group.students.filterNot { it.id == studentId })
        }
    }

    fun importStudents(classId: String, text: String): Int {
        val imported = parseStudentList(text)
        var addedCount = 0
        updateClass(classId) { group ->
            val result = group.students.toMutableList()
            imported.forEach { candidate ->
                val duplicate = result.any { existing ->
                    if (candidate.studentNumber.isNotBlank()) {
                        existing.studentNumber.equals(candidate.studentNumber, ignoreCase = true)
                    } else {
                        existing.studentNumber.isBlank() && existing.name.equals(candidate.name, ignoreCase = true)
                    }
                }
                if (!duplicate) {
                    result += Student(newId(), candidate.name, candidate.studentNumber)
                    addedCount++
                }
            }
            group.copy(students = result)
        }
        return addedCount
    }

    fun saveSession(classId: String, entries: List<AttendanceEntry>): String {
        val session = AttendanceSession(
            id = newId(),
            classId = classId,
            createdAt = System.currentTimeMillis(),
            entries = entries,
        )
        _sessions.value = listOf(session) + _sessions.value
        saveSessions()
        return session.id
    }

    private fun updateClass(classId: String, transform: (ClassGroup) -> ClassGroup) {
        _classes.value = _classes.value.map { group ->
            if (group.id == classId) transform(group) else group
        }
        saveClasses()
    }

    private fun saveClasses() {
        val array = JSONArray()
        _classes.value.forEach { group ->
            val students = JSONArray()
            group.students.forEach { student ->
                students.put(
                    JSONObject()
                        .put("id", student.id)
                        .put("name", student.name)
                        .put("studentNumber", student.studentNumber),
                )
            }
            array.put(
                JSONObject()
                    .put("id", group.id)
                    .put("name", group.name)
                    .put("students", students),
            )
        }
        preferences.edit().putString(KEY_CLASSES, array.toString()).apply()
    }

    private fun saveSessions() {
        val array = JSONArray()
        _sessions.value.forEach { session ->
            val entries = JSONArray()
            session.entries.forEach { entry ->
                entries.put(
                    JSONObject()
                        .put("studentId", entry.studentId)
                        .put("studentName", entry.studentName)
                        .put("studentNumber", entry.studentNumber)
                        .put("status", entry.status.name)
                        .put("reason", entry.reason),
                )
            }
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("classId", session.classId)
                    .put("createdAt", session.createdAt)
                    .put("entries", entries),
            )
        }
        preferences.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    private fun loadClasses(): List<ClassGroup> = runCatching {
        val array = JSONArray(preferences.getString(KEY_CLASSES, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val studentArray = item.optJSONArray("students") ?: JSONArray()
                val students = buildList {
                    for (studentIndex in 0 until studentArray.length()) {
                        val student = studentArray.getJSONObject(studentIndex)
                        add(
                            Student(
                                id = student.getString("id"),
                                name = student.getString("name"),
                                studentNumber = student.optString("studentNumber"),
                            ),
                        )
                    }
                }
                add(ClassGroup(item.getString("id"), item.getString("name"), students))
            }
        }
    }.getOrDefault(emptyList())

    private fun loadSessions(): List<AttendanceSession> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SESSIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val entryArray = item.optJSONArray("entries") ?: JSONArray()
                val entries = buildList {
                    for (entryIndex in 0 until entryArray.length()) {
                        val entry = entryArray.getJSONObject(entryIndex)
                        add(
                            AttendanceEntry(
                                studentId = entry.getString("studentId"),
                                studentName = entry.getString("studentName"),
                                studentNumber = entry.optString("studentNumber"),
                                status = runCatching {
                                    AttendanceStatus.valueOf(entry.getString("status"))
                                }.getOrDefault(AttendanceStatus.ABSENT),
                                reason = entry.optString("reason"),
                            ),
                        )
                    }
                }
                add(
                    AttendanceSession(
                        id = item.getString("id"),
                        classId = item.getString("classId"),
                        createdAt = item.getLong("createdAt"),
                        entries = entries,
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_CLASSES = "classes"
        private const val KEY_SESSIONS = "sessions"

        private fun newId(): String = UUID.randomUUID().toString()
    }
}

fun parseStudentList(text: String): List<ImportedStudent> {
    val lines = text
        .removePrefix("\uFEFF")
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()
    if (lines.isEmpty()) return emptyList()

    val delimiter = listOf(',', '\t', ';').maxBy { candidate -> lines.first().count { it == candidate } }
    val rows = lines.map { parseDelimitedLine(it, delimiter).map(String::trim) }
    val first = rows.first().map { it.lowercase() }
    val nameHeaders = setOf("姓名", "名字", "name", "student name", "学生姓名")
    val numberHeaders = setOf("学号", "student id", "studentid", "id", "编号")
    val nameColumn = first.indexOfFirst { it in nameHeaders }
    val numberColumn = first.indexOfFirst { it in numberHeaders }
    val hasHeader = nameColumn >= 0 || numberColumn >= 0

    return rows.drop(if (hasHeader) 1 else 0).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        val (name, number) = if (hasHeader) {
            val parsedName = row.getOrNull(nameColumn).orEmpty()
            val parsedNumber = row.getOrNull(numberColumn).orEmpty()
            parsedName to parsedNumber
        } else if (row.size == 1) {
            row[0] to ""
        } else if (row[0].looksLikeStudentNumber()) {
            row.getOrNull(1).orEmpty() to row[0]
        } else {
            row[0] to row.getOrNull(1).orEmpty()
        }
        name.trim().takeIf { it.isNotEmpty() }?.let { ImportedStudent(it, number.trim()) }
    }
}

private fun String.looksLikeStudentNumber(): Boolean =
    isNotBlank() && all { it.isDigit() || it == '-' || it == '_' }

private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
    if (delimiter !in line) return listOf(line)
    val result = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' && quoted && line.getOrNull(index + 1) == '"' -> {
                field.append('"')
                index++
            }
            character == '"' -> quoted = !quoted
            character == delimiter && !quoted -> {
                result += field.toString()
                field.clear()
            }
            else -> field.append(character)
        }
        index++
    }
    result += field.toString()
    return result
}
