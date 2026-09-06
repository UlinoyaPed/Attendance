package com.ulinoyaped.attendance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.ulinoyaped.attendance.data.AttendanceStatus
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.ClassSituation
import com.ulinoyaped.attendance.data.SituationAssignment
import com.ulinoyaped.attendance.data.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SituationScreen(
    group: ClassGroup,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetAssignment: (String, String, AttendanceStatus, String) -> Unit,
    onRemoveAssignment: (String, String) -> Unit,
) {
    var selected by remember(group.id) { mutableStateOf<ClassSituation?>(null) }
    var nameDialog by remember { mutableStateOf<ClassSituation?>(null) }
    var addingName by remember { mutableStateOf(false) }
    BackHandler(enabled = selected != null) { selected = null }
    val current = selected?.let { chosen -> group.situations.firstOrNull { it.id == chosen.id } }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(current?.name ?: "班级情况", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = { if (current != null) selected = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            } },
            actions = { if (current != null) IconButton(onClick = { nameDialog = current }) {
                Icon(Icons.Default.Edit, "修改名称")
            } },
        )
    }) { padding ->
        if (current == null) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text("将长期请假、不参与或固定迟到原因按学生预先保存；使用时只覆写这里列出的学生。", style = MaterialTheme.typography.bodyMedium) }
                items(group.situations, key = { it.id }) { situation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selected = situation },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    ) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(situation.name, style = MaterialTheme.typography.titleMedium)
                            Text("${situation.assignments.size} 名学生", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDelete(situation.id) }) { Icon(Icons.Default.Delete, "删除") }
                    } }
                }
                item { Button(onClick = { addingName = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("添加情况")
                } }
            }
        } else {
            SituationAssignments(
                modifier = Modifier.padding(padding), group = group, situation = current,
                onSet = { studentId, status, reason -> onSetAssignment(current.id, studentId, status, reason) },
                onRemove = { onRemoveAssignment(current.id, it) },
            )
        }
    }
    if (addingName || nameDialog != null) {
        SituationNameDialog(
            initial = nameDialog?.name.orEmpty(),
            onDismiss = { addingName = false; nameDialog = null },
            onConfirm = { value ->
                nameDialog?.let { onRename(it.id, value) } ?: onAdd(value)
                addingName = false; nameDialog = null
            },
        )
    }
}

@Composable
private fun SituationAssignments(
    modifier: Modifier,
    group: ClassGroup,
    situation: ClassSituation,
    onSet: (String, AttendanceStatus, String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<Student?>(null) }
    var adding by remember { mutableStateOf(false) }
    val studentById = group.students.associateBy { it.id }
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("逐个添加和覆写。一个学生在同一情况中只有一条规则。", style = MaterialTheme.typography.bodyMedium) }
        items(situation.assignments, key = { it.studentId }) { assignment ->
            val student = studentById[assignment.studentId] ?: return@items
            Card(
                Modifier.fillMaxWidth().clickable { adding = false; editing = student },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            ) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium)
                    Text(listOf(assignment.status.label, assignment.reason).filter { it.isNotBlank() }.joinToString(" · "))
                }
                IconButton(onClick = { onRemove(student.id) }) { Icon(Icons.Default.Delete, "移除") }
            } }
        }
        item { Button(onClick = { adding = true; editing = group.students.firstOrNull() }, enabled = group.students.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("添加或覆写学生")
        } }
    }
    editing?.let { initialStudent ->
        AssignmentDialog(
            students = group.students,
            initialStudent = initialStudent,
            initial = if (adding) null else situation.assignments.firstOrNull { it.studentId == initialStudent.id },
            onDismiss = { adding = false; editing = null },
            onConfirm = { student, status, reason -> onSet(student.id, status, reason); adding = false; editing = null },
        )
    }
}

@Composable
private fun AssignmentDialog(
    students: List<Student>, initialStudent: Student, initial: SituationAssignment?,
    onDismiss: () -> Unit, onConfirm: (Student, AttendanceStatus, String) -> Unit,
) {
    var student by remember { mutableStateOf(initialStudent) }
    var status by remember { mutableStateOf(initial?.status ?: AttendanceStatus.LEAVE) }
    var reason by remember { mutableStateOf(initial?.reason.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("学生情况") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("学生", style = MaterialTheme.typography.labelLarge) }
            if (initial != null) item { Text(initialStudent.name, modifier = Modifier.padding(vertical = 8.dp)) }
            else items(students, key = { it.id }) { option ->
                Row(Modifier.fillMaxWidth().clickable { student = option }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(student.id == option.id, onClick = { student = option }); Text(option.name)
                }
            }
            item { Text("状态", style = MaterialTheme.typography.labelLarge) }
            items(AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }) { option ->
                Row(Modifier.fillMaxWidth().clickable { status = option }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(status == option, onClick = { status = option }); Text(option.label)
                }
            }
            item { OutlinedTextField(reason, { reason = it.take(240).filterNot(::unsafeTextCharacter) }, label = { Text("原因（可选）") }, singleLine = true) }
        }
    }, confirmButton = { TextButton(onClick = { onConfirm(student, status, reason) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun SituationNameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial.isEmpty()) "添加情况" else "修改名称") },
        text = { OutlinedTextField(value, { value = it.take(120).filterNot(::unsafeTextCharacter) }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun unsafeTextCharacter(character: Char): Boolean =
    character.isISOControl() || character == '\u2028' || character == '\u2029'
