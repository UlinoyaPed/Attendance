package com.ulinoyaped.attendance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ulinoyaped.attendance.data.AttendanceEntry
import com.ulinoyaped.attendance.data.AttendanceRepository
import com.ulinoyaped.attendance.data.AttendanceSession
import com.ulinoyaped.attendance.data.AttendanceStatus
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.ClassAttendanceSettings
import com.ulinoyaped.attendance.data.GestureAction
import com.ulinoyaped.attendance.data.HistoryTitleMode
import com.ulinoyaped.attendance.data.StatusColorOption
import com.ulinoyaped.attendance.data.StatusIconOption
import com.ulinoyaped.attendance.data.Student
import com.ulinoyaped.attendance.data.iconFor
import com.ulinoyaped.attendance.data.colorFor
import com.ulinoyaped.attendance.data.forClass
import com.ulinoyaped.attendance.data.toClassSettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs

private sealed interface Screen {
    data class Root(val tab: RootTab) : Screen
    data class ClassDetail(val classId: String) : Screen
    data class ClassSettings(val classId: String) : Screen
    data class RollCall(val classId: String, val backToClassDetail: Boolean = false) : Screen
    data class Result(
        val classId: String,
        val sessionId: String,
        val backTarget: ResultBackTarget = ResultBackTarget.CLASS_DETAIL,
    ) : Screen
}

private enum class ResultBackTarget {
    CLASSES,
    CLASS_DETAIL,
    HISTORY,
}

private enum class RootTab(val label: String) {
    CLASSES("班级"),
    HISTORY("历史"),
    SETTINGS("设置"),
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
    ICON_PRESENT,
    ICON_LATE,
    ICON_LEAVE,
    ICON_ABSENT,
    ICON_EXEMPT,
    COLOR_PRESENT,
    COLOR_LATE,
    COLOR_LEAVE,
    COLOR_ABSENT,
    COLOR_EXEMPT,
    HISTORY_TITLE,
}

private enum class SettingsTab(val label: String) {
    OPERATIONS("操作"),
    REASONS("原因"),
    ICONS("外观"),
    EXPORT("导出"),
    HISTORY("历史"),
}

private enum class ClassSettingSelector {
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
    val backTarget = screen
    BackHandler(enabled = backTarget !is Screen.Root || backTarget.tab != RootTab.CLASSES) {
        screen = when (val current = screen) {
            is Screen.Root -> Screen.Root(RootTab.CLASSES)
            is Screen.ClassDetail -> Screen.Root(RootTab.CLASSES)
            is Screen.ClassSettings -> Screen.ClassDetail(current.classId)
            is Screen.RollCall -> if (current.backToClassDetail) {
                Screen.ClassDetail(current.classId)
            } else {
                Screen.Root(RootTab.CLASSES)
            }
            is Screen.Result -> when (current.backTarget) {
                ResultBackTarget.CLASSES -> Screen.Root(RootTab.CLASSES)
                ResultBackTarget.CLASS_DETAIL -> Screen.ClassDetail(current.classId)
                ResultBackTarget.HISTORY -> Screen.Root(RootTab.HISTORY)
            }
        }
    }

