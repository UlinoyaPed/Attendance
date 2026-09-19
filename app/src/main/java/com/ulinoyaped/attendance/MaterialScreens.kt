package com.ulinoyaped.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.MaterialRecordStatus
import com.ulinoyaped.attendance.data.MaterialTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTasksScreen(
    group: ClassGroup,
    onBack: () -> Unit,
    onCreate: (String, List<String>) -> String,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MaterialTask?>(null) }
    val active = group.materialTasks.filter { it.completedAt == null }.sortedByDescending { it.updatedAt }
    val completed = group.materialTasks.filter { it.completedAt != null }.sortedByDescending { it.updatedAt }
    Scaffold(
        topBar = { TopAppBar(
            title = { Text("材料登记") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        ) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("任务创建后立即保存，可随时从历史返回修改登记状态。", style = MaterialTheme.typography.bodyMedium) }
            item { Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("新建材料任务")
            } }
            if (active.isNotEmpty()) item { MaterialSectionTitle("进行中") }
            items(active, key = { it.id }) { task -> MaterialTaskCard(group, task, { onOpen(task.id) }, { deleting = task }) }
            if (completed.isNotEmpty()) item { MaterialSectionTitle("历史") }
            items(completed, key = { it.id }) { task -> MaterialTaskCard(group, task, { onOpen(task.id) }, { deleting = task }) }
            if (group.materialTasks.isEmpty()) item {
                Text("还没有材料任务", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            }
        }
    }
    if (creating) MaterialTaskCreateDialog(
        onDismiss = { creating = false },
        onConfirm = { title, names ->
            val taskId = onCreate(title, names)
            creating = false
            onOpen(taskId)
        },
    )
    deleting?.let { task -> AlertDialog(
        onDismissRequest = { deleting = null },
        title = { Text("删除材料任务？") },
        text = { Text("“${task.title}”及全部登记状态将被删除。") },
        confirmButton = { TextButton(onClick = { onDelete(task.id); deleting = null }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
    ) }
}

@Composable
private fun MaterialTaskCard(group: ClassGroup, task: MaterialTask, onOpen: () -> Unit, onDelete: () -> Unit) {
    val total = group.students.size * task.materials.size
    val completed = task.records.count { it.status == MaterialRecordStatus.COMPLETED }
    val rejected = task.records.count { it.status == MaterialRecordStatus.REJECTED }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            Text("${task.materials.size} 份材料 · 完成 $completed/$total${if (rejected > 0) " · 不合格 $rejected" else ""}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTaskScreen(
    group: ClassGroup,
    task: MaterialTask,
    onBack: () -> Unit,
    onSetStatus: (String, String, MaterialRecordStatus) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val statusByKey = remember(task.records) { task.records.associateBy { it.studentId to it.materialId } }
    Scaffold(topBar = { TopAppBar(
        title = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        actions = {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "删除任务") }
            TextButton(onClick = { onSetCompleted(task.completedAt == null) }) {
                Text(if (task.completedAt == null) "完成" else "重新编辑")
            }
        },
    ) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val total = group.students.size * task.materials.size
                val done = task.records.count { it.status == MaterialRecordStatus.COMPLETED }
                val rejected = task.records.count { it.status == MaterialRecordStatus.REJECTED }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${task.materials.joinToString("、") { it.name }}", style = MaterialTheme.typography.titleMedium)
                        Text("已完成 $done/$total · 不合格 $rejected", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(group.students, key = { it.id }) { student ->
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(student.name, style = MaterialTheme.typography.titleMedium)
                        if (student.studentNumber.isNotBlank()) Text(student.studentNumber, style = MaterialTheme.typography.bodySmall)
                        task.materials.forEach { material ->
                            val current = statusByKey[student.id to material.id]?.status ?: MaterialRecordStatus.PENDING
                            Text(material.name, style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MaterialStatusChip("未登记", current == MaterialRecordStatus.PENDING) {
                                    onSetStatus(student.id, material.id, MaterialRecordStatus.PENDING)
                                }
                                MaterialStatusChip("已完成", current == MaterialRecordStatus.COMPLETED) {
                                    onSetStatus(student.id, material.id, MaterialRecordStatus.COMPLETED)
                                }
                                MaterialStatusChip("不合格", current == MaterialRecordStatus.REJECTED) {
                                    onSetStatus(student.id, material.id, MaterialRecordStatus.REJECTED)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("删除材料任务？") },
        text = { Text("“${task.title}”及全部登记状态将被删除。") },
        confirmButton = { TextButton(onClick = onDelete) { Text("删除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
    )
}

@Composable
private fun RowScope.MaterialStatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun MaterialTaskCreateDialog(onDismiss: () -> Unit, onConfirm: (String, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    val names = materials.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.distinct().toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建材料任务") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = safeMaterialText(it, 120) }, label = { Text("任务名称") }, singleLine = true)
            OutlinedTextField(
                materials, { materials = safeMaterialText(it, 3000, allowNewline = true) },
                label = { Text("材料名称，每行一份") }, minLines = 4, maxLines = 8,
                supportingText = { Text("已填写 ${names.size} 份") },
            )
        } },
        confirmButton = { TextButton(onClick = { onConfirm(title.trim(), names) }, enabled = title.isNotBlank() && names.isNotEmpty() && names.size <= 20) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun MaterialSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

private fun safeMaterialText(value: String, max: Int, allowNewline: Boolean = false): String = value.take(max).filterNot {
    (it.isISOControl() && !(allowNewline && it == '\n')) || it == '\u2028' || it == '\u2029'
}
