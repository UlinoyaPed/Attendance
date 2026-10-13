package com.ulinoyaped.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.*
import com.ulinoyaped.attendance.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h780dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualInteractionTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    // Dialogs have their own Android Compose view and must also inherit the real font configuration.
    private fun useLargeFont() {
        for (context in listOf(ui.activity, ui.activity.applicationContext)) {
            val resources = context.resources
            val configuration = android.content.res.Configuration(resources.configuration)
            configuration.fontScale = 1.5f
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }

    @Composable private fun TestTheme(dark: Boolean = false, large: Boolean = false, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalAttendanceLayout provides AttendanceLayout(dark),
            LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
            MaterialTheme(colorScheme = attendanceColorScheme(ThemeSource.ATTENDANCE, "#5559C9", dark),
                typography = AttendanceTypography, shapes = attendanceShapes(false)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    private fun capture(name: String) {
        val file = File("build/reports/ui/$name.png")
        file.parentFile?.mkdirs()
        ui.runOnIdle {
            val view = ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView
                ?: ui.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun homeHasAWorkingPrimaryActionAndRendersAtLargeFont() {
        useLargeFont()
        var starts = 0
        ui.setContent { TestTheme(large = true) {
            ClassActionScreen(ClassGroup("class", "2026 秋 · 综合实践一班", listOf(Student("1", "张三", "2026001"))),
                ProfileIconOption.PERSON, AppSettings(), {}, { starts++ }, { _, _ -> }, {}, {})
        } }
        capture("home-light-320-large")
        ui.onNodeWithText("开始点名").performClick()
        assertEquals(1, starts)
        ui.onNodeWithText("管理班级").performScrollTo().assertIsDisplayed()
    }

    @Test fun darkClassSettingsRestoreDoesNotToggleTheSwitch() {
        useLargeFont()
        var toggles = 0
        var reset: String? = null
        ui.setContent { TestTheme(dark = true, large = true) {
            CompositionLocalProvider(LocalClassSettingsScope provides ClassSettingsScope("一班", setOf("showStudentNumbers"), { reset = it }, {}, {})) {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                    FeatureHeader("本班设置", "保留班级设置的覆写与恢复入口", Icons.Default.Palette)
                    }
                    item {
                    SettingsGroup("点名列表") {
                        SwitchSettingRow("显示学生学号", true) { toggles++ }
                        SettingRow("点按默认选择", "到场", {})
                        SettingRow("默认未到原因", "比赛外出，预计本周五下午返校", {})
                    }
                    }
                }
            }
        } }
        capture("settings-dark-320-large")
        ui.onNodeWithContentDescription("恢复显示学生学号 为全局设置").performClick()
        assertEquals("showStudentNumbers", reset)
        assertEquals(0, toggles)
        ui.onNodeWithText("显示学生学号").performClick()
        assertEquals(1, toggles)
    }

    @Test fun defaultHomeHasALightAndDarkIdentity() {
        var dark by mutableStateOf(false)
        ui.setContent { TestTheme(dark = dark) {
            ClassActionScreen(ClassGroup("class", "2026 秋 · 一班", listOf(Student("1", "张三", "2026001"))),
                ProfileIconOption.PERSON, AppSettings(), {}, {}, { _, _ -> }, {}, {})
        } }
        capture("home-light-320")
        ui.runOnIdle { dark = true }
        capture("home-dark-320")
        ui.onNodeWithText("管理班级").performScrollTo().assertIsDisplayed()
    }

    @Test fun materialAvatarStillProtectsRejectedItemsInTheNewDarkLayout() {
        val task = MaterialTask("task", "秋季活动材料收集", 0, 0,
            materials = listOf(MaterialItem("a", "活动登记表"), MaterialItem("b", "家长知情同意书")),
            participants = listOf(Student("1", "张三", "2026001")),
            records = listOf(MaterialRecord("1", "a", MaterialRecordStatus.REJECTED)))
        var confirm: Pair<String, Boolean>? = null
        ui.setContent { TestTheme(dark = true) {
            MaterialTaskScreen(task, AppSettings(), {}, { _, _, _ -> }, { id, overwrite -> confirm = id to overwrite },
                { task.records }, { _, _ -> }, {}, {})
        } }
        capture("materials-dark-320")
        ui.onNodeWithContentDescription("确认张三的未登记材料").performClick()
        ui.onNodeWithText("只确认 1 份未登记").performClick()
        assertEquals("1" to false, confirm)
    }

    @Test fun rollCallStatusRemainsExplicitWhenColorsAndIconsAreCustomized() {
        useLargeFont()
        val settings = AppSettings(presentColor = StatusColorOption.PRIMARY, absentColor = StatusColorOption.PRIMARY)
        var mark by mutableStateOf(AttendanceMark(AttendanceStatus.ABSENT, "活动外出，预计周五返校"))
        ui.setContent { TestTheme(large = true) {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { RollCallSearchTools("", {}, RollCallFilter.EXCEPTIONS, {}) }
                item { RollCallItem(Student("1", "阿依古丽·买买提", "202610080001"), mark,
                    StatusIconOption.PERSON, StatusColorOption.PRIMARY,
                    true, true, false, false, { mark = AttendanceMark(AttendanceStatus.PRESENT) }, {}, {}, {}, settings, {}) }
            }
        } }
        ui.onNodeWithText("缺勤").assertIsDisplayed()
        ui.onNodeWithText("异常").assertIsDisplayed().assertIsSelected()
        capture("roll-call-light-320-large")
        ui.onNodeWithText("阿依古丽·买买提").performClick()
        ui.onNodeWithText("到").assertIsDisplayed()
    }

    @Test fun longReasonPickerStartsAtCurrentValueAndCanReachTheLastValue() {
        useLargeFont()
        val reasons = (1..100).map { "原因$it" }
        var choice = "原因95"
        var open by mutableStateOf(true)
        ui.setContent { TestTheme(large = true) {
            if (open) PreferenceChoiceSheet("默认未到原因", reasons, choice, { it }, { open = false }, { choice = it; open = false })
        } }
        ui.onNodeWithText("原因95").assertIsDisplayed()
        assertTrue("The dialog uses large fonts too", ui.onNodeWithText("原因95").fetchSemanticsNode().boundsInRoot.height >= 30f)
        capture("reasons-sheet-320-large")
        ui.onNodeWithText("原因100").performScrollTo().performClick()
        assertEquals("原因100", choice)
    }

    @Test fun themeCardsAllowCustomColorsWithoutDiscardingTheStoredSeed() {
        var settings by mutableStateOf(AppSettings(themeSeed = "#B83384"))
        ui.setContent { TestTheme {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(16.dp)) {
                item { ThemeSettingsCard(settings) { source, seed -> settings = settings.copy(themeSource = source, themeSeed = seed) } }
            }
        } }
        capture("theme-styles-light-320")
        ui.onNodeWithText("自定义").performClick()
        assertEquals(ThemeSource.CUSTOM, settings.themeSource)
        assertEquals("#B83384", settings.themeSeed)
        ui.onNodeWithText("主颜色 #RRGGBB").performScrollTo().assertIsDisplayed()
    }
}
