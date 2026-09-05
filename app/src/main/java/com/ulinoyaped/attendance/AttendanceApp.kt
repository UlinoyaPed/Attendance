package com.ulinoyaped.attendance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import com.ulinoyaped.attendance.data.resultCollapseOptions
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.ulinoyaped.attendance.data.DisplayOption
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
            onSetDisplayOption = repository::setDisplayOption,
            onExportBackup = repository::exportBackup,
            onImportBackup = repository::importBackup,
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
                    settings = settings,
                    sessions = sessions.filter { it.classId == group.id },
                    onBack = { screen = Screen.Root(RootTab.CLASSES) },
                    onAddStudent = { name, number -> repository.addStudent(group.id, name, number) },
                    onRemoveStudent = { repository.removeStudent(group.id, it) },
                    onImport = { repository.importStudents(group.id, it) },
                    onStart = { screen = Screen.RollCall(group.id, backToClassDetail = true) },
                    onOpenResult = { screen = Screen.Result(group.id, it) },
                    onDeleteSession = repository::deleteSession,
                    onOpenSettings = { screen = Screen.ClassSettings(group.id) },
                    onRenameClass = { repository.renameClass(group.id, it) },
                    onUpdateStudent = { studentId, name, number ->
                        repository.updateStudent(group.id, studentId, name, number)
                    },
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
                    initialEntries = repository.getRollCallDraft(group.id),
                    onDraftChange = { repository.saveRollCallDraft(group.id, it) },
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
                    onUpdateEntry = { studentId, status, reason ->
                        repository.updateSessionEntry(session.id, studentId, status, reason)
                    },
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
    onSetDisplayOption: (DisplayOption, Boolean) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
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
            settings = settings,
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
            showStatistics = settings.showHistoryStatistics,
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
            onSetDisplayOption = onSetDisplayOption,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
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
    settings: AppSettings,
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
                if (settings.showClassOperationHint) {
                    item {
                        Text(
                            "轻点班级立即开始点名，长按可编辑班级",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
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
                                if (settings.showClassStudentCount) {
                                    Text(
                                        "${group.students.size} 名学生 · 点击开始",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
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
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text("管理名单、导入学生并查看班级历史") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            classToManage = null
                            onEditClass(group.id)
                        },
                    )
                    ListItem(
                        headlineContent = { Text("删除班级") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
    settings: AppSettings,
    sessions: List<AttendanceSession>,
    onBack: () -> Unit,
    onAddStudent: (String, String) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onImport: (String) -> Int,
    onStart: () -> Unit,
    onOpenResult: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRenameClass: (String) -> Unit,
    onUpdateStudent: (String, String, String) -> Unit,
) {
    val effectiveSettings = settings.forClass(group)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddStudent by remember { mutableStateOf(false) }
    var showImportOptions by remember { mutableStateOf(false) }
    var showTextImport by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var sessionToDelete by remember { mutableStateOf<AttendanceSession?>(null) }
    var showRenameClass by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var showRosterExport by remember { mutableStateOf(false) }
    val rosterText = remember(group) { buildRosterExport(group) }
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
    val rosterSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(rosterText) }
                    ?: error("无法写入文件")
            }.isSuccess
            scope.launch { snackbarHostState.showSnackbar(if (saved) "名单已保存" else "名单保存失败") }
        }
    }

    Scaffold(
        topBar = {
            SimpleBackBar(group.name, onBack) {
                IconButton(onClick = { showRenameClass = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "修改班级名称")
                }
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
            item {
                OutlinedButton(
                    onClick = { showRosterExport = true },
                    enabled = group.students.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("导出名单", modifier = Modifier.padding(start = 5.dp))
                }
            }
            item { SectionTitle("学生名单 · ${group.students.size}") }
            if (group.students.isEmpty()) {
                item {
                    HintCard("支持 CSV/TXT；推荐表头为“学号,姓名”。重复学号会自动跳过。")
                }
            } else {
                items(group.students, key = { it.id }) { student ->
                    StudentListItem(
                        student = student,
                        showStudentNumber = effectiveSettings.showStudentNumbers,
                        onEdit = { studentToEdit = student },
                        onDelete = { studentToDelete = student },
                    )
                }
            }
            if (sessions.isNotEmpty()) {
                item { SectionTitle("历史记录") }
                items(sessions, key = { it.id }) { session ->
                    HistoryItem(
                        session = session,
                        showStatistics = settings.showHistoryStatistics,
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

    if (showRenameClass) {
        TextInputDialog(
            title = "修改班级名称",
            label = "班级名称",
            confirmText = "保存",
            initialValue = group.name,
            onDismiss = { showRenameClass = false },
            onConfirm = {
                onRenameClass(it)
                showRenameClass = false
            },
        )
    }

    studentToEdit?.let { student ->
        AddStudentDialog(
            title = "修改学生信息",
            confirmText = "保存",
            initialName = student.name,
            initialNumber = student.studentNumber,
            onDismiss = { studentToEdit = null },
            onConfirm = { name, number ->
                onUpdateStudent(student.id, name, number)
                studentToEdit = null
            },
        )
    }

    if (showRosterExport) {
        AlertDialog(
            onDismissRequest = { showRosterExport = false },
            title = { Text("导出班级名单") },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        rosterText,
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
                        clipboard.setPrimaryClip(ClipData.newPlainText("${group.name}名单", rosterText))
                        showRosterExport = false
                        scope.launch { snackbarHostState.showSnackbar("名单已复制") }
                    },
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("复制", modifier = Modifier.padding(start = 4.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRosterExport = false
                        rosterSaveLauncher.launch("${group.name}-名单.csv")
                    },
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("保存文件", modifier = Modifier.padding(start = 4.dp))
                }
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
    initialEntries: List<AttendanceEntry>,
    onDraftChange: (List<AttendanceEntry>) -> Unit,
    onBack: () -> Unit,
    onFinish: (List<AttendanceEntry>) -> Unit,
) {
    val effectiveSettings = settings.forClass(group)
    val marks = remember(group.id) {
        mutableStateMapOf<String, Mark>().apply {
            val currentStudentIds = group.students.mapTo(mutableSetOf()) { it.id }
            initialEntries.forEach { entry ->
                if (entry.studentId in currentStudentIds && entry.status != AttendanceStatus.UNMARKED) {
                    put(entry.studentId, Mark(entry.status, entry.reason))
                }
            }
        }
    }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showClearDraftDialog by remember(group.id) { mutableStateOf(false) }
    val checked = group.students.count { marks[it.id]?.status != null }

    fun updateMark(student: Student, mark: Mark?) {
        if (mark == null) {
            marks.remove(student.id)
        } else {
            marks[student.id] = mark
        }
        onDraftChange(
            group.students.mapNotNull { currentStudent ->
                marks[currentStudent.id]?.let { currentMark ->
                    AttendanceEntry(
                        studentId = currentStudent.id,
                        studentName = currentStudent.name,
                        studentNumber = currentStudent.studentNumber,
                        status = currentMark.status,
                        reason = currentMark.reason,
                    )
                }
            },
        )
    }

    fun applyAction(student: Student, action: GestureAction) {
        when (action) {
            GestureAction.EDIT -> editingStudent = student
            GestureAction.PRESENT -> updateMark(student, Mark(AttendanceStatus.PRESENT))
            GestureAction.LATE -> updateMark(student, Mark(AttendanceStatus.LATE, effectiveSettings.defaultReason))
            GestureAction.LEAVE -> updateMark(student, Mark(AttendanceStatus.LEAVE, effectiveSettings.defaultReason))
            GestureAction.ABSENT -> updateMark(student, Mark(AttendanceStatus.ABSENT, effectiveSettings.defaultReason))
            GestureAction.EXEMPT -> updateMark(student, Mark(AttendanceStatus.EXEMPT))
            GestureAction.CLEAR -> updateMark(student, null)
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
        topBar = {
            TopAppBar(
                title = {
                    Text("${group.name} · 点名", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showClearDraftDialog = true },
                        enabled = marks.isNotEmpty(),
                    ) { Text("清空草稿") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        if (
                            checked < group.students.size &&
                            effectiveSettings.confirmIncompleteAttendance
                        ) {
                            showFinishDialog = true
                        } else {
                            finish()
                        }
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
            if (effectiveSettings.showRollCallProgress || effectiveSettings.showOperationHint) {
                item {
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        if (effectiveSettings.showRollCallProgress) {
                            Text(
                                "$checked / ${group.students.size} 已处理",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (effectiveSettings.showOperationHint) {
                            Text(
                                "点按：${effectiveSettings.defaultStatus.label} · 长按：${effectiveSettings.longPressAction.label} · 左滑：${effectiveSettings.swipeLeftAction.label} · 右滑：${effectiveSettings.swipeRightAction.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            items(group.students, key = { it.id }) { student ->
                val mark = marks[student.id]
                RollCallItem(
                    student = student,
                    mark = mark,
                    iconOption = mark?.status?.let { effectiveSettings.iconFor(it) },
                    colorOption = mark?.status?.let { effectiveSettings.colorFor(it) },
                    showStudentNumber = effectiveSettings.showStudentNumbers,
                    showReason = effectiveSettings.showReasonsInRollCall,
                    showStatusButton = effectiveSettings.showStatusButton,
                    compact = effectiveSettings.compactRollCallRows,
                    onTogglePresent = {
                        if (mark?.status == effectiveSettings.defaultStatus) {
                            updateMark(student, null)
                        } else {
                            updateMark(
                                student,
                                Mark(
                                    effectiveSettings.defaultStatus,
                                    effectiveSettings.defaultReason.takeIf {
                                        statusUsesReason(effectiveSettings.defaultStatus)
                                    }.orEmpty(),
                                ),
                            )
                        }
                    },
                    onLongPress = { applyAction(student, effectiveSettings.longPressAction) },
                    onSwipeLeft = { applyAction(student, effectiveSettings.swipeLeftAction) },
                    onSwipeRight = { applyAction(student, effectiveSettings.swipeRightAction) },
                    swipeSettings = effectiveSettings,
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
                updateMark(student, mark)
                editingStudent = null
            },
        )
    }

    if (showClearDraftDialog) {
        AlertDialog(
            onDismissRequest = { showClearDraftDialog = false },
            title = { Text("清空本班点名草稿？") },
            text = { Text("将清除本班本次点名的全部状态和原因，所有学生恢复为未点。不会删除历史记录或其他班级的草稿，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDraftChange(emptyList())
                    marks.clear()
                    editingStudent = null
                    showFinishDialog = false
                    showClearDraftDialog = false
                }) { Text("清空草稿") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDraftDialog = false }) { Text("取消") }
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
    onUpdateEntry: (String, AttendanceStatus, String) -> Unit,
    onBack: () -> Unit,
) {
    val effectiveSettings = settings.forClass(group)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<AttendanceEntry?>(null) }
    var collapsedStatuses by remember(session.id, effectiveSettings.collapsedResultStatuses) {
        mutableStateOf(effectiveSettings.collapsedResultStatuses)
    }
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
            if (effectiveSettings.showResultSummary) {
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
            }
            if (effectiveSettings.groupResultsByStatus) {
                resultStatuses.forEach { status ->
                    val entries = session.entries.filter { it.status == status }
                    if (entries.isNotEmpty() || effectiveSettings.showEmptyResultGroups) {
                        item(key = "section-${status.name}") {
                            TextButton(
                                onClick = {
                                    collapsedStatuses = if (status in collapsedStatuses) {
                                        collapsedStatuses - status
                                    } else {
                                        collapsedStatuses + status
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${status.label} · ${entries.size}", modifier = Modifier.weight(1f))
                                Icon(
                                    if (status in collapsedStatuses) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (status in collapsedStatuses) "展开" else "折叠",
                                )
                            }
                        }
                        if (status in collapsedStatuses) {
                            // Keep the heading visible while hiding the category's rows.
                        } else if (entries.isEmpty()) {
                            item(key = "empty-${status.name}") { HintCard("此分类暂无学生") }
                        } else {
                            items(entries, key = { "${status.name}-${it.studentId}" }) { entry ->
                                ResultEntryItem(
                                    entry = entry,
                                    settings = effectiveSettings,
                                    onEdit = { entryToEdit = entry },
                                )
                            }
                        }
                    }
                }
            } else {
                item { SectionTitle("人员明细 · ${session.entries.size}") }
                items(session.entries, key = { it.studentId }) { entry ->
                    CompactResultItem(
                        entry = entry,
                        settings = effectiveSettings,
                        onEdit = { entryToEdit = entry },
                    )
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

    entryToEdit?.let { entry ->
        StatusDialog(
            student = Student(entry.studentId, entry.studentName, entry.studentNumber),
            initial = Mark(entry.status, entry.reason),
            presetReasons = effectiveSettings.absenceReasons,
            defaultReason = effectiveSettings.defaultReason,
            onDismiss = { entryToEdit = null },
            onConfirm = { mark ->
                onUpdateEntry(entry.studentId, mark.status, mark.reason)
                entryToEdit = null
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
    showStatistics: Boolean,
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
                        showStatistics = showStatistics,
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
    onSetDisplayOption: (DisplayOption, Boolean) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onSetExportHeader: (Boolean) -> Unit,
    onSetExportSummary: (Boolean) -> Unit,
    onSetExportPresentStudents: (Boolean) -> Unit,
    onSetExportStudentNumber: (Boolean) -> Unit,
    onSetExportReason: (Boolean) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    val pageStateHolder = rememberSaveableStateHolder()
    BackHandler(enabled = page != null) { page = null }
    var showAddReason by remember { mutableStateOf(false) }
    var selector by remember { mutableStateOf<SettingSelector?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(onExportBackup()) }
                    ?: error("无法写入文件")
            }.isSuccess
            scope.launch { snackbarHostState.showSnackbar(if (saved) "备份已保存" else "备份保存失败") }
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取文件")
            }.getOrNull()
            if (content == null) {
                scope.launch { snackbarHostState.showSnackbar("备份读取失败") }
            } else {
                showRestoreConfirmation = content
            }
        }
    }
    Scaffold(
        topBar = {
            if (page == null) RootLargeTopBar("设置")
            else SimpleBackBar(
                title = settingsDestinations.flatten().first { it.key == page }.title,
                onBack = { page = null },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = page, animationSpec = tween(180), label = "settingsPage") { displayedPage ->
                pageStateHolder.SaveableStateProvider(displayedPage ?: "settingsHome") {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (displayedPage == null) {
                            items(settingsDestinations) { destinations ->
                                SettingsDestinationGroup(destinations) { page = it }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        if (displayedPage != null) {
                            item {
                                val destination = settingsDestinations.flatten().first { it.key == displayedPage }
                                SettingsPageIntro(destination)
                            }
                        }
                        if (displayedPage == "操作") {
                            item {
                                SettingsGroup {
                                    SettingRow(
                                        title = "点按默认选择",
                                        value = settings.defaultStatus.label,
                                        onClick = { selector = SettingSelector.DEFAULT_STATUS },
                                    )

                                    SettingRow(
                                        title = "长按姓名",
                                        value = settings.longPressAction.label,
                                        onClick = { selector = SettingSelector.LONG_PRESS },
                                    )

                                    SettingRow(
                                        title = "向左滑动",
                                        value = settings.swipeLeftAction.label,
                                        onClick = { selector = SettingSelector.SWIPE_LEFT },
                                    )

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
                        if (displayedPage == "显示") {
                            item {
                                SettingsGroup(title = "班级首页", subtitle = "首页信息与操作引导") {
                                    SwitchSettingRow(
                                        "显示班级人数",
                                        settings.showClassStudentCount,
                                    ) { onSetDisplayOption(DisplayOption.CLASS_STUDENT_COUNT, it) }

                                    SwitchSettingRow(
                                        "显示班级页操作提示",
                                        settings.showClassOperationHint,
                                    ) { onSetDisplayOption(DisplayOption.CLASS_OPERATION_HINT, it) }

                                }
                            }
                            item {
                                SettingsGroup(title = "点名列表", subtitle = "名单信息、操作控件与行间距") {
                                    SwitchSettingRow(
                                        "显示学生学号",
                                        settings.showStudentNumbers,
                                    ) { onSetDisplayOption(DisplayOption.STUDENT_NUMBERS, it) }

                                    SwitchSettingRow(
                                        "显示已处理进度",
                                        settings.showRollCallProgress,
                                    ) { onSetDisplayOption(DisplayOption.ROLL_CALL_PROGRESS, it) }

                                    SwitchSettingRow(
                                        "显示点名操作提示",
                                        settings.showOperationHint,
                                    ) { onSetDisplayOption(DisplayOption.OPERATION_HINT, it) }

                                    SwitchSettingRow(
                                        "显示状态按钮",
                                        settings.showStatusButton,
                                    ) { onSetDisplayOption(DisplayOption.STATUS_BUTTON, it) }

                                    SwitchSettingRow(
                                        "名单中显示原因",
                                        settings.showReasonsInRollCall,
                                    ) { onSetDisplayOption(DisplayOption.REASONS_IN_ROLL_CALL, it) }

                                    SwitchSettingRow(
                                        "紧凑点名列表",
                                        settings.compactRollCallRows,
                                    ) { onSetDisplayOption(DisplayOption.COMPACT_ROLL_CALL, it) }

                                    SwitchSettingRow(
                                        "未点完时二次确认",
                                        settings.confirmIncompleteAttendance,
                                    ) { onSetDisplayOption(DisplayOption.CONFIRM_INCOMPLETE, it) }

                                }
                            }
                            item {
                                SettingsGroup(title = "结果与历史", subtitle = "统计信息与空分类显示") {
                                    SwitchSettingRow(
                                        "显示结果统计卡",
                                        settings.showResultSummary,
                                    ) { onSetDisplayOption(DisplayOption.RESULT_SUMMARY, it) }

                                    SwitchSettingRow(
                                        "显示空结果分类",
                                        settings.showEmptyResultGroups,
                                    ) { onSetDisplayOption(DisplayOption.EMPTY_RESULT_GROUPS, it) }
                                    SwitchSettingRow(
                                        "显示历史统计",
                                        settings.showHistoryStatistics,
                                    ) { onSetDisplayOption(DisplayOption.HISTORY_STATISTICS, it) }
                                }
                            }
                            item {
                                SettingsGroup(title = "分类折叠", subtitle = "进入结果页时默认收起指定分类，可手动展开") {
                                    resultCollapseOptions.forEach { (option, status) ->

                                        SwitchSettingRow(
                                            "默认折叠${status.label}列表",
                                            status in settings.collapsedResultStatuses,
                                        ) { onSetDisplayOption(option, it) }
                                    }

                                }
                            }
                        }
                        if (displayedPage == "原因") {
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
                        if (displayedPage == "外观") {
                            item {
                                SettingsGroup {
                                    StatusAppearanceRow(
                                        "到场", settings.presentIcon, settings.presentColor,
                                        { selector = SettingSelector.ICON_PRESENT },
                                        { selector = SettingSelector.COLOR_PRESENT },
                                    )

                                    StatusAppearanceRow(
                                        "迟到", settings.lateIcon, settings.lateColor,
                                        { selector = SettingSelector.ICON_LATE },
                                        { selector = SettingSelector.COLOR_LATE },
                                    )

                                    StatusAppearanceRow(
                                        "请假", settings.leaveIcon, settings.leaveColor,
                                        { selector = SettingSelector.ICON_LEAVE },
                                        { selector = SettingSelector.COLOR_LEAVE },
                                    )

                                    StatusAppearanceRow(
                                        "缺勤", settings.absentIcon, settings.absentColor,
                                        { selector = SettingSelector.ICON_ABSENT },
                                        { selector = SettingSelector.COLOR_ABSENT },
                                    )

                                    StatusAppearanceRow(
                                        "不参与", settings.exemptIcon, settings.exemptColor,
                                        { selector = SettingSelector.ICON_EXEMPT },
                                        { selector = SettingSelector.COLOR_EXEMPT },
                                    )
                                }
                            }
                        }
                        if (displayedPage == "结果") {
                            item {
                                SettingsGroup {
                                    SwitchSettingRow(
                                        "按状态分类排列",
                                        settings.groupResultsByStatus,
                                        onSetGroupResultsByStatus,
                                    )
                                }
                            }
                        }
                        if (displayedPage == "导出") {
                            item {
                                SettingsGroup(title = "抬头与统计", subtitle = "选择导出文本的概览信息") {
                                    SwitchSettingRow("班级与点名时间", settings.exportHeader, onSetExportHeader)

                                    SwitchSettingRow("到勤统计", settings.exportSummary, onSetExportSummary)

                                }
                            }
                            item {
                                SettingsGroup(title = "分类明细", subtitle = "选择包含哪些学生；不影响总人数统计") {
                                    SwitchSettingRow("到场学生明细", settings.exportPresentStudents, onSetExportPresentStudents)

                                    SwitchSettingRow("迟到学生明细", settings.exportLateStudents) { onSetDisplayOption(DisplayOption.EXPORT_LATE, it) }

                                    SwitchSettingRow("请假学生明细", settings.exportLeaveStudents) { onSetDisplayOption(DisplayOption.EXPORT_LEAVE, it) }

                                    SwitchSettingRow("缺勤学生明细", settings.exportAbsentStudents) { onSetDisplayOption(DisplayOption.EXPORT_ABSENT, it) }

                                    SwitchSettingRow("不参与学生明细", settings.exportExemptStudents) { onSetDisplayOption(DisplayOption.EXPORT_EXEMPT, it) }

                                }
                            }
                            item {
                                SettingsGroup(title = "人员信息", subtitle = "自定义每名学生的导出字段") {
                                    SwitchSettingRow("学生学号", settings.exportStudentNumber, onSetExportStudentNumber)

                                    SwitchSettingRow("原因或备注", settings.exportReason, onSetExportReason)
                                }
                            }
                        }
                        if (displayedPage == "历史") {
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
                        if (displayedPage == "备份") {
                            item {
                                SettingsGroup {
                                    ActionSettingRow(
                                        title = "导出完整备份",
                                        subtitle = "保存班级、名单、历史记录和全部设置",
                                        icon = Icons.Default.Save,
                                        onClick = { backupExportLauncher.launch("attendance-backup.json") },
                                    )

                                    ActionSettingRow(
                                        title = "从备份恢复",
                                        subtitle = "恢复 JSON 备份并覆盖当前全部数据",
                                        icon = Icons.Default.FileUpload,
                                        onClick = {
                                            backupImportLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                                        },
                                    )
                                }
                            }
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

    showRestoreConfirmation?.let { backupText ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = null },
            title = { Text("恢复完整备份？") },
            text = { Text("当前班级、名单、历史记录和设置将被备份文件覆盖。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val restored = onImportBackup(backupText)
                        showRestoreConfirmation = null
                        scope.launch {
                            snackbarHostState.showSnackbar(if (restored) "备份恢复完成" else "备份格式无效")
                        }
                    },
                ) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = null }) { Text("取消") }
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

                        SettingRow("默认未到原因", draft.defaultReason.ifBlank { "不预填" }) {
                            selector = ClassSettingSelector.DEFAULT_REASON
                        }

                        SettingRow("长按姓名", draft.longPressAction.label) {
                            selector = ClassSettingSelector.LONG_PRESS
                        }

                        SettingRow("向左滑动", draft.swipeLeftAction.label) {
                            selector = ClassSettingSelector.SWIPE_LEFT
                        }

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
                item { SectionTitle("界面显示") }
                item {
                    SettingsGroup {
                        SwitchSettingRow("显示学生学号", draft.showStudentNumbers) {
                            draft = draft.copy(showStudentNumbers = it)
                        }

                        SwitchSettingRow("显示已处理进度", draft.showRollCallProgress) {
                            draft = draft.copy(showRollCallProgress = it)
                        }

                        SwitchSettingRow("显示点名操作提示", draft.showOperationHint) {
                            draft = draft.copy(showOperationHint = it)
                        }

                        SwitchSettingRow("显示状态按钮", draft.showStatusButton) {
                            draft = draft.copy(showStatusButton = it)
                        }

                        SwitchSettingRow("名单中显示原因", draft.showReasonsInRollCall) {
                            draft = draft.copy(showReasonsInRollCall = it)
                        }

                        SwitchSettingRow("紧凑点名列表", draft.compactRollCallRows) {
                            draft = draft.copy(compactRollCallRows = it)
                        }

                        SwitchSettingRow("未点完时二次确认", draft.confirmIncompleteAttendance) {
                            draft = draft.copy(confirmIncompleteAttendance = it)
                        }

                        SwitchSettingRow("显示结果统计卡", draft.showResultSummary) {
                            draft = draft.copy(showResultSummary = it)
                        }

                        SwitchSettingRow("显示空结果分类", draft.showEmptyResultGroups) {
                            draft = draft.copy(showEmptyResultGroups = it)
                        }
                        resultCollapseOptions.values.forEach { status ->

                            SwitchSettingRow("默认折叠${status.label}列表", status in draft.collapsedResultStatuses) { enabled ->
                                draft = draft.copy(
                                    collapsedResultStatuses = if (enabled) draft.collapsedResultStatuses + status
                                        else draft.collapsedResultStatuses - status,
                                )
                            }
                        }
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

private data class SettingsDestination(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val settingsDestinations = listOf(
    listOf(
        SettingsDestination("操作", "点名操作", "点按默认状态、长按与左右滑动", Icons.Default.Settings),
        SettingsDestination("原因", "未到原因", "常用原因、默认选择与拖动排序", Icons.Default.EventBusy),
    ),
    listOf(
        SettingsDestination("外观", "状态外观", "到场、请假等状态的图标与颜色", Icons.Default.Palette),
        SettingsDestination("显示", "界面显示", "控件、文字、紧凑布局与默认折叠", Icons.Default.Person),
        SettingsDestination("结果", "结果排列", "按状态分类或显示完整人员列表", Icons.Default.Groups),
    ),
    listOf(
        SettingsDestination("导出", "文本导出", "分类明细、统计、学号与原因", Icons.Default.Save),
        SettingsDestination("历史", "历史记录", "班级名称与点名时间标题", Icons.Default.History),
        SettingsDestination("备份", "数据备份", "完整备份与恢复班级、历史和草稿", Icons.Default.FileUpload),
    ),
)

@Composable
private fun SettingsDestinationGroup(destinations: List<SettingsDestination>, onOpen: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
            .compositeOver(MaterialTheme.colorScheme.surface),
    ) {
        Column {
            destinations.forEachIndexed { index, destination ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(destination.key) }
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(destination.icon, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(destination.title, style = MaterialTheme.typography.titleMedium)
                        Text(destination.subtitle, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            .compositeOver(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.animateContentSize(animationSpec = tween(180)).padding(vertical = 8.dp)) {
            if (title != null) {
                Text(title, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
            }
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))
            }
            content()
        }
    }
}

@Composable
private fun SettingsPageIntro(destination: SettingsDestination) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
            .compositeOver(MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(destination.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(destination.title, style = MaterialTheme.typography.titleMedium)
                Text(destination.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingsRowIcon(title: String) {
    val icon = when {
        title.contains("折叠") -> Icons.Default.ExpandMore
        title.contains("历史") || title.contains("时间") || title.contains("记录") -> Icons.Default.History
        title.contains("原因") || title.contains("请假") -> Icons.Default.EventBusy
        title.contains("迟到") -> Icons.Default.Schedule
        title.contains("缺勤") -> Icons.Default.Close
        title.contains("不参与") -> Icons.Default.RemoveCircle
        title.contains("统计") || title.contains("进度") || title.contains("到场") -> Icons.Default.CheckCircle
        title.contains("学号") || title.contains("名单") || title.contains("学生") -> Icons.Default.Person
        title.contains("班级") -> Icons.Default.Groups
        title.contains("提示") || title.contains("确认") -> Icons.Default.Help
        else -> Icons.Default.Settings
    }
    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(18.dp))
}

private fun settingDescription(title: String): String? = when (title) {
    "显示班级人数" -> "在首页班级卡片中显示名单人数"
    "显示班级页操作提示" -> "显示点击点名、长按编辑的操作说明"
    "显示学生学号" -> "在名单、点名和结果中显示学号"
    "显示已处理进度" -> "显示已点人数与本班总人数"
    "显示点名操作提示" -> "显示点按、长按和左右滑动对应的操作"
    "显示状态按钮" -> "在姓名右侧提供状态选择入口"
    "名单中显示原因" -> "在点名列表中显示已填写的原因或备注"
    "紧凑点名列表" -> "缩小行间距，一屏显示更多学生"
    "未点完时二次确认" -> "还有未点学生时，结束前再次确认"
    "显示结果统计卡" -> "显示各状态的人数统计"
    "显示空结果分类" -> "分类没有学生时仍显示分类标题"
    "显示历史统计" -> "在历史列表中显示到场与缺勤等人数"
    "按状态分类排列" -> "结果与导出按到场、请假等状态分组"
    "班级与点名时间" -> "在导出文本开头显示班名与时间"
    "到勤统计" -> "导出完整人数统计，独立于明细筛选"
    "学生学号" -> "在导出的姓名前显示学号"
    "原因或备注" -> "将填写的原因附在导出的姓名后"
    "覆盖全局点名设置" -> "为当前班级单独保存操作与显示偏好"
    else -> when {
        title.startsWith("默认折叠") -> "进入结果页时收起此分类，点击标题可展开"
        title.endsWith("学生明细") -> "在导出文本中包含此分类的学生名单"
        else -> null
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowIcon(title)
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("当前：$value", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionSettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("›", fontSize = 22.sp)
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
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).padding(20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowIcon(title)
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            settingDescription(title)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Switch(checked = checked, onCheckedChange = null)
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
    showStudentNumber: Boolean,
    showReason: Boolean,
    showStatusButton: Boolean,
    compact: Boolean,
    onTogglePresent: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeSettings: AppSettings,
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
    // Pre-composite the tint so the swipe background cannot bleed through the card.
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 1f)
    val container = markColor?.copy(alpha = 0.16f)?.compositeOver(surfaceColor) ?: surfaceColor

    val swipeAction = if (horizontalOffset > 0f) swipeSettings.swipeRightAction else swipeSettings.swipeLeftAction
    val swipeStatus = when (swipeAction) {
        GestureAction.PRESENT -> AttendanceStatus.PRESENT
        GestureAction.LATE -> AttendanceStatus.LATE
        GestureAction.LEAVE -> AttendanceStatus.LEAVE
        GestureAction.ABSENT -> AttendanceStatus.ABSENT
        GestureAction.EXEMPT -> AttendanceStatus.EXEMPT
        GestureAction.EDIT, GestureAction.CLEAR -> null
    }
    val swipeColor = swipeStatus?.let { statusColor(swipeSettings.colorFor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val swipeIcon = swipeStatus?.let { statusImageVector(swipeSettings.iconFor(it)) }
        ?: if (swipeAction == GestureAction.EDIT) Icons.Default.Edit else Icons.Default.RemoveCircle
    val swipeText = swipeStatus?.label ?: if (swipeAction == GestureAction.EDIT) "状态" else "清除"

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
        if (horizontalOffset != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // Fill behind the foreground's rounded corners before clipping labels.
                    .background(swipeColor.copy(alpha = 0.2f).compositeOver(surfaceColor))
                    .drawWithContent {
                        // Reveal only the strip vacated by the foreground card, including
                        // during the return animation. Never draw labels beneath its content.
                        val revealed = horizontalOffset.roundToInt().toFloat()
                            .coerceIn(-size.width, size.width)
                        clipRect(
                            left = if (revealed >= 0f) 0f else size.width + revealed,
                            right = if (revealed >= 0f) revealed else size.width,
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = if (horizontalOffset > 0f) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (horizontalOffset > 0f) Icon(swipeIcon, null, tint = swipeColor, modifier = Modifier.size(20.dp))
                    Text(swipeText, color = MaterialTheme.colorScheme.onSurface)
                    if (horizontalOffset < 0f) Icon(swipeIcon, null, tint = swipeColor, modifier = Modifier.size(20.dp))
                }
            }
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
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = if (compact) 12.dp else 14.dp,
                    vertical = if (compact) 5.dp else 10.dp,
                ),
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
                        student.studentNumber.takeIf { showStudentNumber && it.isNotBlank() },
                        mark?.status?.label,
                        mark?.reason?.takeIf { showReason && it.isNotBlank() },
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
                if (showStatusButton) {
                    TextButton(onClick = onEdit) { Text("状态") }
                }
            }
        }
    }
}

@Composable
private fun StudentListItem(
    student: Student,
    showStudentNumber: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
                if (showStudentNumber && student.studentNumber.isNotBlank()) {
                    Text(
                        student.studentNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "修改学生信息")
            }
            TextButton(onClick = onDelete) { Text("移除") }
        }
    }
}

@Composable
private fun HistoryItem(
    session: AttendanceSession,
    showStatistics: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
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
                if (showStatistics) {
                    Text(
                        "共 ${session.entries.size} 人 · 缺勤 $absent · 请假 $leave · 迟到 $late",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    showStatistics: Boolean,
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
                    if (showStatistics) {
                        "$detailPrefix · 到 $present · 缺勤 $absent · 不参与 $exempt"
                    } else {
                        detailPrefix
                    },
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
private fun ResultEntryItem(
    entry: AttendanceEntry,
    settings: AppSettings,
    onEdit: () -> Unit,
) {
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
                if (settings.showStudentNumbers && entry.studentNumber.isNotBlank()) {
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
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑${entry.studentName}的点名记录",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            StatusBadge(entry.status, settings.iconFor(entry.status), settings.colorFor(entry.status))
        }
    }
}

@Composable
private fun CompactResultItem(
    entry: AttendanceEntry,
    settings: AppSettings,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(entry.studentName, modifier = Modifier.weight(1f))
        if (settings.showStudentNumbers && entry.studentNumber.isNotBlank()) {
            Text(
                entry.studentNumber,
                modifier = Modifier.padding(end = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑${entry.studentName}的点名记录",
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
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
    title: String = "添加学生",
    confirmText: String = "添加",
    initialName: String = "",
    initialNumber: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var number by remember(initialNumber) { mutableStateOf(initialNumber) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, number) }) { Text(confirmText) }
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
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
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

private fun buildRosterExport(group: ClassGroup): String = buildString {
    appendLine("学号,姓名")
    group.students.forEach { student ->
        append(csvCell(student.studentNumber))
        append(',')
        appendLine(csvCell(student.name))
    }
}.trimEnd()

private fun csvCell(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) {
    "\"${value.replace("\"", "\"\"")}\""
} else {
    value
}

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
        when (it.status) {
            AttendanceStatus.PRESENT -> settings.exportPresentStudents
            AttendanceStatus.LATE -> settings.exportLateStudents
            AttendanceStatus.LEAVE -> settings.exportLeaveStudents
            AttendanceStatus.ABSENT -> settings.exportAbsentStudents
            AttendanceStatus.EXEMPT -> settings.exportExemptStudents
            AttendanceStatus.UNMARKED -> false
        }
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
