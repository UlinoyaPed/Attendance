package com.ulinoyaped.attendance

import com.ulinoyaped.attendance.ui.theme.LocalAttendanceLayout
import com.ulinoyaped.attendance.ui.theme.statusSurface
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.ProfileIconOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassActionScreen(
    group: ClassGroup?,
    profileIcon: ProfileIconOption,
    settings: AppSettings,
    onChooseClass: () -> Unit,
    onStartAttendance: () -> Unit,
    onStartMaterials: (String, List<String>) -> Unit,
    onManageClass: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var showMaterialDialog by remember(group?.id) { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班级事务") },
                actions = { ProfileAvatarButton(profileIcon, onChooseClass) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(LocalAttendanceLayout.current.groupGap),
        ) {
            item {
                FeatureHeader(
                    title = group?.name ?: "选择一个班级",
                    subtitle = if (group == null) "点击右上角头像创建或选择班级" else listOfNotNull(
                        "${group.students.size} 名学生".takeIf { settings.showClassStudentCount },
                        "右上角切换班级".takeIf { settings.showClassOperationHint },
                    ).joinToString(" · "), icon = Icons.Default.Groups,
                )
            }
            if (group != null) item {
                ClassActionItem(
                    title = "开始点名",
                    subtitle = "创建一次新的点名记录",
                    icon = Icons.Default.CheckCircle,
                    iconColor = MaterialTheme.colorScheme.primary,
                    prominent = true,
                    onClick = onStartAttendance,
                )
            }
            if (group != null) item {
                ClassActionItem(
                    title = "材料登记",
                    subtitle = "自定义本次名称和一份或多份材料",
                    icon = Icons.Default.Inventory2,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { showMaterialDialog = true },
                )
            }
            if (group != null) item {
                Text("班级", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                ClassActionItem(
                    title = "管理班级",
                    subtitle = "编辑班名、学生名单、情况和班级设置",
                    icon = Icons.Default.Settings,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onManageClass,
                )
            }
        }
    }
    if (showMaterialDialog && group != null) {
        MaterialTaskCreateDialog(
            onDismiss = { showMaterialDialog = false },
            onConfirm = { title, names ->
                showMaterialDialog = false
                onStartMaterials(title, names)
            },
        )
    }
}

@Composable
private fun ClassActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    prominent: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (prominent) scheme.primary else scheme.surface,
        contentColor = if (prominent) scheme.onPrimary else scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().then(if (prominent) Modifier.background(Brush.linearGradient(listOf(scheme.primary, lerp(scheme.primary, scheme.secondary, 0.20f)))) else Modifier).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (prominent) scheme.onPrimary.copy(alpha = 0.14f) else statusSurface(iconColor),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (prominent) scheme.onPrimary else iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (prominent) scheme.onPrimary else scheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(22.dp), tint = if (prominent) scheme.onPrimary else scheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProfileAvatarButton(icon: ProfileIconOption, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = statusSurface(MaterialTheme.colorScheme.primary)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    profileImageVector(icon),
                    contentDescription = "切换班级",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

internal fun profileImageVector(option: ProfileIconOption): ImageVector = when (option) {
    ProfileIconOption.PERSON -> Icons.Default.Person
    ProfileIconOption.SCHOOL -> Icons.Default.School
    ProfileIconOption.GROUPS -> Icons.Default.Groups
    ProfileIconOption.STAR -> Icons.Default.Star
}
