package com.ulinoyaped.attendance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ulinoyaped.attendance.data.AttendanceEntry
import com.ulinoyaped.attendance.data.AttendanceRepository
import com.ulinoyaped.attendance.data.AttendanceSession
import com.ulinoyaped.attendance.data.AttendanceStatus
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.GestureAction
import com.ulinoyaped.attendance.data.Student
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Screen {
    data class Root(val tab: RootTab) : Screen
    data class ClassDetail(val classId: String) : Screen
    data class RollCall(val classId: String) : Screen
    data class Result(val classId: String, val sessionId: String, val backToHistory: Boolean = false) : Screen
}

private enum class RootTab(val label: String, val shortLabel: String) {
    CLASSES("班级", "班"),
    HISTORY("历史", "史"),
    SETTINGS("设置", "设"),
}

private data class Mark(
    val status: AttendanceStatus,
    val reason: String = "",
)

private enum class SettingSelector {
    DEFAULT_STATUS,
    DEFAULT_REASON,
    LONG_PRESS,
    SWIPE_LEFT,
    SWIPE_RIGHT,
}

@Composable
fun AttendanceApp() {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository(context.applicationContext) }
    val classes by repository.classes.collectAsStateWithLifecycle()
    val sessions by repository.sessions.collectAsStateWithLifecycle()
    val settings by repository.settings.collectAsStateWithLifecycle()
    var screen: Screen by remember { mutableStateOf(Screen.Root(RootTab.CLASSES)) }

    when (val current = screen) {
        is Screen.Root -> RootScreen(
            selectedTab = current.tab,
            classes = classes,
            sessions = sessions,
            settings = settings,
            onSelectTab = { screen = Screen.Root(it) },
            onAddClass = repository::addClass,
            onOpenClass = { screen = Screen.ClassDetail(it) },
            onDeleteClass = repository::deleteClass,
            onOpenResult = { classId, sessionId -> screen = Screen.Result(classId, sessionId, true) },
            onAddReason = repository::addAbsenceReason,
            onRemoveReason = repository::removeAbsenceReason,
            onSetDefaultReason = repository::setDefaultReason,
            onSetDefaultStatus = repository::setDefaultStatus,
            onSetLongPressAction = repository::setLongPressAction,
            onSetSwipeLeftAction = repository::setSwipeLeftAction,
            onSetSwipeRightAction = repository::setSwipeRightAction,
        )

        is Screen.ClassDetail -> {
            val group = classes.firstOrNull { it.id == current.classId }
            if (group == null) {
                screen = Screen.Root(RootTab.CLASSES)
            } else {
                ClassDetailScreen(
                    group = group,
                    sessions = sessions.filter { it.classId == group.id },
                    onBack = { screen = Screen.Root(RootTab.CLASSES) },
                    onAddStudent = { name, number -> repository.addStudent(group.id, name, number) },
                    onRemoveStudent = { repository.removeStudent(group.id, it) },
                    onImport = { repository.importStudents(group.id, it) },
                    onStart = { screen = Screen.RollCall(group.id) },
                    onOpenResult = { screen = Screen.Result(group.id, it) },
                )
            }
        }

        is Screen.RollCall -> {
            val group = classes.firstOrNull { it.id == current.classId }
            if (group == null) {
                screen = Screen.Root(RootTab.CLASSES)
            } else {
                RollCallScreen(
                    group = group,
                    settings = settings,
                    onBack = { screen = Screen.ClassDetail(group.id) },
                    onFinish = { entries ->
                        val sessionId = repository.saveSession(group.id, entries)
                        screen = Screen.Result(group.id, sessionId)
                    },
                )
            }
        }

        is Screen.Result -> {
            val group = classes.firstOrNull { it.id == current.classId }
            val session = sessions.firstOrNull { it.id == current.sessionId }
            if (group == null || session == null) {
                screen = Screen.Root(RootTab.HISTORY)
            } else {
                ResultScreen(
                    group = group,
                    session = session,
                    onBack = {
                        screen = if (current.backToHistory) {
                            Screen.Root(RootTab.HISTORY)
                        } else {
                            Screen.ClassDetail(group.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RootScreen(
    selectedTab: RootTab,
    classes: List<ClassGroup>,
    sessions: List<AttendanceSession>,
    settings: AppSettings,
    onSelectTab: (RootTab) -> Unit,
    onAddClass: (String) -> Unit,
    onOpenClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit,
    onOpenResult: (String, String) -> Unit,
    onAddReason: (String) -> Unit,
    onRemoveReason: (String) -> Unit,
    onSetDefaultReason: (String) -> Unit,
    onSetDefaultStatus: (AttendanceStatus) -> Unit,
    onSetLongPressAction: (GestureAction) -> Unit,
    onSetSwipeLeftAction: (GestureAction) -> Unit,
    onSetSwipeRightAction: (GestureAction) -> Unit,
) {
    val bottomBar: @Composable () -> Unit = {
        RootNavigationBar(selectedTab = selectedTab, onSelectTab = onSelectTab)
    }
    when (selectedTab) {
        RootTab.CLASSES -> ClassesScreen(
            classes = classes,
            onAddClass = onAddClass,
            onOpenClass = onOpenClass,
            onDeleteClass = onDeleteClass,
            bottomBar = bottomBar,
        )
        RootTab.HISTORY -> HistoryScreen(
            classes = classes,
            sessions = sessions,
            onOpenResult = onOpenResult,
            bottomBar = bottomBar,
        )
        RootTab.SETTINGS -> SettingsScreen(
            settings = settings,
            onAddReason = onAddReason,
            onRemoveReason = onRemoveReason,
            onSetDefaultReason = onSetDefaultReason,
            onSetDefaultStatus = onSetDefaultStatus,
            onSetLongPressAction = onSetLongPressAction,
            onSetSwipeLeftAction = onSetSwipeLeftAction,
            onSetSwipeRightAction = onSetSwipeRightAction,
            bottomBar = bottomBar,
        )
    }
}

@Composable
private fun RootNavigationBar(selectedTab: RootTab, onSelectTab: (RootTab) -> Unit) {
    NavigationBar {
        RootTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Text(tab.shortLabel, fontWeight = FontWeight.Bold) },
                label = { Text(tab.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassesScreen(
    classes: List<ClassGroup>,
    onAddClass: (String) -> Unit,
    onOpenClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var classToDelete by remember { mutableStateOf<ClassGroup?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的班级") }) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", fontSize = 28.sp)
            }
        },
    ) { padding ->
        if (classes.isEmpty()) {
            EmptyState(
                title = "还没有班级",
                description = "创建班级后即可导入名单并开始点名",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(classes, key = { it.id }) { group ->
                    Card(
                        onClick = { onOpenClass(group.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp, 12.dp, 8.dp, 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${group.students.size} 名学生",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { classToDelete = group }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TextInputDialog(
            title = "创建班级",
            label = "班级名称",
            confirmText = "创建",
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddClass(it)
                showAddDialog = false
            },
        )
    }

    classToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("删除 ${group.name}？") },
            text = { Text("班级、名单和所有历史点名记录都会删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClass(group.id)
                        classToDelete = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassDetailScreen(
    group: ClassGroup,
    sessions: List<AttendanceSession>,
    onBack: () -> Unit,
    onAddStudent: (String, String) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onImport: (String) -> Int,
    onStart: () -> Unit,
    onOpenResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddStudent by remember { mutableStateOf(false) }
    var showImportOptions by remember { mutableStateOf(false) }
    var showTextImport by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取文件")
            }
            scope.launch {
                result.onSuccess { text ->
                    val count = onImport(text)
                    snackbarHostState.showSnackbar(if (count > 0) "已导入 $count 名学生" else "没有可导入的新学生")
                }.onFailure {
                    snackbarHostState.showSnackbar("名单读取失败")
                }
            }
        }
    }

    Scaffold(
        topBar = { SimpleBackBar(group.name, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Button(
                    onClick = onStart,
                    enabled = group.students.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (group.students.isEmpty()) "添加学生后开始点名" else "开始点名")
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showAddStudent = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("手动添加") }
                    OutlinedButton(
                        onClick = { showImportOptions = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("导入名单") }
                }
            }
            item { SectionTitle("学生名单 · ${group.students.size}") }
            if (group.students.isEmpty()) {
                item {
                    HintCard("支持 CSV/TXT；推荐表头为“学号,姓名”。重复学号会自动跳过。")
                }
            } else {
                items(group.students, key = { it.id }) { student ->
                    StudentListItem(student = student, onDelete = { studentToDelete = student })
                }
            }
            if (sessions.isNotEmpty()) {
                item { SectionTitle("历史记录") }
                items(sessions, key = { it.id }) { session ->
                    HistoryItem(session = session, onClick = { onOpenResult(session.id) })
                }
            }
        }
    }

    if (showAddStudent) {
        AddStudentDialog(
            onDismiss = { showAddStudent = false },
            onConfirm = { name, number ->
                onAddStudent(name, number)
                showAddStudent = false
            },
        )
    }

    if (showImportOptions) {
        AlertDialog(
            onDismissRequest = { showImportOptions = false },
            title = { Text("导入名单") },
            text = { Text("可以选择 CSV/TXT 文件，也可以直接粘贴名单文本。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportOptions = false
                        showTextImport = true
                    },
                ) { Text("粘贴文本") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportOptions = false
                        importLauncher.launch(
                            arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel"),
                        )
                    },
                ) { Text("选择文件") }
            },
        )
    }

    if (showTextImport) {
        TextImportDialog(
            onDismiss = { showTextImport = false },
            onConfirm = { text ->
                val count = onImport(text)
                showTextImport = false
                scope.launch {
                    snackbarHostState.showSnackbar(if (count > 0) "已导入 $count 名学生" else "没有可导入的新学生")
                }
            },
        )
    }

    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("移除 ${student.name}？") },
            text = { Text("既有点名记录不会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveStudent(student.id)
                        studentToDelete = null
                    },
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RollCallScreen(
    group: ClassGroup,
    settings: AppSettings,
    onBack: () -> Unit,
    onFinish: (List<AttendanceEntry>) -> Unit,
) {
    val marks = remember(group.id) { mutableStateMapOf<String, Mark>() }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val checked = group.students.count { marks[it.id]?.status != null }

    fun applyAction(student: Student, action: GestureAction) {
        when (action) {
            GestureAction.EDIT -> editingStudent = student
            GestureAction.PRESENT -> marks[student.id] = Mark(AttendanceStatus.PRESENT)
            GestureAction.LATE -> marks[student.id] = Mark(AttendanceStatus.LATE, settings.defaultReason)
            GestureAction.LEAVE -> marks[student.id] = Mark(AttendanceStatus.LEAVE, settings.defaultReason)
            GestureAction.ABSENT -> marks[student.id] = Mark(AttendanceStatus.ABSENT, settings.defaultReason)
            GestureAction.CLEAR -> marks.remove(student.id)
        }
    }

    fun finish() {
        val entries = group.students.map { student ->
            val mark = marks[student.id] ?: Mark(AttendanceStatus.ABSENT, settings.defaultReason)
            AttendanceEntry(
                studentId = student.id,
                studentName = student.name,
                studentNumber = student.studentNumber,
                status = mark.status,
                reason = mark.reason,
            )
        }
        onFinish(entries)
    }

    Scaffold(
        topBar = { SimpleBackBar("${group.name} · 点名", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        if (checked < group.students.size) showFinishDialog = true else finish()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                ) { Text("结束并查看结果") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        "$checked / ${group.students.size} 已处理",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "点按：${settings.defaultStatus.label} · 长按：${settings.longPressAction.label} · 左滑：${settings.swipeLeftAction.label} · 右滑：${settings.swipeRightAction.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(group.students, key = { it.id }) { student ->
                val mark = marks[student.id]
                RollCallItem(
                    student = student,
                    mark = mark,
                    onTogglePresent = {
                        if (mark?.status == settings.defaultStatus) {
                            marks.remove(student.id)
                        } else {
                            marks[student.id] = Mark(
                                settings.defaultStatus,
                                settings.defaultReason.takeIf { settings.defaultStatus != AttendanceStatus.PRESENT }.orEmpty(),
                            )
                        }
                    },
                    onLongPress = { applyAction(student, settings.longPressAction) },
                    onSwipeLeft = { applyAction(student, settings.swipeLeftAction) },
                    onSwipeRight = { applyAction(student, settings.swipeRightAction) },
                    swipeLeftLabel = settings.swipeLeftAction.label,
                    swipeRightLabel = settings.swipeRightAction.label,
                    onEdit = { editingStudent = student },
                )
            }
        }
    }

    editingStudent?.let { student ->
        StatusDialog(
            student = student,
            initial = marks[student.id] ?: Mark(
                settings.defaultStatus,
                settings.defaultReason.takeIf { settings.defaultStatus != AttendanceStatus.PRESENT }.orEmpty(),
            ),
            presetReasons = settings.absenceReasons,
            defaultReason = settings.defaultReason,
            onDismiss = { editingStudent = null },
            onConfirm = { mark ->
                marks[student.id] = mark
                editingStudent = null
            },
        )
    }

    if (showFinishDialog) {
        val remaining = group.students.size - checked
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("还有 $remaining 人未点") },
            text = { Text("继续结束后，未标记的学生将自动记为缺勤。") },
            confirmButton = {
                TextButton(onClick = { showFinishDialog = false; finish() }) { Text("记为缺勤并结束") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("继续点名") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(
    group: ClassGroup,
    session: AttendanceSession,
    onBack: () -> Unit,
) {
    val counts = AttendanceStatus.entries.associateWith { status ->
        session.entries.count { it.status == status }
    }
    val exceptional = session.entries.filter { it.status != AttendanceStatus.PRESENT }

    Scaffold(topBar = { SimpleBackBar("点名结果", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(group.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    formatTime(session.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard("到", counts.getValue(AttendanceStatus.PRESENT), Modifier.weight(1f))
                    SummaryCard("迟到", counts.getValue(AttendanceStatus.LATE), Modifier.weight(1f))
                    SummaryCard("请假", counts.getValue(AttendanceStatus.LEAVE), Modifier.weight(1f))
                    SummaryCard("缺勤", counts.getValue(AttendanceStatus.ABSENT), Modifier.weight(1f))
                }
            }
            item { SectionTitle("异常情况") }
            if (exceptional.isEmpty()) {
                item { HintCard("全员到齐") }
            } else {
                items(exceptional, key = { it.studentId }) { entry ->
                    ResultEntryItem(entry)
                }
            }
            item { SectionTitle("全部学生 · ${session.entries.size}") }
            items(session.entries, key = { "all-${it.studentId}" }) { entry ->
                CompactResultItem(entry)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    classes: List<ClassGroup>,
    sessions: List<AttendanceSession>,
    onOpenResult: (String, String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val classNames = classes.associate { it.id to it.name }
    Scaffold(
        topBar = { TopAppBar(title = { Text("点名历史") }) },
        bottomBar = bottomBar,
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(
                title = "还没有点名记录",
                description = "完成一次点名后，结果会显示在这里",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(sessions.sortedByDescending { it.createdAt }, key = { it.id }) { session ->
                    GlobalHistoryItem(
                        className = classNames[session.classId].orEmpty(),
                        session = session,
                        onClick = { onOpenResult(session.classId, session.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onAddReason: (String) -> Unit,
    onRemoveReason: (String) -> Unit,
    onSetDefaultReason: (String) -> Unit,
    onSetDefaultStatus: (AttendanceStatus) -> Unit,
    onSetLongPressAction: (GestureAction) -> Unit,
    onSetSwipeLeftAction: (GestureAction) -> Unit,
    onSetSwipeRightAction: (GestureAction) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showAddReason by remember { mutableStateOf(false) }
    var selector by remember { mutableStateOf<SettingSelector?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle("点名操作") }
            item {
                SettingsGroup {
                    SettingRow(
                        title = "点按默认选择",
                        value = settings.defaultStatus.label,
                        onClick = { selector = SettingSelector.DEFAULT_STATUS },
                    )
                    HorizontalDivider()
                    SettingRow(
                        title = "默认未到原因",
                        value = settings.defaultReason.ifBlank { "不预填" },
                        onClick = { selector = SettingSelector.DEFAULT_REASON },
                    )
                    HorizontalDivider()
                    SettingRow(
                        title = "长按姓名",
                        value = settings.longPressAction.label,
                        onClick = { selector = SettingSelector.LONG_PRESS },
                    )
                    HorizontalDivider()
                    SettingRow(
                        title = "向左滑动",
                        value = settings.swipeLeftAction.label,
                        onClick = { selector = SettingSelector.SWIPE_LEFT },
                    )
                    HorizontalDivider()
                    SettingRow(
                        title = "向右滑动",
                        value = settings.swipeRightAction.label,
                        onClick = { selector = SettingSelector.SWIPE_RIGHT },
                    )
                }
            }
            item {
                Text(
                    "再次点按相同的默认状态会清除标记。滑动操作完成后卡片会自动回位。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("常用未到原因")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showAddReason = true }) { Text("添加") }
                }
            }
            if (settings.absenceReasons.isEmpty()) {
                item { HintCard("暂未设置常用原因。点名时仍可手动输入原因。") }
            } else {
                items(settings.absenceReasons, key = { it }) { reason ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(reason, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRemoveReason(reason) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    if (showAddReason) {
        TextInputDialog(
            title = "添加常用原因",
            label = "原因",
            confirmText = "添加",
            onDismiss = { showAddReason = false },
            onConfirm = {
                onAddReason(it)
                showAddReason = false
            },
        )
    }

    selector?.let { selected ->
        when (selected) {
            SettingSelector.DEFAULT_STATUS -> StatusChoiceDialog(
                title = "点按默认选择",
                selected = settings.defaultStatus,
                onDismiss = { selector = null },
                onSelect = { onSetDefaultStatus(it); selector = null },
            )
            SettingSelector.DEFAULT_REASON -> ReasonChoiceDialog(
                reasons = settings.absenceReasons,
                selected = settings.defaultReason,
                onDismiss = { selector = null },
                onSelect = { onSetDefaultReason(it); selector = null },
            )
            SettingSelector.LONG_PRESS -> GestureChoiceDialog(
                title = "长按姓名",
                selected = settings.longPressAction,
                onDismiss = { selector = null },
                onSelect = { onSetLongPressAction(it); selector = null },
            )
            SettingSelector.SWIPE_LEFT -> GestureChoiceDialog(
                title = "向左滑动",
                selected = settings.swipeLeftAction,
                onDismiss = { selector = null },
                onSelect = { onSetSwipeLeftAction(it); selector = null },
            )
            SettingSelector.SWIPE_RIGHT -> GestureChoiceDialog(
                title = "向右滑动",
                selected = settings.swipeRightAction,
                onDismiss = { selector = null },
                onSelect = { onSetSwipeRightAction(it); selector = null },
            )
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, color = MaterialTheme.colorScheme.primary)
        Text("  ›", fontSize = 22.sp)
    }
}

@Composable
private fun StatusChoiceDialog(
    title: String,
    selected: AttendanceStatus,
    onDismiss: () -> Unit,
    onSelect: (AttendanceStatus) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                AttendanceStatus.entries.filterNot { it == AttendanceStatus.UNMARKED }.forEach { status ->
                    ChoiceRow(status.label, status == selected) { onSelect(status) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ReasonChoiceDialog(
    reasons: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认未到原因") },
        text = {
            Column {
                ChoiceRow("不预填", selected.isEmpty()) { onSelect("") }
                reasons.forEach { reason ->
                    ChoiceRow(reason, reason == selected) { onSelect(reason) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun GestureChoiceDialog(
    title: String,
    selected: GestureAction,
    onDismiss: () -> Unit,
    onSelect: (GestureAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                GestureAction.entries.forEach { action ->
                    ChoiceRow(action.label, action == selected) { onSelect(action) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RollCallItem(
    student: Student,
    mark: Mark?,
    onTogglePresent: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeLeftLabel: String,
    swipeRightLabel: String,
    onEdit: () -> Unit,
) {
    val marked = mark != null
    val container = when (mark?.status) {
        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
        AttendanceStatus.LATE -> Color(0xFFFFE0B2)
        AttendanceStatus.LEAVE -> Color(0xFFE1E2EC)
        AttendanceStatus.ABSENT -> Color(0xFFFFDAD6)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight()
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            val direction = swipeState.dismissDirection
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(if (direction == SwipeToDismissBoxValue.StartToEnd) swipeRightLabel else swipeLeftLabel)
            }
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(onClick = onTogglePresent, onLongClick = onLongPress),
            colors = CardDefaults.cardColors(containerColor = container),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (marked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (marked) "✓" else "",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium)
                    val detail = listOfNotNull(
                        student.studentNumber.takeIf(String::isNotBlank),
                        mark?.status?.label,
                        mark?.reason?.takeIf(String::isNotBlank),
                    ).joinToString(" · ")
                    if (detail.isNotEmpty()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TextButton(onClick = onEdit) { Text("状态") }
            }
        }
    }
}

@Composable
private fun StudentListItem(student: Student, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.titleMedium)
                if (student.studentNumber.isNotBlank()) {
                    Text(
                        student.studentNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            TextButton(onClick = onDelete) { Text("移除") }
        }
    }
}

@Composable
private fun HistoryItem(session: AttendanceSession, onClick: () -> Unit) {
    val absent = session.entries.count { it.status == AttendanceStatus.ABSENT }
    val leave = session.entries.count { it.status == AttendanceStatus.LEAVE }
    val late = session.entries.count { it.status == AttendanceStatus.LATE }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTime(session.createdAt), style = MaterialTheme.typography.titleSmall)
                Text(
                    "共 ${session.entries.size} 人 · 缺勤 $absent · 请假 $leave · 迟到 $late",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", fontSize = 28.sp)
        }
    }
}

@Composable
private fun GlobalHistoryItem(className: String, session: AttendanceSession, onClick: () -> Unit) {
    val present = session.entries.count { it.status == AttendanceStatus.PRESENT }
    val absent = session.entries.count { it.status == AttendanceStatus.ABSENT }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(className.ifBlank { "已删除的班级" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatTime(session.createdAt)} · 到 $present · 缺勤 $absent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", fontSize = 28.sp)
        }
    }
}

@Composable
private fun ResultEntryItem(entry: AttendanceEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.studentName, style = MaterialTheme.typography.titleMedium)
                if (entry.studentNumber.isNotBlank()) {
                    Text(
                        entry.studentNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.reason.isNotBlank()) {
                    Text("原因：${entry.reason}", modifier = Modifier.padding(top = 5.dp))
                }
            }
            StatusBadge(entry.status)
        }
    }
}

@Composable
private fun CompactResultItem(entry: AttendanceEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(entry.studentName, modifier = Modifier.weight(1f))
        if (entry.studentNumber.isNotBlank()) {
            Text(
                entry.studentNumber,
                modifier = Modifier.padding(end = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusBadge(entry.status)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusBadge(status: AttendanceStatus) {
    val color = when (status) {
        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
        AttendanceStatus.LATE -> Color(0xFFFFE0B2)
        AttendanceStatus.LEAVE -> MaterialTheme.colorScheme.secondaryContainer
        AttendanceStatus.ABSENT -> Color(0xFFFFDAD6)
        AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(status.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatusDialog(
    student: Student,
    initial: Mark,
    presetReasons: List<String>,
    defaultReason: String,
    onDismiss: () -> Unit,
    onConfirm: (Mark) -> Unit,
) {
    var status by remember(student.id) { mutableStateOf(initial.status) }
    var reason by remember(student.id) { mutableStateOf(initial.reason) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column {
                listOf(
                    AttendanceStatus.PRESENT,
                    AttendanceStatus.LATE,
                    AttendanceStatus.LEAVE,
                    AttendanceStatus.ABSENT,
                ).forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            status = option
                            if (option == AttendanceStatus.PRESENT) reason = ""
                            else if (reason.isBlank()) reason = defaultReason
                        }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = status == option,
                            onClick = {
                                status = option
                                if (option == AttendanceStatus.PRESENT) reason = ""
                                else if (reason.isBlank()) reason = defaultReason
                            },
                        )
                        Text(option.label)
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("原因或备注（可选）") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    maxLines = 3,
                )
                if (status != AttendanceStatus.PRESENT && presetReasons.isNotEmpty()) {
                    Text(
                        "常用原因",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetReasons) { preset ->
                            AssistChip(onClick = { reason = preset }, label = { Text(preset) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(Mark(status, reason.trim())) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加学生") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("学号（可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, number) }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TextImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("粘贴名单") },
        text = {
            Column {
                Text(
                    "每行一人，可使用“学号,姓名”或“姓名,学号”两列格式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("名单文本") },
                    placeholder = { Text("学号,姓名\n20260001,张三\n20260002,李四") },
                    minLines = 7,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleBackBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text("‹", fontSize = 30.sp)
            }
        },
    )
}

@Composable
private fun EmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun HintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
