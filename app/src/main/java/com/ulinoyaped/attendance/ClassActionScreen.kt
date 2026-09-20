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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.ClassGroup
import com.ulinoyaped.attendance.data.ProfileIconOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassActionScreen(
    group: ClassGroup?,
    profileIcon: ProfileIconOption,
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
                title = { Text("选择工作") },
                actions = { ProfileAvatarButton(profileIcon, onChooseClass) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(group?.name ?: "尚未选择班级", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (group == null) "点击右上角头像创建或选择班级。"
                    else "${group.students.size} 名学生 · 点击右上角头像切换班级",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp, bottom = 8.dp),
                )
            }
            if (group != null) item {
                ClassActionItem(
                    title = "开始点名",
                    subtitle = "创建一次新的点名记录",
                    icon = Icons.Default.CheckCircle,
                    iconColor = MaterialTheme.colorScheme.primary,
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
                    iconColor = MaterialTheme.colorScheme.secondary,
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
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = iconColor.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(15.dp),
                color = iconColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ProfileAvatarButton(icon: ProfileIconOption, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    profileImageVector(icon),
                    contentDescription = "切换班级",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
