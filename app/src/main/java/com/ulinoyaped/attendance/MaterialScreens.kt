package com.ulinoyaped.attendance

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.MaterialRecordStatus
import com.ulinoyaped.attendance.data.MaterialTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTaskScreen(
    task: MaterialTask,
    onBack: () -> Unit,
    onSetStatus: (String, String, MaterialRecordStatus) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var editingRecord by remember(task.id) { mutableStateOf<Pair<String, String>?>(null) }
    val statusByKey = remember(task.records) { task.records.associateBy { it.studentId to it.materialId } }
    Scaffold(topBar = { TopAppBar(
        title = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "删除登记") }
            TextButton(onClick = { onSetCompleted(task.completedAt == null) }) {
                Text(if (task.completedAt == null) "结束登记" else "继续编辑")
            }
        },
    ) }) { padding ->
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
                        Text("已完成 $done/$total · 不合格 $rejected", style = MaterialTheme.typography.bodyMedium)
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
                    Column(Modifier.fillMaxWidth().padding(14.dp, 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val complete = completedForStudent == task.materials.size
                            val avatarColor = when {
                                rejectedForStudent > 0 -> MaterialTheme.colorScheme.error
                                complete -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = avatarColor) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when {
                                            rejectedForStudent > 0 -> Icons.Default.Close
                                            complete -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = null,
                                        tint = if (complete || rejectedForStudent > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            StudentIdentityText(
                                student = student,
                                detail = listOfNotNull(
                                    student.studentNumber.takeIf { it.isNotBlank() },
                                    "已完成 $completedForStudent/${task.materials.size}",
                                    "不合格 $rejectedForStudent".takeIf { rejectedForStudent > 0 },
                                ).joinToString(" · "),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        task.materials.forEach { material ->
                            val current = statusByKey[student.id to material.id]?.status ?: MaterialRecordStatus.PENDING
                            MaterialRecordRow(
                                name = material.name,
                                status = current,
                                onToggle = {
                                    onSetStatus(
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
                                    onSetStatus(studentId, materialId, status)
                                    editingRecord = null
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = current == status, onClick = {
                                    onSetStatus(studentId, materialId, status)
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
}

@Composable
private fun MaterialRecordRow(
    name: String,
    status: MaterialRecordStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val tint = when (status) {
        MaterialRecordStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        MaterialRecordStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        MaterialRecordStatus.REJECTED -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
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
            TextButton(onClick = onEdit) { Text("状态") }
        }
    }
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
