package com.ulinoyaped.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.MaterialRecordStatus
import com.ulinoyaped.attendance.data.MaterialRecord
import com.ulinoyaped.attendance.data.MaterialTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTaskScreen(
    task: MaterialTask,
    settings: AppSettings,
    onBack: () -> Unit,
    onSetStatus: (String, String, MaterialRecordStatus) -> Unit,
    onCompleteStudent: (String, Boolean) -> Unit,
    onReadRecords: () -> List<MaterialRecord>,
    onRestoreRecords: (List<MaterialRecord>, List<MaterialRecord>) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var editingRecord by remember(task.id) { mutableStateOf<Pair<String, String>?>(null) }
    var confirmingStudent by remember(task.id) { mutableStateOf<String?>(null) }
    val undo = rememberRecentOperationUndo(task.id)
    val statusByKey = remember(task.records) { task.records.associateBy { it.studentId to it.materialId } }

    fun changeRecords(message: String, action: () -> Unit) {
        val before = onReadRecords().toList()
        action()
        val after = onReadRecords().toList()
        if (before != after) undo.offer(message) { onRestoreRecords(after, before) }
    }

    fun setStatus(studentId: String, materialId: String, status: MaterialRecordStatus) {
        val student = task.participants.first { it.id == studentId }
        val material = task.materials.first { it.id == materialId }
        changeRecords("${student.name} · ${material.name}已标记为${materialStatusLabel(status)}") {
            onSetStatus(studentId, materialId, status)
        }
    }

    fun completeStudent(studentId: String, overwriteRejected: Boolean = false) {
        val student = task.participants.first { it.id == studentId }
        changeRecords(if (overwriteRejected) "已确认${student.name}的全部材料" else "已确认${student.name}的未登记材料") {
            onCompleteStudent(studentId, overwriteRejected)
        }
    }
    Scaffold(topBar = { TopAppBar(
        title = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "删除登记") }
            TextButton(onClick = { undo.clear(); onSetCompleted(task.completedAt == null) }) {
                Text(if (task.completedAt == null) "结束登记" else "继续编辑")
            }
        },
    ) }, snackbarHost = { SnackbarHost(undo.host) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val total = task.participants.size * task.materials.size
                val done = task.records.count { it.status == MaterialRecordStatus.COMPLETED }
                val rejected = task.records.count { it.status == MaterialRecordStatus.REJECTED }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${task.materials.joinToString("、") { it.name }}", style = MaterialTheme.typography.titleMedium)
                        if (settings.showMaterialProgress) Text("已完成 $done/$total · 不合格 $rejected", style = MaterialTheme.typography.bodyMedium)
                        if (settings.showMaterialOperationHint) Text(
                            "右滑确认 · 左滑不合格 · 长按修改状态" + if (settings.materialAvatarCompletesAll) " · 头像确认未登记材料" else "",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            items(task.participants, key = { it.id }) { student ->
                val completedForStudent = task.materials.count { material ->
                    statusByKey[student.id to material.id]?.status == MaterialRecordStatus.COMPLETED
                }
                val rejectedForStudent = task.materials.count { material ->
                    statusByKey[student.id to material.id]?.status == MaterialRecordStatus.REJECTED
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp, if (settings.compactMaterialRows) 6.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(if (settings.compactMaterialRows) 4.dp else 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val complete = completedForStudent == task.materials.size
                            val avatarColor = when {
                                rejectedForStudent > 0 -> MaterialTheme.colorScheme.error
                                complete -> materialStatusColor(MaterialRecordStatus.COMPLETED)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(onClick = {
                                if (onReadRecords().any { it.studentId == student.id && it.status == MaterialRecordStatus.REJECTED }) confirmingStudent = student.id
                                else completeStudent(student.id)
                            }, enabled = settings.materialAvatarCompletesAll, modifier = Modifier.size(48.dp), shape = CircleShape, color = avatarColor) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when {
                                            rejectedForStudent > 0 -> Icons.Default.Close
                                            complete -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = "确认${student.name}的全部材料",
                                        tint = if (rejectedForStudent > 0) MaterialTheme.colorScheme.onError
                                            else if (complete) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            StudentIdentityText(
                                student = student,
                                detail = listOfNotNull(
                                    student.studentNumber.takeIf { settings.showStudentNumbers && it.isNotBlank() },
                                    "已完成 $completedForStudent/${task.materials.size}".takeIf { settings.showMaterialProgress },
                                    "不合格 $rejectedForStudent".takeIf { settings.showMaterialProgress && rejectedForStudent > 0 },
                                ).joinToString(" · "),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        task.materials.forEach { material ->
                            val current = statusByKey[student.id to material.id]?.status ?: MaterialRecordStatus.PENDING
                            MaterialRecordRow(
                                showStatusButton = settings.showMaterialStatusButton,
                                compact = settings.compactMaterialRows,
                                recordKey = "${task.id}/${student.id}/${material.id}",
                                onReject = { setStatus(student.id, material.id, MaterialRecordStatus.REJECTED) },
                                onComplete = { setStatus(student.id, material.id, MaterialRecordStatus.COMPLETED) },
                                name = material.name,
                                status = current,
                                onToggle = {
                                    setStatus(
                                        student.id,
                                        material.id,
                                        if (current == MaterialRecordStatus.COMPLETED) MaterialRecordStatus.PENDING
                                        else MaterialRecordStatus.COMPLETED,
                                    )
                                },
                                onEdit = { editingRecord = student.id to material.id },
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("删除材料登记？") },
        text = { Text("“${task.title}”及全部登记状态将被删除。") },
        confirmButton = { TextButton(onClick = onDelete) { Text("删除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
    )
    editingRecord?.let { (studentId, materialId) ->
        val student = task.participants.firstOrNull { it.id == studentId }
        val material = task.materials.firstOrNull { it.id == materialId }
        if (student != null && material != null) {
            val current = statusByKey[studentId to materialId]?.status ?: MaterialRecordStatus.PENDING
            AlertDialog(
                onDismissRequest = { editingRecord = null },
                title = { Text("${student.name} · ${material.name}") },
                text = {
                    Column {
                        MaterialRecordStatus.entries.forEach { status ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    setStatus(studentId, materialId, status)
                                    editingRecord = null
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = current == status, onClick = {
                                    setStatus(studentId, materialId, status)
                                    editingRecord = null
                                })
                                Text(materialStatusLabel(status))
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { editingRecord = null }) { Text("取消") } },
            )
        }
    }
    confirmingStudent?.let { studentId ->
        val student = task.participants.first { it.id == studentId }
        val rejected = onReadRecords().count { it.studentId == studentId && it.status == MaterialRecordStatus.REJECTED }
        AlertDialog(
            onDismissRequest = { confirmingStudent = null },
            title = { Text("${student.name}有 $rejected 份不合格材料") },
            text = { Column {
                Text("默认保留不合格结果，只确认未登记材料。")
                TextButton(onClick = { completeStudent(studentId, true); confirmingStudent = null }) { Text("覆盖不合格，确认全部材料") }
            } },
            confirmButton = { TextButton(onClick = { completeStudent(studentId); confirmingStudent = null }) { Text("只确认未登记") } },
            dismissButton = { TextButton(onClick = { confirmingStudent = null }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MaterialRecordRow(
    recordKey: String,
    showStatusButton: Boolean,
    compact: Boolean,
    onReject: () -> Unit,
    onComplete: () -> Unit,
    name: String,
    status: MaterialRecordStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when (status) {
        MaterialRecordStatus.PENDING -> scheme.onSurfaceVariant
        MaterialRecordStatus.COMPLETED -> scheme.onPrimary
        MaterialRecordStatus.REJECTED -> scheme.onErrorContainer
    }
    SwipeRecordContainer(
        key = recordKey,
        left = SwipeActionVisual("不合格", MaterialTheme.colorScheme.error, Icons.Default.Close),
        right = SwipeActionVisual("已完成", materialStatusColor(MaterialRecordStatus.COMPLETED), Icons.Default.CheckCircle),
        onSwipeLeft = onReject,
        onSwipeRight = onComplete,
    ) { swipeModifier ->
        Surface(
            modifier = swipeModifier.fillMaxWidth().combinedClickable(onClick = onToggle, onLongClick = onEdit),
            shape = RoundedCornerShape(12.dp),
            color = when (status) {
                MaterialRecordStatus.PENDING -> scheme.surface
                MaterialRecordStatus.COMPLETED -> scheme.primary
                MaterialRecordStatus.REJECTED -> scheme.errorContainer
            },
            contentColor = tint,
            border = if (status == MaterialRecordStatus.PENDING) BorderStroke(1.dp, scheme.outlineVariant) else null,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = if (compact) 2.dp else 5.dp, bottom = if (compact) 2.dp else 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when (status) {
                        MaterialRecordStatus.PENDING -> Icons.Default.Inventory2
                        MaterialRecordStatus.COMPLETED -> Icons.Default.CheckCircle
                        MaterialRecordStatus.REJECTED -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(materialStatusLabel(status), style = MaterialTheme.typography.bodySmall, color = tint)
                }
                if (showStatusButton) TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = tint)) { Text("状态") }
            }
        }
    }
}

@Composable
private fun materialStatusColor(status: MaterialRecordStatus): Color = when (status) {
    MaterialRecordStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    MaterialRecordStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    MaterialRecordStatus.REJECTED -> MaterialTheme.colorScheme.error
}

private fun materialStatusLabel(status: MaterialRecordStatus): String = when (status) {
    MaterialRecordStatus.PENDING -> "未登记"
    MaterialRecordStatus.COMPLETED -> "已完成"
    MaterialRecordStatus.REJECTED -> "不合格"
}

@Composable
internal fun MaterialTaskCreateDialog(onDismiss: () -> Unit, onConfirm: (String, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    val names = materials.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.distinct().toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开始材料登记") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = safeMaterialText(it, 120) }, label = { Text("本次登记名称") }, singleLine = true)
            OutlinedTextField(
                materials, { materials = safeMaterialText(it, 3000, allowNewline = true) },
                label = { Text("材料名称，每行一份") }, minLines = 4, maxLines = 8,
                supportingText = { Text("已填写 ${names.size} 份") },
            )
        } },
        confirmButton = { TextButton(onClick = { onConfirm(title.trim(), names) }, enabled = title.isNotBlank() && names.isNotEmpty() && names.size <= 20) { Text("开始") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun safeMaterialText(value: String, max: Int, allowNewline: Boolean = false): String = value.take(max).filterNot {
    (it.isISOControl() && !(allowNewline && it == '\n')) || it == '\u2028' || it == '\u2029'
}
