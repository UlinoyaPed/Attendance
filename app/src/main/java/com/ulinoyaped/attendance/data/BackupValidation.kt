package com.ulinoyaped.attendance.data

import org.json.JSONArray
import org.json.JSONObject

fun validateBackup(root: JSONObject) {
    require(root.getInt("formatVersion") == 1) { "不支持的备份版本" }
    fun field(obj: JSONObject, key: String, max: Int = 120, required: Boolean = true): String {
        val value = if (!required && !obj.has(key)) "" else obj.get(key)
        require(value is String) { "$key 必须是文本" }
        requireSafeField(value, key, max, required)
        return value
    }
    fun unique(array: JSONArray, key: String, limit: Int): Set<String> {
        require(array.length() <= limit) { "$key 记录数量超限" }
        val ids = mutableSetOf<String>()
        for (i in 0 until array.length()) require(ids.add(field(array.getJSONObject(i), key))) { "$key 重复" }
        return ids
    }
    fun settings(obj: JSONObject) {
        if (obj.has("defaultStatus")) {
            require(obj.getString("defaultStatus") in AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.map { it.name }) {
                "默认点名状态无效"
            }
        }
        field(obj, "defaultReason", 240, false)
        for (key in obj.keys()) {
            val value = obj.get(key)
            if (key.startsWith("show") || key.startsWith("export") ||
                key in listOf("groupResultsByStatus", "confirmIncompleteAttendance", "compactRollCallRows")) {
                require(value is Boolean) { "$key 必须是开关值" }
            }
            val choices = when {
                key in listOf("longPressAction", "swipeLeftAction", "swipeRightAction") -> GestureAction.entries.map { it.name }
                key.endsWith("Icon") -> StatusIconOption.entries.map { it.name }
                key.endsWith("Color") -> StatusColorOption.entries.map { it.name }
                key == "historyTitleMode" -> HistoryTitleMode.entries.map { it.name }
                else -> null
            }
            if (choices != null) require(value is String && value in choices) { "$key 选项无效" }
        }
        (if (obj.has("collapsedResultStatuses")) obj.getJSONArray("collapsedResultStatuses") else null)?.let { array ->
            require(array.length() <= 5)
            for (i in 0 until array.length()) require(array.getString(i) in AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.map { it.name })
        }
    }
    val classes = root.getJSONArray("classes")
    val classIds = unique(classes, "id", 200)
    val rosterIds = mutableMapOf<String, Set<String>>()
    for (i in 0 until classes.length()) {
        val group = classes.getJSONObject(i)
        field(group, "name")
        val students = group.getJSONArray("students")
        rosterIds[group.getString("id")] = unique(students, "id", MAX_STUDENTS)
        for (j in 0 until students.length()) {
            field(students.getJSONObject(j), "name")
            field(students.getJSONObject(j), "studentNumber", required = false)
        }
        group.optJSONObject("attendanceSettings")?.let(::settings)
    }
    fun entries(obj: JSONObject, draft: Boolean) {
        val classId = field(obj, "classId")
        require(classId in classIds) { "记录引用不存在的班级" }
        val array = obj.getJSONArray("entries")
        val ids = unique(array, "studentId", MAX_STUDENTS)
        if (draft) require(ids.all { it in rosterIds.getValue(classId) }) { "草稿引用不存在的学生" }
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            field(entry, "studentName")
            field(entry, "studentNumber", required = false)
            field(entry, "reason", 240, false)
            require(entry.getString("status") in AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.map { it.name }) { "点名状态无效" }
        }
    }
    val sessions = root.getJSONArray("sessions")
    unique(sessions, "id", 10000)
    for (i in 0 until sessions.length()) {
        val session = sessions.getJSONObject(i)
        require(session.getLong("createdAt") > 0) { "点名时间无效" }
        entries(session, false)
    }
    val drafts = if (root.has("rollCallDrafts")) root.getJSONArray("rollCallDrafts") else JSONArray()
    unique(drafts, "classId", 200)
    for (i in 0 until drafts.length()) entries(drafts.getJSONObject(i), true)
    val options = root.getJSONObject("settings")
    settings(options)
    val reasons = if (options.has("absenceReasons")) options.getJSONArray("absenceReasons") else JSONArray()
    require(reasons.length() <= 100) { "原因最多 100 项" }
    for (i in 0 until reasons.length()) requireSafeField(reasons.getString(i), "原因", 240)
    val knownReasons = if (!options.has("absenceReasons")) AppSettings().absenceReasons.toSet()
        else (0 until reasons.length()).map { reasons.getString(it).trim() }.toSet()
    fun defaultReason(obj: JSONObject) {
        val reason = obj.optString("defaultReason").trim()
        require(reason.isEmpty() || reason in knownReasons) { "默认原因不在常用原因列表中" }
    }
    defaultReason(options)
    for (i in 0 until classes.length()) classes.getJSONObject(i).optJSONObject("attendanceSettings")?.let(::defaultReason)
}
