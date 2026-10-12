# 视觉重构针对性审查（2026-10-12）

基线：`master` 的 `3c77235390326ae9528b9cc5eabe496aa15cd422`。本轮只修复下面三类可定位的问题，不扩张业务功能或主题设置。

## 三项问题与最小修复

| 问题 | 代码依据与实际影响 | 修复位置 / 最小方案 |
| --- | --- | --- |
| 新主题枚举阻断旧版整份备份恢复 | 原 `AttendanceRepository.settingsToJson` 直接写 `ATTENDANCE/SOFT`；重构前 `BackupValidation.settings` 只认可 `SYSTEM/CUSTOM`。在 `fc2b278` 实测报 `themeSource 选项无效`，`importBackup` 返回 false；失败在提交前，原数据未覆盖。班级覆写的 values 也携带主题字段，不能只修全局设置。 | `AttendanceRepository.kt`：保留旧版认识的 `themeSource=CUSTOM` 和已有 seed，另存可选 `themePreset`；新版优先读 preset，也兼容 3c77235 的直接枚举。导出时转换已确认的旧快照及班级 values，避免“升级后未操作就导出”绕过兼容修复。`BackupValidation.kt` 验证 preset，非法元数据仍原子拒绝。 |
| 学号使状态与原因被省略 | 原 `StudentIdentityText` 细节强制 `maxLines=1`；点名拼接顺序是学号、状态、原因。320dp / 大字体时，尾部“状态”按钮与图标占据宽度，长学号可以把异常状态挤进省略号。如果用户选了相同图标或主题色，辨认尤其困难。材料进度也排在学号后。 | `SharedComponents.kt` / `AttendanceApp.kt` / `MaterialScreens.kt`：状态作为独立文字先显示，学号与原因有自己的省略预算；普通点名 / 历史原因最多两行，紧凑点名保持一行。标签为最长三字状态预留随字体缩放的宽度，减少仅切换状态产生的换行跳动。材料进度先于学号。普通、选择模式都应用。 |
| 外观控件固定横排挤压大字体布局 | `ThemeSettingsCard` 四个快捷色均为最小宽度 TextButton，原固定 Row 无法换行；`StatusAppearanceRow` 的图标名 TextButton 与颜色按钮占用尾部固定宽度，压缩状态名和班级继承信息。 | `ThemeSettingsCard.kt`：快捷色改为 FlowRow。`AttendanceApp.StatusAppearanceRow`：当前状态图标成为 48dp 编辑入口，移除宽尾部文字按钮，图标说明置于有弹性宽度的标题列；颜色入口保留，补充明确无障碍说明。 |

## 核对通过的路径

- Attendance / 柔和使用项目配色；柔和另有形状、间距和次要前景差异。系统动态仅在 API 31+ 使用系统色，旧系统回退 Attendance。自定义只消费 seed；非自定义模式保留 seed，不偷偷覆盖用户选择。
- 四条颜色路由现在由 `attendanceColorScheme` 共用，运行时、预览与单元测试使用同一函数。动态色由 Android 调用方提供，测试验证角色接入和旧系统回退，不把模拟颜色断言当成真机壁纸取色验收。
- 已保存 SYSTEM / CUSTOM 和旧备份继续读取；3c77235 已保存的 ATTENDANCE / SOFT 也继续读取。主题是全局项，班级覆写不改变全局主题。
- 状态选项未被主题切换或备份恢复重置。PURPLE、TEAL 等选择及班级逐项覆写保持；只有用户显式选择 PRIMARY 时才跟随主色。默认状态图标 / 文字使用独立深浅语义色。测试检查四条主题路由与极端自定义 seed。
- 点名、材料、历史和设置使用项目 typography、theme shapes、中性 surface；材料不合格 / 完成采用业务红 / 绿。滑动组件的 opaque tonal 背景、触发距离、长按、拖动原因排序和撤销逻辑不变。

## 备份兼容范围

导出仍基于最后成功提交的快照；不会为了兼容导出当前尚未写成功的内存状态，也不会在导出时写偏好。兼容编码只改主题元数据，班级、学生、历史快照、草稿、材料任务与覆写内容保留。

新版保存和导出新预设时，旧版可识别 `CUSTOM + themeSeed`，忽略未知的 `themePreset`。旧版只能使用保留的主色与其原有形状，不能还原新预设的完整视觉规则；旧版再保存/导出后会丢掉它不认识的 preset，这是合理的视觉降级。新版自身往返保留 preset 和用户之前选择的自定义 seed。

已在重构前 `fc2b278` 的独立 worktree 中执行实际旧 repository / validator 导入和重载，确认：直接新枚举被拒且不写入；兼容主题元数据可以恢复班级、学生、历史、草稿、材料及自定义状态 / 班级覆写。该结论针对主题变化与此版本；更早版本原有的业务字段和校验限制不会被主题元数据回退消除。

## 预览覆盖与剩余验收

`DesignPreviews.kt` 新增实际历史条目、点名草稿、结果原因、材料历史、自定义外观、班级覆写状态、API 35 动态色，以及相同自定义图标 / 颜色且隐藏状态按钮的情况。窄屏场景为 320dp / 1.5 倍字体，新增历史和自定义场景均有深色版本。

预览是布局入口，不是渲染通过的证明。现有 callback 多为 fixture 空实现，因此静态预览无法验证：左右滑动揭示与回弹、隐藏选择的批量范围、Snackbar 连续替换和撤销、不合格覆盖确认、材料完成 / 重开、原因拖动和边缘滚动。还需要真机 / 模拟器检查键盘弹出、完成材料任务与这几类交互。本环境尚无可用模拟器，未声称完成设备截图或手势验收。

## 自动构建和发布

`.github/workflows/build-apk.yml` 在 master push 和手动执行时先跑 lint、测试和 R8 Release 构建，上传 APK 及 SHA-256。只有 master 且仍为当前分支 HEAD 时发布滚动 `nightly` prerelease，避免旧排队任务回退 nightly。发布作业单独获得 contents:write；同组串行发布，失败构建不更新 tag / Release，其他正式发布不变。

沿用未签名 release APK：`Attendance-nightly-unsigned.apk`。它需要自行签名才能安装；工作流不生成临时签名密钥，不新增 secrets，也不把 nightly 标成正式 latest。