    when (val current = screen) {
        is Screen.Root -> RootScreen(
            selectedTab = current.tab,
            classes = classes,
            sessions = sessions,
            settings = settings,
            onSelectTab = { screen = Screen.Root(it) },
            onAddClass = repository::addClass,
            onStartClass = { screen = Screen.RollCall(it) },
            onEditClass = { screen = Screen.ClassDetail(it) },
            onDeleteClass = repository::deleteClass,
            onOpenResult = { classId, sessionId ->
                screen = Screen.Result(classId, sessionId, ResultBackTarget.HISTORY)
            },
            onDeleteSession = repository::deleteSession,
            onAddReason = repository::addAbsenceReason,
            onRemoveReason = repository::removeAbsenceReason,
            onMoveReason = repository::moveAbsenceReason,
            onSetDefaultReason = repository::setDefaultReason,
            onSetDefaultStatus = repository::setDefaultStatus,
            onSetLongPressAction = repository::setLongPressAction,
            onSetSwipeLeftAction = repository::setSwipeLeftAction,
            onSetSwipeRightAction = repository::setSwipeRightAction,
            onSetStatusIcon = repository::setStatusIcon,
            onSetStatusColor = repository::setStatusColor,
            onSetGroupResultsByStatus = repository::setGroupResultsByStatus,
            onSetHistoryTitleMode = repository::setHistoryTitleMode,
            onSetExportHeader = repository::setExportHeader,
            onSetExportSummary = repository::setExportSummary,
            onSetExportPresentStudents = repository::setExportPresentStudents,
            onSetExportStudentNumber = repository::setExportStudentNumber,
            onSetExportReason = repository::setExportReason,
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
                    onStart = { screen = Screen.RollCall(group.id, backToClassDetail = true) },
                    onOpenResult = { screen = Screen.Result(group.id, it) },
                    onDeleteSession = repository::deleteSession,
                    onOpenSettings = { screen = Screen.ClassSettings(group.id) },
                )
            }
        }

        is Screen.ClassSettings -> {
            val group = classes.firstOrNull { it.id == current.classId }
            if (group == null) {
                screen = Screen.Root(RootTab.CLASSES)
            } else {
                ClassSettingsScreen(
                    group = group,
                    globalSettings = settings,
                    onBack = { screen = Screen.ClassDetail(group.id) },
                    onSave = {
                        repository.setClassAttendanceSettings(group.id, it)
                        screen = Screen.ClassDetail(group.id)
                    },
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
                    onBack = {
                        screen = if (current.backToClassDetail) {
                            Screen.ClassDetail(group.id)
                        } else {
                            Screen.Root(RootTab.CLASSES)
                        }
                    },
                    onFinish = { entries ->
                        val sessionId = repository.saveSession(group.id, entries)
                        val backTarget = if (current.backToClassDetail) {
                            ResultBackTarget.CLASS_DETAIL
                        } else {
                            ResultBackTarget.CLASSES
                        }
                        screen = Screen.Result(group.id, sessionId, backTarget)
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
                    settings = settings,
                    onBack = {
                        screen = when (current.backTarget) {
                            ResultBackTarget.CLASSES -> Screen.Root(RootTab.CLASSES)
                            ResultBackTarget.CLASS_DETAIL -> Screen.ClassDetail(group.id)
                            ResultBackTarget.HISTORY -> Screen.Root(RootTab.HISTORY)
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
    onStartClass: (String) -> Unit,
    onEditClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit,
    onOpenResult: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onAddReason: (String) -> Unit,
    onRemoveReason: (String) -> Unit,
    onMoveReason: (Int, Int) -> Unit,
    onSetDefaultReason: (String) -> Unit,
    onSetDefaultStatus: (AttendanceStatus) -> Unit,
    onSetLongPressAction: (GestureAction) -> Unit,
    onSetSwipeLeftAction: (GestureAction) -> Unit,
    onSetSwipeRightAction: (GestureAction) -> Unit,
    onSetStatusIcon: (AttendanceStatus, StatusIconOption) -> Unit,
    onSetStatusColor: (AttendanceStatus, StatusColorOption) -> Unit,
    onSetGroupResultsByStatus: (Boolean) -> Unit,
    onSetHistoryTitleMode: (HistoryTitleMode) -> Unit,
    onSetExportHeader: (Boolean) -> Unit,
    onSetExportSummary: (Boolean) -> Unit,
    onSetExportPresentStudents: (Boolean) -> Unit,
    onSetExportStudentNumber: (Boolean) -> Unit,
    onSetExportReason: (Boolean) -> Unit,
) {
    val bottomBar: @Composable () -> Unit = {
        RootNavigationBar(selectedTab = selectedTab, onSelectTab = onSelectTab)
    }
    when (selectedTab) {
        RootTab.CLASSES -> ClassesScreen(
            classes = classes,
            onAddClass = onAddClass,
            onStartClass = onStartClass,
            onEditClass = onEditClass,
            onDeleteClass = onDeleteClass,
            bottomBar = bottomBar,
        )
        RootTab.HISTORY -> HistoryScreen(
            classes = classes,
            sessions = sessions,
            onOpenResult = onOpenResult,
            onDeleteSession = onDeleteSession,
            titleMode = settings.historyTitleMode,
            bottomBar = bottomBar,
        )
        RootTab.SETTINGS -> SettingsScreen(
            settings = settings,
            onAddReason = onAddReason,
            onRemoveReason = onRemoveReason,
            onMoveReason = onMoveReason,
            onSetDefaultReason = onSetDefaultReason,
            onSetDefaultStatus = onSetDefaultStatus,
            onSetLongPressAction = onSetLongPressAction,
            onSetSwipeLeftAction = onSetSwipeLeftAction,
            onSetSwipeRightAction = onSetSwipeRightAction,
            onSetStatusIcon = onSetStatusIcon,
            onSetStatusColor = onSetStatusColor,
            onSetGroupResultsByStatus = onSetGroupResultsByStatus,
            onSetHistoryTitleMode = onSetHistoryTitleMode,
            onSetExportHeader = onSetExportHeader,
            onSetExportSummary = onSetExportSummary,
            onSetExportPresentStudents = onSetExportPresentStudents,
            onSetExportStudentNumber = onSetExportStudentNumber,
            onSetExportReason = onSetExportReason,
            bottomBar = bottomBar,
        )
    }
}

@Composable
private fun RootNavigationBar(selectedTab: RootTab, onSelectTab: (RootTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        RootTab.entries.forEach { tab ->
            val icon = when (tab) {
                RootTab.CLASSES -> Icons.Default.Groups
                RootTab.HISTORY -> Icons.Default.History
                RootTab.SETTINGS -> Icons.Default.Settings
            }
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootLargeTopBar(title: String) {
    LargeTopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ClassesScreen(
    classes: List<ClassGroup>,
    onAddClass: (String) -> Unit,
    onStartClass: (String) -> Unit,
    onEditClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var classToManage by remember { mutableStateOf<ClassGroup?>(null) }
    var classToDelete by remember { mutableStateOf<ClassGroup?>(null) }

    Scaffold(
        topBar = { RootLargeTopBar("我的班级") },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "创建班级")
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
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "轻点班级立即开始点名，长按可编辑班级",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                items(classes, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onStartClass(group.id) },
                                onLongClick = { classToManage = group },
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${group.students.size} 名学生 · 点击开始",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "开始点名",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    classToManage?.let { group ->
        AlertDialog(
            onDismissRequest = { classToManage = null },
            title = { Text(group.name) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("编辑班级") },
                        supportingContent = { Text("管理名单、导入学生并查看班级历史") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            classToManage = null
                            onEditClass(group.id)
                        },
                    )
                    ListItem(
                        headlineContent = { Text("删除班级") },
                        supportingContent = { Text("同时删除名单与全部点名历史") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                        modifier = Modifier.clickable {
                            classToManage = null
                            classToDelete = group
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { classToManage = null }) { Text("取消") }
            },
        )
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
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddStudent by remember { mutableStateOf(false) }
    var showImportOptions by remember { mutableStateOf(false) }
    var showTextImport by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var sessionToDelete by remember { mutableStateOf<AttendanceSession?>(null) }
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
        topBar = {
            SimpleBackBar(group.name, onBack) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "班级设置")
                }
            }
        },
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
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
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
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("手动添加", modifier = Modifier.padding(start = 5.dp))
                    }
                    OutlinedButton(
                        onClick = { showImportOptions = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("导入名单", modifier = Modifier.padding(start = 5.dp))
                    }
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
                    HistoryItem(
                        session = session,
                        onClick = { onOpenResult(session.id) },
                        onDelete = { sessionToDelete = session },
                    )
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
    sessionToDelete?.let { session ->
        DeleteHistoryDialog(
            session = session,
            onDismiss = { sessionToDelete = null },
            onConfirm = {
                onDeleteSession(session.id)
                sessionToDelete = null
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
    val effectiveSettings = settings.forClass(group)
    val marks = remember(group.id) { mutableStateMapOf<String, Mark>() }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val checked = group.students.count { marks[it.id]?.status != null }

    fun applyAction(student: Student, action: GestureAction) {
        when (action) {
            GestureAction.EDIT -> editingStudent = student
            GestureAction.PRESENT -> marks[student.id] = Mark(AttendanceStatus.PRESENT)
            GestureAction.LATE -> marks[student.id] = Mark(AttendanceStatus.LATE, effectiveSettings.defaultReason)
            GestureAction.LEAVE -> marks[student.id] = Mark(AttendanceStatus.LEAVE, effectiveSettings.defaultReason)
            GestureAction.ABSENT -> marks[student.id] = Mark(AttendanceStatus.ABSENT, effectiveSettings.defaultReason)
            GestureAction.EXEMPT -> marks[student.id] = Mark(AttendanceStatus.EXEMPT)
            GestureAction.CLEAR -> marks.remove(student.id)
        }
    }

    fun finish() {
        val entries = group.students.map { student ->
            val mark = marks[student.id] ?: Mark(AttendanceStatus.ABSENT, effectiveSettings.defaultReason)
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
                        "点按：${effectiveSettings.defaultStatus.label} · 长按：${effectiveSettings.longPressAction.label} · 左滑：${effectiveSettings.swipeLeftAction.label} · 右滑：${effectiveSettings.swipeRightAction.label}",
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
                    iconOption = mark?.status?.let { effectiveSettings.iconFor(it) },
                    colorOption = mark?.status?.let { effectiveSettings.colorFor(it) },
                    onTogglePresent = {
                        if (mark?.status == effectiveSettings.defaultStatus) {
                            marks.remove(student.id)
                        } else {
                            marks[student.id] = Mark(
                                effectiveSettings.defaultStatus,
                                effectiveSettings.defaultReason.takeIf { statusUsesReason(effectiveSettings.defaultStatus) }.orEmpty(),
                            )
                        }
                    },
                    onLongPress = { applyAction(student, effectiveSettings.longPressAction) },
                    onSwipeLeft = { applyAction(student, effectiveSettings.swipeLeftAction) },
                    onSwipeRight = { applyAction(student, effectiveSettings.swipeRightAction) },
                    swipeLeftLabel = effectiveSettings.swipeLeftAction.label,
                    swipeRightLabel = effectiveSettings.swipeRightAction.label,
                    onEdit = { editingStudent = student },
                )
            }
        }
    }

    editingStudent?.let { student ->
        StatusDialog(
            student = student,
            initial = marks[student.id] ?: Mark(
                effectiveSettings.defaultStatus,
                effectiveSettings.defaultReason.takeIf { statusUsesReason(effectiveSettings.defaultStatus) }.orEmpty(),
            ),
            presetReasons = effectiveSettings.absenceReasons,
            defaultReason = effectiveSettings.defaultReason,
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
    settings: AppSettings,
    onBack: () -> Unit,
) {
    val effectiveSettings = settings.forClass(group)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    val exportText = remember(group, session, effectiveSettings) {
        buildAttendanceExport(group, session, effectiveSettings)
    }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportText) }
                    ?: error("无法写入文件")
            }.isSuccess
            scope.launch { snackbarHostState.showSnackbar(if (saved) "文本已保存" else "保存失败") }
        }
    }
    val counts = AttendanceStatus.entries.associateWith { status ->
        session.entries.count { it.status == status }
    }
    val resultStatuses = listOf(
        AttendanceStatus.PRESENT,
        AttendanceStatus.LATE,
        AttendanceStatus.LEAVE,
        AttendanceStatus.ABSENT,
        AttendanceStatus.EXEMPT,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("点名结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("导出", modifier = Modifier.padding(start = 4.dp))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(resultStatuses) { status ->
                        SummaryCard(
                            label = status.label,
                            count = counts.getValue(status),
                            iconOption = effectiveSettings.iconFor(status),
                            colorOption = effectiveSettings.colorFor(status),
                            modifier = Modifier.width(84.dp),
                        )
                    }
                }
            }
            if (effectiveSettings.groupResultsByStatus) {
                resultStatuses.forEach { status ->
                    val entries = session.entries.filter { it.status == status }
                    if (entries.isNotEmpty()) {
                        item(key = "section-${status.name}") {
                            SectionTitle("${status.label} · ${entries.size}")
                        }
                        items(entries, key = { "${status.name}-${it.studentId}" }) { entry ->
                            ResultEntryItem(entry, effectiveSettings)
                        }
                    }
                }
            } else {
                item { SectionTitle("人员明细 · ${session.entries.size}") }
                items(session.entries, key = { it.studentId }) { entry ->
                    CompactResultItem(entry, effectiveSettings)
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出本次点名") },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        exportText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 12,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("点名结果", exportText))
                        showExportDialog = false
                        scope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                    },
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("复制文本", modifier = Modifier.padding(start = 4.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        saveLauncher.launch("attendance-${formatFileTime(session.createdAt)}.txt")
                    },
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("保存文件", modifier = Modifier.padding(start = 4.dp))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    classes: List<ClassGroup>,
    sessions: List<AttendanceSession>,
    onOpenResult: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    titleMode: HistoryTitleMode,
    bottomBar: @Composable () -> Unit,
) {
    val classNames = classes.associate { it.id to it.name }
    var sessionToDelete by remember { mutableStateOf<AttendanceSession?>(null) }
    Scaffold(
        topBar = { RootLargeTopBar("点名历史") },
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
                        titleMode = titleMode,
                        onClick = { onOpenResult(session.classId, session.id) },
                        onDelete = { sessionToDelete = session },
                    )
                }
            }
        }
    }
    sessionToDelete?.let { session ->
        DeleteHistoryDialog(
            session = session,
            onDismiss = { sessionToDelete = null },
            onConfirm = {
                onDeleteSession(session.id)
                sessionToDelete = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onAddReason: (String) -> Unit,
    onRemoveReason: (String) -> Unit,
    onMoveReason: (Int, Int) -> Unit,
    onSetDefaultReason: (String) -> Unit,
    onSetDefaultStatus: (AttendanceStatus) -> Unit,
    onSetLongPressAction: (GestureAction) -> Unit,
    onSetSwipeLeftAction: (GestureAction) -> Unit,
    onSetSwipeRightAction: (GestureAction) -> Unit,
    onSetStatusIcon: (AttendanceStatus, StatusIconOption) -> Unit,
    onSetStatusColor: (AttendanceStatus, StatusColorOption) -> Unit,
    onSetGroupResultsByStatus: (Boolean) -> Unit,
    onSetHistoryTitleMode: (HistoryTitleMode) -> Unit,
    onSetExportHeader: (Boolean) -> Unit,
    onSetExportSummary: (Boolean) -> Unit,
    onSetExportPresentStudents: (Boolean) -> Unit,
    onSetExportStudentNumber: (Boolean) -> Unit,
    onSetExportReason: (Boolean) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showAddReason by remember { mutableStateOf(false) }
    var selector by remember { mutableStateOf<SettingSelector?>(null) }
    var selectedTab by remember { mutableStateOf(SettingsTab.OPERATIONS) }
    Scaffold(
        topBar = {
            Column {
                RootLargeTopBar("设置")
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 12.dp,
                    divider = {},
                ) {
                    SettingsTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (selectedTab) {
                SettingsTab.OPERATIONS -> {
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
                            "再次点按相同状态会清除标记。左右滑动需要明确的横向手势。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }

                SettingsTab.REASONS -> {
                    item { SectionTitle("未到原因") }
                    item {
                        SettingsGroup {
                            SettingRow(
                                title = "默认未到原因",
                                value = settings.defaultReason.ifBlank { "不预填" },
                                onClick = { selector = SettingSelector.DEFAULT_REASON },
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle("常用原因顺序")
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { showAddReason = true }) { Text("添加") }
                        }
                    }
                    if (settings.absenceReasons.isEmpty()) {
                        item { HintCard("暂未设置常用原因。点名时仍可手动输入原因。") }
                    } else {
                        itemsIndexed(settings.absenceReasons, key = { _, reason -> reason }) { index, reason ->
                            val density = LocalDensity.current
                            var accumulatedDrag by remember(reason) { mutableFloatStateOf(0f) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "拖动排序",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .padding(9.dp)
                                            .pointerInput(reason, index) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { accumulatedDrag = 0f },
                                                    onDragEnd = { accumulatedDrag = 0f },
                                                    onDragCancel = { accumulatedDrag = 0f },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount.y
                                                        val threshold = with(density) { 42.dp.toPx() }
                                                        if (abs(accumulatedDrag) >= threshold) {
                                                            val target = if (accumulatedDrag > 0) index + 1 else index - 1
                                                            if (target in settings.absenceReasons.indices) {
                                                                onMoveReason(index, target)
                                                            }
                                                            accumulatedDrag = 0f
                                                        }
                                                    },
                                                )
                                            },
                                    )
                                    Text(reason, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onRemoveReason(reason) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsTab.ICONS -> {
                    item { SectionTitle("状态外观") }
                    item {
                        SettingsGroup {
                            StatusAppearanceRow(
                                "到场", settings.presentIcon, settings.presentColor,
                                { selector = SettingSelector.ICON_PRESENT },
                                { selector = SettingSelector.COLOR_PRESENT },
                            )
                            HorizontalDivider()
                            StatusAppearanceRow(
                                "迟到", settings.lateIcon, settings.lateColor,
                                { selector = SettingSelector.ICON_LATE },
                                { selector = SettingSelector.COLOR_LATE },
                            )
                            HorizontalDivider()
                            StatusAppearanceRow(
                                "请假", settings.leaveIcon, settings.leaveColor,
                                { selector = SettingSelector.ICON_LEAVE },
                                { selector = SettingSelector.COLOR_LEAVE },
                            )
                            HorizontalDivider()
                            StatusAppearanceRow(
                                "缺勤", settings.absentIcon, settings.absentColor,
                                { selector = SettingSelector.ICON_ABSENT },
                                { selector = SettingSelector.COLOR_ABSENT },
                            )
                            HorizontalDivider()
                            StatusAppearanceRow(
                                "不参与", settings.exemptIcon, settings.exemptColor,
                                { selector = SettingSelector.ICON_EXEMPT },
                                { selector = SettingSelector.COLOR_EXEMPT },
                            )
                        }
                    }
                }

                SettingsTab.EXPORT -> {
                    item { SectionTitle("结果排列") }
                    item {
                        SettingsGroup {
                            SwitchSettingRow(
                                "按状态分类排列",
                                settings.groupResultsByStatus,
                                onSetGroupResultsByStatus,
                            )
                        }
                    }
                    item { SectionTitle("文本导出内容") }
                    item {
                        SettingsGroup {
                            SwitchSettingRow("班级与点名时间", settings.exportHeader, onSetExportHeader)
                            HorizontalDivider()
                            SwitchSettingRow("到勤统计", settings.exportSummary, onSetExportSummary)
                            HorizontalDivider()
                            SwitchSettingRow("到场学生明细", settings.exportPresentStudents, onSetExportPresentStudents)
                            HorizontalDivider()
                            SwitchSettingRow("学生学号", settings.exportStudentNumber, onSetExportStudentNumber)
                            HorizontalDivider()
                            SwitchSettingRow("原因或备注", settings.exportReason, onSetExportReason)
                        }
                    }
                }

                SettingsTab.HISTORY -> {
                    item { SectionTitle("历史记录") }
                    item {
                        SettingsGroup {
                            SettingRow(
                                title = "记录标题",
                                value = settings.historyTitleMode.label,
                                onClick = { selector = SettingSelector.HISTORY_TITLE },
                            )
                        }
                    }
                    item {
                        Text(
                            "选择历史列表优先显示班级名称或点名时间。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
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
            SettingSelector.ICON_PRESENT -> IconChoiceDialog(
                title = "到场图标",
                selected = settings.presentIcon,
                onDismiss = { selector = null },
                onSelect = { onSetStatusIcon(AttendanceStatus.PRESENT, it); selector = null },
            )
            SettingSelector.ICON_LATE -> IconChoiceDialog(
                title = "迟到图标",
                selected = settings.lateIcon,
                onDismiss = { selector = null },
                onSelect = { onSetStatusIcon(AttendanceStatus.LATE, it); selector = null },
            )
            SettingSelector.ICON_LEAVE -> IconChoiceDialog(
                title = "请假图标",
                selected = settings.leaveIcon,
                onDismiss = { selector = null },
                onSelect = { onSetStatusIcon(AttendanceStatus.LEAVE, it); selector = null },
            )
            SettingSelector.ICON_ABSENT -> IconChoiceDialog(
                title = "缺勤图标",
                selected = settings.absentIcon,
                onDismiss = { selector = null },
                onSelect = { onSetStatusIcon(AttendanceStatus.ABSENT, it); selector = null },
            )
            SettingSelector.ICON_EXEMPT -> IconChoiceDialog(
                title = "不参与图标",
                selected = settings.exemptIcon,
                onDismiss = { selector = null },
                onSelect = { onSetStatusIcon(AttendanceStatus.EXEMPT, it); selector = null },
            )
            SettingSelector.COLOR_PRESENT -> ColorChoiceDialog(
                "到场颜色", settings.presentColor, { selector = null },
            ) { onSetStatusColor(AttendanceStatus.PRESENT, it); selector = null }
            SettingSelector.COLOR_LATE -> ColorChoiceDialog(
                "迟到颜色", settings.lateColor, { selector = null },
            ) { onSetStatusColor(AttendanceStatus.LATE, it); selector = null }
            SettingSelector.COLOR_LEAVE -> ColorChoiceDialog(
                "请假颜色", settings.leaveColor, { selector = null },
            ) { onSetStatusColor(AttendanceStatus.LEAVE, it); selector = null }
            SettingSelector.COLOR_ABSENT -> ColorChoiceDialog(
                "缺勤颜色", settings.absentColor, { selector = null },
            ) { onSetStatusColor(AttendanceStatus.ABSENT, it); selector = null }
            SettingSelector.COLOR_EXEMPT -> ColorChoiceDialog(
                "不参与颜色", settings.exemptColor, { selector = null },
            ) { onSetStatusColor(AttendanceStatus.EXEMPT, it); selector = null }
            SettingSelector.HISTORY_TITLE -> HistoryTitleChoiceDialog(
                selected = settings.historyTitleMode,
                onDismiss = { selector = null },
                onSelect = { onSetHistoryTitleMode(it); selector = null },
            )
        }
    }
}

@Composable
private fun ClassSettingsScreen(
    group: ClassGroup,
    globalSettings: AppSettings,
    onBack: () -> Unit,
    onSave: (ClassAttendanceSettings?) -> Unit,
) {
    var customEnabled by remember(group.id, group.attendanceSettings) {
        mutableStateOf(group.attendanceSettings != null)
    }
    var draft by remember(group.id, group.attendanceSettings, globalSettings) {
        mutableStateOf(group.attendanceSettings ?: globalSettings.toClassSettings())
    }
    var selector by remember { mutableStateOf<ClassSettingSelector?>(null) }

    Scaffold(
        topBar = {
            SimpleBackBar("${group.name} · 设置", onBack) {
                TextButton(onClick = { onSave(draft.takeIf { customEnabled }) }) {
                    Text("保存")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle("班级专属设置") }
            item {
                SettingsGroup {
                    SwitchSettingRow("覆盖全局点名设置", customEnabled) { enabled ->
                        customEnabled = enabled
                        if (enabled && group.attendanceSettings == null) {
                            draft = globalSettings.toClassSettings()
                        }
                    }
                }
            }
            if (customEnabled) {
                item { SectionTitle("点名操作") }
                item {
                    SettingsGroup {
                        SettingRow("点按默认选择", draft.defaultStatus.label) {
                            selector = ClassSettingSelector.DEFAULT_STATUS
                        }
                        HorizontalDivider()
                        SettingRow("默认未到原因", draft.defaultReason.ifBlank { "不预填" }) {
                            selector = ClassSettingSelector.DEFAULT_REASON
                        }
                        HorizontalDivider()
                        SettingRow("长按姓名", draft.longPressAction.label) {
                            selector = ClassSettingSelector.LONG_PRESS
                        }
                        HorizontalDivider()
                        SettingRow("向左滑动", draft.swipeLeftAction.label) {
                            selector = ClassSettingSelector.SWIPE_LEFT
                        }
                        HorizontalDivider()
                        SettingRow("向右滑动", draft.swipeRightAction.label) {
                            selector = ClassSettingSelector.SWIPE_RIGHT
                        }
                    }
                }
                item { SectionTitle("结果排列") }
                item {
                    SettingsGroup {
                        SwitchSettingRow(
                            "按状态分类排列",
                            draft.groupResultsByStatus,
                        ) { draft = draft.copy(groupResultsByStatus = it) }
                    }
                }
            } else {
                item { HintCard("当前跟随全局设置。启用后，这个班级可以单独指定点按、手势、默认原因和结果排列。") }
            }
            item {
                Text(
                    "未到原因列表、状态图标、颜色、导出内容和历史标题继续使用全局设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }

    selector?.let { selected ->
        when (selected) {
            ClassSettingSelector.DEFAULT_STATUS -> StatusChoiceDialog(
                "点按默认选择", draft.defaultStatus, { selector = null },
            ) { draft = draft.copy(defaultStatus = it); selector = null }
            ClassSettingSelector.DEFAULT_REASON -> ReasonChoiceDialog(
                globalSettings.absenceReasons, draft.defaultReason, { selector = null },
            ) { draft = draft.copy(defaultReason = it); selector = null }
            ClassSettingSelector.LONG_PRESS -> GestureChoiceDialog(
                "长按姓名", draft.longPressAction, { selector = null },
            ) { draft = draft.copy(longPressAction = it); selector = null }
            ClassSettingSelector.SWIPE_LEFT -> GestureChoiceDialog(
                "向左滑动", draft.swipeLeftAction, { selector = null },
            ) { draft = draft.copy(swipeLeftAction = it); selector = null }
            ClassSettingSelector.SWIPE_RIGHT -> GestureChoiceDialog(
                "向右滑动", draft.swipeRightAction, { selector = null },
            ) { draft = draft.copy(swipeRightAction = it); selector = null }
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
private fun StatusAppearanceRow(
    title: String,
    iconOption: StatusIconOption,
    colorOption: StatusColorOption,
    onIconClick: () -> Unit,
    onColorClick: () -> Unit,
) {
    val color = statusColor(colorOption)
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = color.copy(alpha = 0.16f), shape = CircleShape) {
            Icon(
                statusImageVector(iconOption),
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(9.dp).size(18.dp),
            )
        }
        Text(title, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onIconClick) { Text(iconOption.label) }
        IconButton(onClick = onColorClick) {
            Icon(Icons.Default.Palette, contentDescription = "${title}颜色", tint = color)
        }
    }
}

@Composable
private fun SwitchSettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
private fun IconChoiceDialog(
    title: String,
    selected: StatusIconOption,
    onDismiss: () -> Unit,
    onSelect: (StatusIconOption) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                StatusIconOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Icon(statusImageVector(option), contentDescription = null, modifier = Modifier.size(21.dp))
                        Text(option.label, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ColorChoiceDialog(
    title: String,
    selected: StatusColorOption,
    onDismiss: () -> Unit,
    onSelect: (StatusColorOption) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                StatusColorOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(statusColor(option)),
                        )
                        Text(option.label, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun HistoryTitleChoiceDialog(
    selected: HistoryTitleMode,
    onDismiss: () -> Unit,
    onSelect: (HistoryTitleMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史记录标题") },
        text = {
            Column {
                HistoryTitleMode.entries.forEach { option ->
                    ChoiceRow(option.label, option == selected) { onSelect(option) }
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
    iconOption: StatusIconOption?,
    colorOption: StatusColorOption?,
    onTogglePresent: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeLeftLabel: String,
    swipeRightLabel: String,
    onEdit: () -> Unit,
) {
    val marked = mark != null
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val triggerDistance = remember(density) { with(density) { 72.dp.toPx() } }
    val maximumDrag = remember(density) { with(density) { 112.dp.toPx() } }
    var horizontalOffset by remember(student.id) { mutableFloatStateOf(0f) }
    val markColor = colorOption?.let { statusColor(it) }
    val container = markColor?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainerLow

    fun resetHorizontalOffset() {
        val start = horizontalOffset
        scope.launch {
            animate(initialValue = start, targetValue = 0f) { value, _ ->
                horizontalOffset = value
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 20.dp),
            contentAlignment = if (horizontalOffset >= 0f) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            Text(if (horizontalOffset >= 0f) swipeRightLabel else swipeLeftLabel)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(horizontalOffset.roundToInt(), 0) }
                .animateContentSize()
                .pointerInput(student.id, swipeLeftLabel, swipeRightLabel) {
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalOffset = 0f },
                        onDragCancel = { resetHorizontalOffset() },
                        onDragEnd = {
                            when {
                                horizontalOffset >= triggerDistance -> onSwipeRight()
                                horizontalOffset <= -triggerDistance -> onSwipeLeft()
                            }
                            resetHorizontalOffset()
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalOffset = (horizontalOffset + dragAmount)
                                .coerceIn(-maximumDrag, maximumDrag)
                        },
                    )
                }
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
                            if (marked) markColor ?: MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconOption?.let(::statusImageVector) ?: Icons.Default.Person,
                        contentDescription = mark?.status?.label ?: "未点",
                        tint = if (marked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
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
private fun HistoryItem(session: AttendanceSession, onClick: () -> Unit, onDelete: () -> Unit) {
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除记录")
            }
        }
    }
}

@Composable
private fun GlobalHistoryItem(
    className: String,
    session: AttendanceSession,
    titleMode: HistoryTitleMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val present = session.entries.count { it.status == AttendanceStatus.PRESENT }
    val absent = session.entries.count { it.status == AttendanceStatus.ABSENT }
    val exempt = session.entries.count { it.status == AttendanceStatus.EXEMPT }
    val resolvedClassName = className.ifBlank { "已删除的班级" }
    val headline = if (titleMode == HistoryTitleMode.CLASS_NAME) resolvedClassName else formatTime(session.createdAt)
    val detailPrefix = if (titleMode == HistoryTitleMode.CLASS_NAME) formatTime(session.createdAt) else resolvedClassName
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
                Text(headline, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$detailPrefix · 到 $present · 缺勤 $absent · 不参与 $exempt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除记录")
            }
        }
    }
}

@Composable
private fun DeleteHistoryDialog(
    session: AttendanceSession,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除点名记录？") },
        text = { Text("将删除 ${formatTime(session.createdAt)} 的点名结果，此操作无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("删除", modifier = Modifier.padding(start = 4.dp))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ResultEntryItem(entry: AttendanceEntry, settings: AppSettings) {
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
            StatusBadge(entry.status, settings.iconFor(entry.status), settings.colorFor(entry.status))
        }
    }
}

@Composable
private fun CompactResultItem(entry: AttendanceEntry, settings: AppSettings) {
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
        StatusBadge(entry.status, settings.iconFor(entry.status), settings.colorFor(entry.status))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusBadge(
    status: AttendanceStatus,
    iconOption: StatusIconOption,
    colorOption: StatusColorOption,
) {
    val color = statusColor(colorOption)
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                statusImageVector(iconOption),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp),
            )
            Text(status.label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    count: Int,
    iconOption: StatusIconOption,
    colorOption: StatusColorOption,
    modifier: Modifier = Modifier,
) {
    val color = statusColor(colorOption)
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                statusImageVector(iconOption),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
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
                    AttendanceStatus.EXEMPT,
                ).forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            status = option
                            if (!statusUsesReason(option)) reason = ""
                            else if (reason.isBlank()) reason = defaultReason
                        }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = status == option,
                            onClick = {
                                status = option
                                if (!statusUsesReason(option)) reason = ""
                                else if (reason.isBlank()) reason = defaultReason
                            },
                        )
                        Text(option.label)
                    }
                }
                if (statusUsesReason(status)) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("原因或备注（可选）") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        maxLines = 3,
                    )
                }
                if (statusUsesReason(status) && presetReasons.isNotEmpty()) {
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
private fun SimpleBackBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = actions,
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

private fun formatFileTime(timestamp: Long): String =
    SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(timestamp))

private fun statusImageVector(option: StatusIconOption): ImageVector = when (option) {
    StatusIconOption.CHECK -> Icons.Default.CheckCircle
    StatusIconOption.PERSON -> Icons.Default.Person
    StatusIconOption.SCHEDULE -> Icons.Default.Schedule
    StatusIconOption.EVENT_BUSY -> Icons.Default.EventBusy
    StatusIconOption.CLOSE -> Icons.Default.Close
    StatusIconOption.WARNING -> Icons.Default.Warning
    StatusIconOption.STAR -> Icons.Default.Star
    StatusIconOption.HELP -> Icons.Default.Help
    StatusIconOption.REMOVE -> Icons.Default.RemoveCircle
}

@Composable
private fun statusColor(option: StatusColorOption): Color = when (option) {
    StatusColorOption.PRIMARY -> MaterialTheme.colorScheme.primary
    StatusColorOption.GREEN -> Color(0xFF2E7D32)
    StatusColorOption.AMBER -> Color(0xFFF57C00)
    StatusColorOption.BLUE -> Color(0xFF1565C0)
    StatusColorOption.RED -> Color(0xFFC62828)
    StatusColorOption.PURPLE -> Color(0xFF6A1B9A)
    StatusColorOption.TEAL -> Color(0xFF00796B)
    StatusColorOption.GRAY -> Color(0xFF616161)
}

private fun statusUsesReason(status: AttendanceStatus): Boolean = status in setOf(
    AttendanceStatus.LATE,
    AttendanceStatus.LEAVE,
    AttendanceStatus.ABSENT,
)

private fun buildAttendanceExport(
    group: ClassGroup,
    session: AttendanceSession,
    settings: AppSettings,
): String = buildString {
    if (settings.exportHeader) {
        appendLine("${group.name} 点名结果")
        appendLine(formatTime(session.createdAt))
    }
    if (settings.exportSummary) {
        val counts = AttendanceStatus.entries.associateWith { status ->
            session.entries.count { it.status == status }
        }
        if (isNotEmpty()) appendLine()
        appendLine("共 ${session.entries.size} 人")
        appendLine(
            "到场 ${counts.getValue(AttendanceStatus.PRESENT)}，" +
                "迟到 ${counts.getValue(AttendanceStatus.LATE)}，" +
                "请假 ${counts.getValue(AttendanceStatus.LEAVE)}，" +
                "缺勤 ${counts.getValue(AttendanceStatus.ABSENT)}，" +
                "不参与 ${counts.getValue(AttendanceStatus.EXEMPT)}",
        )
    }
    val includedEntries = session.entries.filter {
        settings.exportPresentStudents || it.status != AttendanceStatus.PRESENT
    }
    if (includedEntries.isNotEmpty()) {
        if (isNotEmpty()) appendLine()
        if (settings.groupResultsByStatus) {
            listOf(
                AttendanceStatus.PRESENT,
                AttendanceStatus.LATE,
                AttendanceStatus.LEAVE,
                AttendanceStatus.ABSENT,
                AttendanceStatus.EXEMPT,
            ).forEach { status ->
                val entries = includedEntries.filter { it.status == status }
                if (entries.isNotEmpty()) {
                    appendLine("${status.label}（${entries.size}）")
                    entries.forEach { entry -> appendExportEntry(entry, settings) }
                    appendLine()
                }
            }
        } else {
            appendLine("人员明细")
            includedEntries.forEach { entry ->
                append("[${entry.status.label}] ")
                appendExportEntry(entry, settings)
            }
        }
    } else if (isEmpty()) {
        append("无可导出的点名内容")
    }
}.trimEnd()

private fun StringBuilder.appendExportEntry(entry: AttendanceEntry, settings: AppSettings) {
    if (settings.exportStudentNumber && entry.studentNumber.isNotBlank()) {
        append("${entry.studentNumber} ")
    }
    append(entry.studentName)
    if (settings.exportReason && entry.reason.isNotBlank()) {
        append("（${entry.reason}）")
    }
    appendLine()
}
