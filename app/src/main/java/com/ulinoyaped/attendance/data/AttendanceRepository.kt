package com.ulinoyaped.attendance.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AttendanceRepository internal constructor(private val preferences: android.content.SharedPreferences) {

    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "attendance-writer").apply { isDaemon = true }
    }
    private val _storageError = MutableStateFlow<String?>(null)
    val storageError: StateFlow<String?> = _storageError.asStateFlow()
    @Volatile private var loadFailed = false
    @Volatile private var lastConfirmedSnapshot = preferences.getString("verified_snapshot", null)

    private val _classes = MutableStateFlow(loadClasses())
    val classes: StateFlow<List<ClassGroup>> = _classes.asStateFlow()

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<AttendanceSession>> = _sessions.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val rollCallDrafts = loadRollCallDrafts().toMutableMap()

    init {
        if (!loadFailed) runCatching {
            val current = snapshot()
            validateBackup(JSONObject(current))
            if (lastConfirmedSnapshot == null) lastConfirmedSnapshot = current
        }.onFailure {
            loadFailed = true
            _storageError.value = "本地数据校验失败，已停止保存。请导出原始数据并恢复备份。"
        }
    }

    fun addClass(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        _classes.value = _classes.value + ClassGroup(newId(), cleanName)
        saveClasses()
    }

    fun deleteClass(classId: String) {
        _classes.value = _classes.value.filterNot { it.id == classId }
        _sessions.value = _sessions.value.filterNot { it.classId == classId }
        rollCallDrafts.remove(classId)
        saveClasses()
    }

    fun renameClass(classId: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        updateClass(classId) { it.copy(name = cleanName) }
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
        rollCallDrafts[classId]?.let { draft ->
            val remaining = draft.filterNot { it.studentId == studentId }
            if (remaining.isEmpty()) rollCallDrafts.remove(classId) else rollCallDrafts[classId] = remaining
        }
        updateClass(classId) { group ->
            group.copy(students = group.students.filterNot { it.id == studentId })
        }
    }

    fun updateStudent(classId: String, studentId: String, name: String, studentNumber: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        updateClass(classId) { group ->
            group.copy(
                students = group.students.map { student ->
                    if (student.id == studentId) {
                        student.copy(name = cleanName, studentNumber = studentNumber.trim())
                    } else {
                        student
                    }
                },
            )
        }
    }

    fun importStudents(classId: String, text: String): Int {
        val imported = parseStudentList(text)
        var addedCount = 0
        updateClass(classId) { group ->
            val result = group.students.toMutableList()
            val keys = group.students.mapTo(mutableSetOf()) { studentKey(it.name, it.studentNumber) }
            imported.forEach { candidate ->
                val duplicate = !keys.add(studentKey(candidate.name, candidate.studentNumber))
                if (!duplicate) {
                    result += Student(newId(), candidate.name, candidate.studentNumber)
                    addedCount++
                }
            }
            require(result.size <= MAX_STUDENTS) { "班级最多 $MAX_STUDENTS 人" }
            group.copy(students = result)
        }
        check(awaitSaved()) { "名单未能保存，请先恢复设备数据" }
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
        rollCallDrafts.remove(classId)
        saveSessions()
        return session.id
    }

    fun getRollCallDraft(classId: String): List<AttendanceEntry> =
        rollCallDrafts[classId].orEmpty()

    fun saveRollCallDraft(classId: String, entries: List<AttendanceEntry>) {
        if (entries.isEmpty()) {
            rollCallDrafts.remove(classId)
        } else {
            rollCallDrafts[classId] = entries
        }
        saveRollCallDrafts()
    }

    fun deleteSession(sessionId: String) {
        _sessions.value = _sessions.value.filterNot { it.id == sessionId }
        saveSessions()
    }

    fun updateSessionEntry(
        sessionId: String,
        studentId: String,
        status: AttendanceStatus,
        reason: String,
    ) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id != sessionId) {
                session
            } else {
                session.copy(
                    entries = session.entries.map { entry ->
                        if (entry.studentId == studentId) {
                            entry.copy(status = status, reason = reason.trim())
                        } else {
                            entry
                        }
                    },
                )
            }
        }
        saveSessions()
    }

    fun addAbsenceReason(reason: String) {
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty() || _settings.value.absenceReasons.any { it == cleanReason }) return
        updateSettings(_settings.value.copy(absenceReasons = _settings.value.absenceReasons + cleanReason))
    }

    fun removeAbsenceReason(reason: String) {
        _settings.value = _settings.value.copy(
            absenceReasons = _settings.value.absenceReasons - reason,
            defaultReason = _settings.value.defaultReason.takeUnless { it == reason }.orEmpty(),
        )
        _classes.value = _classes.value.map { group ->
            group.copy(
                attendanceSettings = group.attendanceSettings?.let { settings ->
                    if (settings.defaultReason == reason) settings.copy(defaultReason = "") else settings
                },
            )
        }
        saveClasses()
    }

    fun moveAbsenceReason(fromIndex: Int, toIndex: Int) {
        val reasons = _settings.value.absenceReasons.toMutableList()
        if (fromIndex !in reasons.indices || toIndex !in reasons.indices || fromIndex == toIndex) return
        val reason = reasons.removeAt(fromIndex)
        reasons.add(toIndex, reason)
        updateSettings(_settings.value.copy(absenceReasons = reasons))
    }

    fun setDefaultReason(reason: String) {
        if (reason.isEmpty() || reason in _settings.value.absenceReasons) {
            updateSettings(_settings.value.copy(defaultReason = reason))
        }
    }

    fun setDefaultStatus(status: AttendanceStatus) {
        if (status != AttendanceStatus.UNMARKED) updateSettings(_settings.value.copy(defaultStatus = status))
    }

    fun setLongPressAction(action: GestureAction) {
        updateSettings(_settings.value.copy(longPressAction = action))
    }

    fun setSwipeLeftAction(action: GestureAction) {
        updateSettings(_settings.value.copy(swipeLeftAction = action))
    }

    fun setSwipeRightAction(action: GestureAction) {
        updateSettings(_settings.value.copy(swipeRightAction = action))
    }

    fun setStatusIcon(status: AttendanceStatus, icon: StatusIconOption) {
        val updated = when (status) {
            AttendanceStatus.PRESENT -> _settings.value.copy(presentIcon = icon)
            AttendanceStatus.LATE -> _settings.value.copy(lateIcon = icon)
            AttendanceStatus.LEAVE -> _settings.value.copy(leaveIcon = icon)
            AttendanceStatus.ABSENT -> _settings.value.copy(absentIcon = icon)
            AttendanceStatus.EXEMPT -> _settings.value.copy(exemptIcon = icon)
            AttendanceStatus.UNMARKED -> return
        }
        updateSettings(updated)
    }

    fun setStatusColor(status: AttendanceStatus, color: StatusColorOption) {
        val updated = when (status) {
            AttendanceStatus.PRESENT -> _settings.value.copy(presentColor = color)
            AttendanceStatus.LATE -> _settings.value.copy(lateColor = color)
            AttendanceStatus.LEAVE -> _settings.value.copy(leaveColor = color)
            AttendanceStatus.ABSENT -> _settings.value.copy(absentColor = color)
            AttendanceStatus.EXEMPT -> _settings.value.copy(exemptColor = color)
            AttendanceStatus.UNMARKED -> return
        }
        updateSettings(updated)
    }

    fun setClassAttendanceSettings(classId: String, settings: ClassAttendanceSettings?) {
        updateClass(classId) { it.copy(attendanceSettings = settings) }
    }

    fun setGroupResultsByStatus(enabled: Boolean) =
        updateSettings(_settings.value.copy(groupResultsByStatus = enabled))

    fun setHistoryTitleMode(mode: HistoryTitleMode) =
        updateSettings(_settings.value.copy(historyTitleMode = mode))

    fun setDisplayOption(option: DisplayOption, enabled: Boolean) {
        val collapseStatus = resultCollapseOptions[option]
        if (collapseStatus != null) {
            val current = _settings.value.collapsedResultStatuses
            updateSettings(_settings.value.copy(
                collapsedResultStatuses = if (enabled) current + collapseStatus else current - collapseStatus,
            ))
            return
        }
        val updated = when (option) {
            DisplayOption.STUDENT_NUMBERS -> _settings.value.copy(showStudentNumbers = enabled)
            DisplayOption.CLASS_STUDENT_COUNT -> _settings.value.copy(showClassStudentCount = enabled)
            DisplayOption.CLASS_OPERATION_HINT -> _settings.value.copy(showClassOperationHint = enabled)
            DisplayOption.ROLL_CALL_PROGRESS -> _settings.value.copy(showRollCallProgress = enabled)
            DisplayOption.OPERATION_HINT -> _settings.value.copy(showOperationHint = enabled)
            DisplayOption.STATUS_BUTTON -> _settings.value.copy(showStatusButton = enabled)
            DisplayOption.REASONS_IN_ROLL_CALL -> _settings.value.copy(showReasonsInRollCall = enabled)
            DisplayOption.RESULT_SUMMARY -> _settings.value.copy(showResultSummary = enabled)
            DisplayOption.EMPTY_RESULT_GROUPS -> _settings.value.copy(showEmptyResultGroups = enabled)
            DisplayOption.HISTORY_STATISTICS -> _settings.value.copy(showHistoryStatistics = enabled)
            DisplayOption.CONFIRM_INCOMPLETE -> _settings.value.copy(confirmIncompleteAttendance = enabled)
            DisplayOption.COMPACT_ROLL_CALL -> _settings.value.copy(compactRollCallRows = enabled)
            DisplayOption.EXPORT_LATE -> _settings.value.copy(exportLateStudents = enabled)
            DisplayOption.EXPORT_LEAVE -> _settings.value.copy(exportLeaveStudents = enabled)
            DisplayOption.EXPORT_ABSENT -> _settings.value.copy(exportAbsentStudents = enabled)
            DisplayOption.EXPORT_EXEMPT -> _settings.value.copy(exportExemptStudents = enabled)
            else -> return
        }
        updateSettings(updated)
    }

    fun exportBackup(): String = writer.submit<String> {
        check(_storageError.value == null) { "数据保存失败，请先恢复" }
        JSONObject(checkNotNull(lastConfirmedSnapshot)).toString(2)
    }.get()

    fun awaitSaved(): Boolean = writer.submit<Boolean> { _storageError.value == null }.get()

    fun importBackup(text: String): Boolean = runCatching {
        require(text.length <= MAX_BACKUP_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "备份过大" }
        requireShallowJson(text)
        val root = JSONObject(text)
        validateBackup(root)
        require(root.optInt("formatVersion", 1) == 1) { "不支持的备份版本" }
        val classesJson = root.getJSONArray("classes").toString()
        val sessionsJson = root.getJSONArray("sessions").toString()
        val draftsJson = root.optJSONArray("rollCallDrafts")?.toString() ?: "[]"
        val settingsJson = root.getJSONObject("settings").toString()
        val restoredClasses = parseClasses(classesJson)
        val restoredSessions = parseSessions(sessionsJson)
        val restoredDrafts = parseRollCallDrafts(draftsJson)
        val restoredSettings = parseSettings(settingsJson)
        val normalized = JSONObject().put("formatVersion", 1)
            .put("classes", classesToJson(restoredClasses)).put("sessions", sessionsToJson(restoredSessions))
            .put("rollCallDrafts", rollCallDraftsToJson(restoredDrafts)).put("settings", settingsToJson(restoredSettings))
        val committed = writer.submit<Boolean> { commitSnapshot(normalized.toString()) }.get()
        check(committed) { "备份写入失败，原数据已保留" }
        _classes.value = restoredClasses
        _sessions.value = restoredSessions
        rollCallDrafts.clear()
        rollCallDrafts.putAll(restoredDrafts)
        _settings.value = restoredSettings
        loadFailed = false
        _storageError.value = null
    }.isSuccess

    fun setExportHeader(enabled: Boolean) = updateSettings(_settings.value.copy(exportHeader = enabled))

    fun setExportSummary(enabled: Boolean) = updateSettings(_settings.value.copy(exportSummary = enabled))

    fun setExportPresentStudents(enabled: Boolean) =
        updateSettings(_settings.value.copy(exportPresentStudents = enabled))

    fun setExportStudentNumber(enabled: Boolean) =
        updateSettings(_settings.value.copy(exportStudentNumber = enabled))

    fun setExportReason(enabled: Boolean) = updateSettings(_settings.value.copy(exportReason = enabled))

    private fun updateClass(classId: String, transform: (ClassGroup) -> ClassGroup) {
        _classes.value = _classes.value.map { group ->
            if (group.id == classId) transform(group) else group
        }
        saveClasses()
    }

    private fun classesToJson(groups: List<ClassGroup> = _classes.value): JSONArray {
        val array = JSONArray()
        groups.forEach { group ->
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
                    .put("students", students)
                    .put("attendanceSettings", group.attendanceSettings?.toJson() ?: JSONObject.NULL),
            )
        }
        return array
    }

    private fun sessionsToJson(sessions: List<AttendanceSession> = _sessions.value): JSONArray {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("classId", session.classId)
                    .put("createdAt", session.createdAt)
                    .put("entries", attendanceEntriesToJson(session.entries)),
            )
        }
        return array
    }

    private fun snapshot(
        groups: List<ClassGroup> = _classes.value,
        sessions: List<AttendanceSession> = _sessions.value,
        drafts: Map<String, List<AttendanceEntry>> = rollCallDrafts,
        settings: AppSettings = _settings.value,
    ): String = JSONObject().put("formatVersion", 1)
        .put("classes", classesToJson(groups)).put("sessions", sessionsToJson(sessions))
        .put("rollCallDrafts", rollCallDraftsToJson(drafts)).put("settings", settingsToJson(settings)).toString()

    private fun saveClasses() = persist()
    private fun saveSessions() = persist()
    private fun saveRollCallDrafts() = persist()

    private fun persist() {
        if (loadFailed || _storageError.value != null) return
        val groups = _classes.value
        val sessions = _sessions.value
        val drafts = rollCallDrafts.toMap()
        val settings = _settings.value
        writer.execute {
            if (_storageError.value == null && !runCatching {
                commitSnapshot(snapshot(groups, sessions, drafts, settings))
            }.getOrDefault(false)) {
                loadFailed = true
                _storageError.value = "数据未能写入设备。请恢复上次保存的数据后继续。"
            }
        }
    }

    private fun commitSnapshot(raw: String): Boolean = runCatching {
        require(raw.length <= MAX_BACKUP_BYTES && raw.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES)
        val root = JSONObject(raw)
        validateBackup(root)
        val previous = lastConfirmedSnapshot
        val editor = preferences.edit()
            .putString(KEY_CLASSES, root.getJSONArray("classes").toString())
            .putString(KEY_SESSIONS, root.getJSONArray("sessions").toString())
            .putString(KEY_ROLL_CALL_DRAFTS, root.optJSONArray("rollCallDrafts")?.toString() ?: "[]")
            .putString(KEY_SETTINGS, root.getJSONObject("settings").toString())
            .putString("verified_snapshot", raw)
        if (previous != null) editor.putString("rollback_snapshot", previous)
        editor.commit().also { if (it) lastConfirmedSnapshot = raw }
    }.getOrDefault(false)

    fun recoverPrevious(): Boolean {
        return listOfNotNull(lastConfirmedSnapshot, preferences.getString("rollback_snapshot", null))
            .distinct().any { importBackup(it) }
    }

    fun exportRecoveryData(): String = JSONObject().apply {
        preferences.all.forEach { (key, value) -> put(key, value) }
    }.toString(2)

    private fun rollCallDraftsToJson(drafts: Map<String, List<AttendanceEntry>> = rollCallDrafts): JSONArray = JSONArray().apply {
        drafts.forEach { (classId, entries) ->
            put(
                JSONObject()
                    .put("classId", classId)
                    .put("entries", attendanceEntriesToJson(entries)),
            )
        }
    }

    private fun attendanceEntriesToJson(entries: List<AttendanceEntry>): JSONArray = JSONArray().apply {
        entries.forEach { entry ->
            put(
                JSONObject()
                    .put("studentId", entry.studentId)
                    .put("studentName", entry.studentName)
                    .put("studentNumber", entry.studentNumber)
                    .put("status", entry.status.name)
                    .put("reason", entry.reason),
            )
        }
    }

    private fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        persist()
    }

    private fun settingsToJson(settings: AppSettings): JSONObject {
        val reasons = JSONArray().apply { settings.absenceReasons.forEach { put(it) } }
        return JSONObject()
            .put("absenceReasons", reasons)
            .put("defaultReason", settings.defaultReason)
            .put("defaultStatus", settings.defaultStatus.name)
            .put("longPressAction", settings.longPressAction.name)
            .put("swipeLeftAction", settings.swipeLeftAction.name)
            .put("swipeRightAction", settings.swipeRightAction.name)
            .put("presentIcon", settings.presentIcon.name)
            .put("lateIcon", settings.lateIcon.name)
            .put("leaveIcon", settings.leaveIcon.name)
            .put("absentIcon", settings.absentIcon.name)
            .put("exemptIcon", settings.exemptIcon.name)
            .put("presentColor", settings.presentColor.name)
            .put("lateColor", settings.lateColor.name)
            .put("leaveColor", settings.leaveColor.name)
            .put("absentColor", settings.absentColor.name)
            .put("exemptColor", settings.exemptColor.name)
            .put("groupResultsByStatus", settings.groupResultsByStatus)
            .put("collapsedResultStatuses", JSONArray(settings.collapsedResultStatuses.map { it.name }))
            .put("historyTitleMode", settings.historyTitleMode.name)
            .put("showStudentNumbers", settings.showStudentNumbers)
            .put("showClassStudentCount", settings.showClassStudentCount)
            .put("showClassOperationHint", settings.showClassOperationHint)
            .put("showRollCallProgress", settings.showRollCallProgress)
            .put("showOperationHint", settings.showOperationHint)
            .put("showStatusButton", settings.showStatusButton)
            .put("showReasonsInRollCall", settings.showReasonsInRollCall)
            .put("showResultSummary", settings.showResultSummary)
            .put("showEmptyResultGroups", settings.showEmptyResultGroups)
            .put("showHistoryStatistics", settings.showHistoryStatistics)
            .put("confirmIncompleteAttendance", settings.confirmIncompleteAttendance)
            .put("compactRollCallRows", settings.compactRollCallRows)
            .put("exportHeader", settings.exportHeader)
            .put("exportSummary", settings.exportSummary)
            .put("exportPresentStudents", settings.exportPresentStudents)
            .put("exportLateStudents", settings.exportLateStudents)
            .put("exportLeaveStudents", settings.exportLeaveStudents)
            .put("exportAbsentStudents", settings.exportAbsentStudents)
            .put("exportExemptStudents", settings.exportExemptStudents)
            .put("exportStudentNumber", settings.exportStudentNumber)
            .put("exportReason", settings.exportReason)
    }

    private fun loadClasses(): List<ClassGroup> = runCatching {
        parseClasses(preferences.getString(KEY_CLASSES, "[]").orEmpty())
    }.onFailure { loadFailed = true; _storageError.value = "本地数据损坏，已停止自动保存。请导出原始数据或恢复备份。" }.getOrDefault(emptyList())

    private fun parseClasses(raw: String): List<ClassGroup> {
        val array = JSONArray(raw)
        return buildList {
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
                add(
                    ClassGroup(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        students = students,
                        attendanceSettings = item.optJSONObject("attendanceSettings")?.toClassAttendanceSettings(),
                    ),
                )
            }
        }
    }

    private fun loadSessions(): List<AttendanceSession> = runCatching {
        parseSessions(preferences.getString(KEY_SESSIONS, "[]").orEmpty())
    }.onFailure { loadFailed = true; _storageError.value = "本地数据损坏，已停止自动保存。请导出原始数据或恢复备份。" }.getOrDefault(emptyList())

    private fun parseSessions(raw: String): List<AttendanceSession> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val entries = parseAttendanceEntries(item.optJSONArray("entries") ?: JSONArray())
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
    }

    private fun loadRollCallDrafts(): Map<String, List<AttendanceEntry>> = runCatching {
        parseRollCallDrafts(preferences.getString(KEY_ROLL_CALL_DRAFTS, "[]").orEmpty())
    }.onFailure { loadFailed = true; _storageError.value = "本地草稿损坏，已停止自动保存。" }.getOrDefault(emptyMap())

    private fun parseRollCallDrafts(raw: String): Map<String, List<AttendanceEntry>> {
        val array = JSONArray(raw)
        return buildMap {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                put(
                    item.getString("classId"),
                    parseAttendanceEntries(item.optJSONArray("entries") ?: JSONArray()),
                )
            }
        }
    }

    private fun parseAttendanceEntries(array: JSONArray): List<AttendanceEntry> = buildList {
        for (index in 0 until array.length()) {
            val entry = array.getJSONObject(index)
            add(
                AttendanceEntry(
                    studentId = entry.getString("studentId"),
                    studentName = entry.getString("studentName"),
                    studentNumber = entry.optString("studentNumber"),
                    status = AttendanceStatus.valueOf(entry.getString("status")).also {
                        require(it != AttendanceStatus.UNMARKED) { "记录含无效点名状态" }
                    },
                    reason = entry.optString("reason"),
                ),
            )
        }
    }

    private fun loadSettings(): AppSettings = runCatching {
        parseSettings(preferences.getString(KEY_SETTINGS, null))
    }.onFailure { loadFailed = true; _storageError.value = "本地设置损坏，已停止自动保存。" }.getOrDefault(AppSettings())

    private fun parseSettings(raw: String?): AppSettings {
        val defaults = AppSettings()
        if (raw == null) return defaults
        val json = JSONObject(raw)
        val reasonsArray = json.optJSONArray("absenceReasons")
        val reasons = if (reasonsArray == null) {
            defaults.absenceReasons
        } else {
            buildList {
                for (index in 0 until reasonsArray.length()) {
                    reasonsArray.optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
                }
            }
        }
        require(!json.has("defaultStatus") || json.getString("defaultStatus") in AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.map { it.name })
        return defaults.copy(
            absenceReasons = reasons.distinct(),
            defaultReason = json.optString("defaultReason").takeIf { it in reasons }.orEmpty(),
            defaultStatus = enumValueOrDefault(json.optString("defaultStatus"), defaults.defaultStatus),
            longPressAction = enumValueOrDefault(json.optString("longPressAction"), defaults.longPressAction),
            swipeLeftAction = enumValueOrDefault(json.optString("swipeLeftAction"), defaults.swipeLeftAction),
            swipeRightAction = enumValueOrDefault(json.optString("swipeRightAction"), defaults.swipeRightAction),
            presentIcon = enumValueOrDefault(json.optString("presentIcon"), defaults.presentIcon),
            lateIcon = enumValueOrDefault(json.optString("lateIcon"), defaults.lateIcon),
            leaveIcon = enumValueOrDefault(json.optString("leaveIcon"), defaults.leaveIcon),
            absentIcon = enumValueOrDefault(json.optString("absentIcon"), defaults.absentIcon),
            exemptIcon = enumValueOrDefault(json.optString("exemptIcon"), defaults.exemptIcon),
            presentColor = enumValueOrDefault(json.optString("presentColor"), defaults.presentColor),
            lateColor = enumValueOrDefault(json.optString("lateColor"), defaults.lateColor),
            leaveColor = enumValueOrDefault(json.optString("leaveColor"), defaults.leaveColor),
            absentColor = enumValueOrDefault(json.optString("absentColor"), defaults.absentColor),
            exemptColor = enumValueOrDefault(json.optString("exemptColor"), defaults.exemptColor),
            groupResultsByStatus = json.optBoolean("groupResultsByStatus", defaults.groupResultsByStatus),
            collapsedResultStatuses = json.readCollapsedResultStatuses(),
            historyTitleMode = enumValueOrDefault(json.optString("historyTitleMode"), defaults.historyTitleMode),
            showStudentNumbers = json.optBoolean("showStudentNumbers", defaults.showStudentNumbers),
            showClassStudentCount = json.optBoolean("showClassStudentCount", defaults.showClassStudentCount),
            showClassOperationHint = json.optBoolean("showClassOperationHint", defaults.showClassOperationHint),
            showRollCallProgress = json.optBoolean("showRollCallProgress", defaults.showRollCallProgress),
            showOperationHint = json.optBoolean("showOperationHint", defaults.showOperationHint),
            showStatusButton = json.optBoolean("showStatusButton", defaults.showStatusButton),
            showReasonsInRollCall = json.optBoolean("showReasonsInRollCall", defaults.showReasonsInRollCall),
            showResultSummary = json.optBoolean("showResultSummary", defaults.showResultSummary),
            showEmptyResultGroups = json.optBoolean("showEmptyResultGroups", defaults.showEmptyResultGroups),
            showHistoryStatistics = json.optBoolean("showHistoryStatistics", defaults.showHistoryStatistics),
            confirmIncompleteAttendance = json.optBoolean(
                "confirmIncompleteAttendance",
                defaults.confirmIncompleteAttendance,
            ),
            compactRollCallRows = json.optBoolean("compactRollCallRows", defaults.compactRollCallRows),
            exportHeader = json.optBoolean("exportHeader", defaults.exportHeader),
            exportSummary = json.optBoolean("exportSummary", defaults.exportSummary),
            exportPresentStudents = json.optBoolean("exportPresentStudents", defaults.exportPresentStudents),
            exportLateStudents = json.optBoolean("exportLateStudents", defaults.exportLateStudents),
            exportLeaveStudents = json.optBoolean("exportLeaveStudents", defaults.exportLeaveStudents),
            exportAbsentStudents = json.optBoolean("exportAbsentStudents", defaults.exportAbsentStudents),
            exportExemptStudents = json.optBoolean("exportExemptStudents", defaults.exportExemptStudents),
            exportStudentNumber = json.optBoolean("exportStudentNumber", defaults.exportStudentNumber),
            exportReason = json.optBoolean("exportReason", defaults.exportReason),
        )
    }

    companion object {
        @Volatile private var instance: AttendanceRepository? = null

        fun getInstance(context: Context): AttendanceRepository = instance ?: synchronized(this) {
            instance ?: AttendanceRepository(context.applicationContext.getSharedPreferences("attendance_data", Context.MODE_PRIVATE))
                .also { instance = it }
        }

        private const val KEY_CLASSES = "classes"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_ROLL_CALL_DRAFTS = "roll_call_drafts"
        private const val KEY_SETTINGS = "settings"

        private fun newId(): String = UUID.randomUUID().toString()
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

private fun JSONObject.readCollapsedResultStatuses(): Set<AttendanceStatus> {
    val array = optJSONArray("collapsedResultStatuses") ?: return emptySet()
    return buildSet {
        for (index in 0 until array.length()) {
            val status = runCatching { AttendanceStatus.valueOf(array.optString(index)) }.getOrNull()
            if (status != null && status != AttendanceStatus.UNMARKED) add(status)
        }
    }
}

private fun ClassAttendanceSettings.toJson(): JSONObject = JSONObject()
    .put("defaultReason", defaultReason)
    .put("defaultStatus", defaultStatus.name)
    .put("longPressAction", longPressAction.name)
    .put("swipeLeftAction", swipeLeftAction.name)
    .put("swipeRightAction", swipeRightAction.name)
    .put("groupResultsByStatus", groupResultsByStatus)
    .put("collapsedResultStatuses", JSONArray(collapsedResultStatuses.map { it.name }))
    .put("showStudentNumbers", showStudentNumbers)
    .put("showRollCallProgress", showRollCallProgress)
    .put("showOperationHint", showOperationHint)
    .put("showStatusButton", showStatusButton)
    .put("showReasonsInRollCall", showReasonsInRollCall)
    .put("showResultSummary", showResultSummary)
    .put("showEmptyResultGroups", showEmptyResultGroups)
    .put("confirmIncompleteAttendance", confirmIncompleteAttendance)
    .put("compactRollCallRows", compactRollCallRows)

private fun JSONObject.toClassAttendanceSettings(): ClassAttendanceSettings {
    val defaults = ClassAttendanceSettings()
    return defaults.copy(
        defaultReason = optString("defaultReason"),
        defaultStatus = enumValueOrDefault(optString("defaultStatus"), defaults.defaultStatus),
        longPressAction = enumValueOrDefault(optString("longPressAction"), defaults.longPressAction),
        swipeLeftAction = enumValueOrDefault(optString("swipeLeftAction"), defaults.swipeLeftAction),
        swipeRightAction = enumValueOrDefault(optString("swipeRightAction"), defaults.swipeRightAction),
        groupResultsByStatus = optBoolean("groupResultsByStatus", defaults.groupResultsByStatus),
        collapsedResultStatuses = readCollapsedResultStatuses(),
        showStudentNumbers = optBoolean("showStudentNumbers", defaults.showStudentNumbers),
        showRollCallProgress = optBoolean("showRollCallProgress", defaults.showRollCallProgress),
        showOperationHint = optBoolean("showOperationHint", defaults.showOperationHint),
        showStatusButton = optBoolean("showStatusButton", defaults.showStatusButton),
        showReasonsInRollCall = optBoolean("showReasonsInRollCall", defaults.showReasonsInRollCall),
        showResultSummary = optBoolean("showResultSummary", defaults.showResultSummary),
        showEmptyResultGroups = optBoolean("showEmptyResultGroups", defaults.showEmptyResultGroups),
        confirmIncompleteAttendance = optBoolean(
            "confirmIncompleteAttendance",
            defaults.confirmIncompleteAttendance,
        ),
        compactRollCallRows = optBoolean("compactRollCallRows", defaults.compactRollCallRows),
    )
}

fun parseStudentList(text: String): List<ImportedStudent> {
    require(text.length <= MAX_ROSTER_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_ROSTER_BYTES) { "名单过大" }
    val raw = text.removePrefix("\uFEFF")
    val firstLine = raw.lineSequence().firstOrNull().orEmpty()
    val delimiter = listOf(',', '\t', ';').maxBy { candidate -> firstLine.count { it == candidate } }
    val rows = parseRosterRecords(raw, delimiter)
    if (rows.isEmpty()) return emptyList()
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
        requireSafeField(name, "姓名")
        requireSafeField(number, "学号", required = false)
        ImportedStudent(name.trim(), number.trim())
    }
}

private fun String.looksLikeStudentNumber(): Boolean =
    isNotBlank() && all { it.isDigit() || it == '-' || it == '_' }

private fun studentKey(name: String, number: String): String =
    if (number.isNotBlank()) "number:" + number.trim().lowercase(java.util.Locale.ROOT)
    else "name:" + name.trim().lowercase(java.util.Locale.ROOT)
