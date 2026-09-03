package com.ulinoyaped.attendance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.Student
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Screen {
    data object Classes : Screen
    data class ClassDetail(val classId: String) : Screen
    data class RollCall(val classId: String) : Screen
    data class Result(val classId: String, val sessionId: String) : Screen
}

private data class Mark(
    val status: AttendanceStatus,
    val reason: String = "",
)

@Composable
fun AttendanceApp() {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository(context.applicationContext) }
    val classes by repository.classes.collectAsStateWithLifecycle()
    val sessions by repository.sessions.collectAsStateWithLifecycle()
    var screen: Screen by remember { mutableStateOf(Screen.Classes) }

    when (val current = screen) {
        Screen.Classes -> ClassesScreen(
            classes = classes,
            onAddClass = repository::addClass,
            onOpenClass = { screen = Screen.ClassDetail(it) },
            onDeleteClass = repository::deleteClass,
        )

        is Screen.ClassDetail -> {
            val group = classes.firstOrNull { it.id == current.classId }
            if (group == null) {
                screen = Screen.Classes
            } else {
                ClassDetailScreen(
                    group = group,
                    sessions = sessions.filter { it.classId == group.id },
                    onBack = { screen = Screen.Classes },
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
                screen = Screen.Classes
            } else {
                RollCallScreen(
                    group = group,
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
                screen = Screen.Classes
            } else {
                ResultScreen(
                    group = group,
                    session = session,
                    onBack = { screen = Screen.ClassDetail(group.id) },
                )
            }
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
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var classToDelete by remember { mutableStateOf<ClassGroup?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的班级") }) },
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
                        onClick = {
                            importLauncher.launch(
                                arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel"),
                            )
                        },
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
    onBack: () -> Unit,
    onFinish: (List<AttendanceEntry>) -> Unit,
) {
    val marks = remember(group.id) { mutableStateMapOf<String, Mark>() }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val checked = group.students.count { marks[it.id]?.status != null }

    fun finish() {
        val entries = group.students.map { student ->
            val mark = marks[student.id] ?: Mark(AttendanceStatus.ABSENT)
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
                        "点按学生卡片标记到场；点“状态”可填写迟到、请假或缺勤原因。",
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
                        if (mark?.status == AttendanceStatus.PRESENT) {
                            marks.remove(student.id)
                        } else {
                            marks[student.id] = Mark(AttendanceStatus.PRESENT)
                        }
                    },
                    onEdit = { editingStudent = student },
                )
            }
        }
    }

    editingStudent?.let { student ->
        StatusDialog(
            student = student,
            initial = marks[student.id] ?: Mark(AttendanceStatus.PRESENT),
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

@Composable
private fun RollCallItem(
    student: Student,
    mark: Mark?,
    onTogglePresent: () -> Unit,
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
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onTogglePresent),
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
                        modifier = Modifier.fillMaxWidth().clickable { status = option }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = status == option, onClick = { status = option })
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
