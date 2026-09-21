# EdgeGesture 渐进式重构工作表 (Worksheet & Log)

> 本工作表用于实时记录 EdgeGesture 项目从旧 Java 架构向现代 Android (Kotlin + Compose + Shizuku) 渐进式重构的每一步进展与状态。每次完成相应阶段或任务后均在此更新。

---

## 一、重构总体状态看板 (Dashboard)

- **当前阶段**：第 10 阶段进行中（Phase 8 & Phase 9 已达成，Phase 10 待办任务就绪）
- **项目状态**：🔄 原版核心功能完整复刻完成，真机调试推进中，正进入细节体验与手感极致调优
- **核心成果**：
  1. 保留并升级无障碍服务（AccessibilityService）作为核心手势基础，服务入口重命名为 `ModernGestureService` 解决与旧版混淆
  2. 重构 iOS 风格小白条（WhiteBar）核心功能：单击、长按打开应用及线性马达振动反馈
  3. 引入官方 Shizuku 替换过时的 `adb_process` 本地 HTTP 守护进程
  4. 采用 Kotlin + Jetpack Compose + Material 3 + DataStore 重建现代架构与设置界面（保持原版 4 Tab 布局，彻底移除三段式手势）
  5. 解决 Android 11+ 包可见性（`QUERY_ALL_PACKAGES`），应用选择器支持完整加载与无障碍免授权启动
  6. 实现带字体颜色的色号展示（`ColorHexText`）与 RGB/HEX 双向联动调色盘
  7. 修复退出设置与切回桌面时的热区预览残留问题
  8. 适配 Android 16 (API 36) 与系统手势排除区域（防侧滑冲突）
  9. 生成全新独立 Release APK (`app-release.apk`，包名 `com.omarea.gesture.modern`)

---

## 二、详细任务清单与进度跟踪 (Task Checklist)

### 阶段 1：构建系统与依赖现代化 (Build & Infrastructure)
- [x] **Task 1.1**: 升级 Gradle Wrapper 与 Android Gradle Plugin (AGP)，移除失效的 `jcenter()`
- [x] **Task 1.2**: 配置 Kotlin 编译环境与现代 Android SDK (compileSdk 36, targetSdk 36, minSdk 26)
- [x] **Task 1.3**: 引入核心库依赖：Coroutines、Jetpack DataStore、Material 3、Jetpack Compose、Shizuku API
- [x] **Task 1.4**: 验证基础工程能够成功编译

### 阶段 2：基础核心层与架构解耦 (Core Architecture & Dispatcher)
- [x] **Task 2.1**: 实现 `core/haptics/HapticsManager.kt`（适配 Android 10+ 线性马达复合触感反馈）
- [x] **Task 2.2**: 实现 `core/model/Action.kt`（统一手势与动作模型）
- [x] **Task 2.3**: 实现 `core/config/AppConfigRepository.kt`（基于 DataStore 的响应式配置）
- [x] **Task 2.4**: 实现 `core/dispatcher/ActionDispatcher.kt`（动作分发中枢：无障碍动作 + 应用启动）

### 阶段 3：小白条与触控引擎现代化 (WhiteBar & Touch Engine)
> **优先满足核心诉求**：小白条单击、长按打开指定应用及触感反馈
- [x] **Task 3.1**: 用 Kotlin 重写小白条视图 `ModernWhiteBarView`，支持物理弹簧缩放动画
- [x] **Task 3.2**: 实现低延迟手势识别，触发瞬间配合线性马达触感
- [x] **Task 3.3**: 实现应用选择与启动通道（支持直接启动与无障碍辅助启动）
- [x] **Task 3.4**: 挂载至 `AccessibilityServiceGesture`，完成小白条独立验证

### 阶段 4：Shizuku 特权模块集成 (Shizuku Integration)
- [x] **Task 4.1**: 封装 `core/shizuku/ShizukuManager.kt`（权限检查、授权监听、Binder 调用）
- [x] **Task 4.2**: 接入 Shizuku 特权动作通道（瞬时启动应用绕过 5s 延迟、执行特权 Shell）
- [x] **Task 4.3**: 彻底移除旧版 `adb_process` 目录及本地 HTTP 服务 (`RemoteAPI.java`)

### 阶段 5：两侧边缘手势与防冲突适配 (Side & Bottom Gestures)
- [x] **Task 5.1**: 现代化重构两侧边缘手势触控条（`ModernSideGestureBar.kt`）
- [x] **Task 5.2**: 适配 Android 10+ `setSystemGestureExclusionRects()` 系统手势防冲突
- [x] **Task 5.3**: 适配现代 `WindowMetrics`，精准响应横竖屏旋转与窗口尺寸变更

### 阶段 6：Compose 设置界面与可视化调节 (Modern Settings UI)
- [x] **Task 6.1**: 搭建 Compose + Material 3 主设置界面框架（支持 Monet 动态主题色）
- [x] **Task 6.2**: 小白条配置页（开关、尺寸、颜色、单击/长按动作绑定、触感调节）
- [x] **Task 6.3**: 现代应用选择器弹窗（带应用图标、实时搜索）
- [x] **Task 6.4**: Shizuku 状态卡片与一键引导授权组件
- [x] **Task 6.5**: 边缘手势可视化调节面板

### 阶段 7：代码清理、独立签名与双版本构建 (Cleanup & Signing)
- [x] **Task 7.1**: 清理遗留旧版 XML Fragments (`FragmentBasic`, `FragmentWhiteBar` 等)
- [x] **Task 7.2**: 修改 applicationId 为 `com.omarea.gesture.modern`，支持与原版共存
- [x] **Task 7.3**: 配置专属签名密钥 `app/keystore/release.jks`，自动生成签名包

### 阶段 8：原版功能深度复刻与架构对齐 (Original Feature Parity)
- [x] **Task 8.1**: 重构主界面为标准 4 Tab 布局（基础设置、两侧边缘手势、底部小白条、其他设置），彻底移除三段式手势
- [x] **Task 8.2**: 实现基础设置中的悬停时长精确调节（100ms~600ms）、系统/自定义震感及时长调节
- [x] **Task 8.3**: 复刻原版 7 档电量指示色阶（蓝、青蓝、湖青、翠绿、柠檬黄绿、暖橙、警示赤红）
- [x] **Task 8.4**: 复刻其他设置：低功耗模式、游戏防误触、隐藏桌面图标、前台窗口监视、应用切换黑名单
- [x] **Task 8.5**: 重构无障碍服务入口为 `ModernGestureService`，彻底杜绝与旧版手势无障碍状态混淆

### 阶段 9：体验打磨与真机实测修复 (UX Refinement & Device Testing)
- [x] **Task 9.1**: 声明 `QUERY_ALL_PACKAGES` 权限，彻底修复 Android 11+ 应用选择器只能看到少量应用的问题
- [x] **Task 9.2**: 修复启动应用无需额外悬浮窗/后台授权，通过无障碍辅助直接调起
- [x] **Task 9.3**: 实现色号文本字体带颜色（`ColorHexText`，如 `#ffffff` 为白字，带自适应高对比底色），调色盘支持 HEX 实时编辑与 RGB/Alpha 双向联动
- [x] **Task 9.4**: 修复退出设置页或返回桌面时，屏幕物理边缘热区高亮辅助线残留的问题
- [x] **Task 9.5**: 在真实设备（OnePlus PLF110 / Android 16）通过 ADB 联调验证核心功能

### 阶段 10：手感调优与真机细节极致修复 (Parity & Real-device Polish)
- [x] **Task 10.1**: 彻底删除小白条双击逻辑与 280ms 判定延迟，实现单击 0ms 瞬时响应；界面彻底移除双击选项与仓库配置键
- [x] **Task 10.2**: 彻底修复震动（深度对齐原版 `Gesture.vibrate` 实现：系统震感采用 `VIRTUAL_KEY`/`LONG_PRESS`/`CLOCK_TICK` 穿透 ColorOS 拦截；自定义震感直接调用 `Vibrator` 毫秒级强力振动，彻底废除 `USAGE_TOUCH` 导致的无焦点丢弃）
- [x] **Task 10.3**: 左右边缘手势热区改造为原版从下 0% 到上 100%（`Gravity.BOTTOM`），高度严格按 `screenHeight * percent`，左右卡片增加独立宽度调节滑块（6dp~40dp），删除卡片 4（基本外观）
- [x] **Task 10.4**: 彻底删除“动画模式”（移除伪动画，走系统原生转场）；小白条通过 WindowManager 坐标联动实现系统同款 1:1 跟手物理阻尼位移与微缩放（0.90x），松手弹性回弹（OvershootInterpolator）
- [x] **Task 10.5**: 小白条电量指示实现“现代平滑动态渐变”（绿-黄-橙-红连续过渡）与“原版 7 档色阶”自由切换，设置界面增加动态切换开关
- [x] **Task 10.6**: 彻底修复小白条触控热区“卡在 45 只高不低”的问题（移除多余的 30dp `verticalPadding` 与 14dp 强制下限，热区窗口高度与显示 1:1 严格等于设置值，范围拓展为 8dp~80dp）
- [x] **Task 10.7**: 新增小白条防烧屏微位移（Burn-in Protection / Pixel Shift）：每 60 秒平滑微调小白条像素位置，防止 OLED 屏幕长时间显示造成烧屏，设置中支持开关
- [x] **Task 10.8**: 小白条触控热区宽度独立可调（60dp ~ 400dp），彻底解耦小白条视觉宽度与手势感应宽度
- [x] **Task 10.10**: 彻底解决小白条触控热区宽度“卡住 126dp”问题（根因定位为 `ModernWhiteBarView.onMeasure` 强制使用 `barWidthPx` 覆盖，修改为直接读取 `MeasureSpec` 测得的 `touchWidthPx`，实现 60dp~400dp 任意平滑调节）
- [x] **Task 10.11**: 小白条透明度支持设定为 0%（`ColorPickerDialog` 下限调整为 0f，界面新增快速不透明度调节滑块 0%~100%；透明度为 0 时彻底隐藏小白条本体及描边，只保留触控热区手势感应）
- [x] **Task 10.12**: 全面清理精简应用内冗余说明文本（移除小白条/边缘手势各界面的大段冗长说明横幅、滑块后置说明、7档色阶冗余明细等，恢复极简清爽 UI）

- [x] **Task 10.13**: 自适应应用图标体系重绘（参考 `spotoolfy_flutter` 与现代 Material You 规范，构建矢量前景手势剪影 `ic_launcher_foreground.xml`、柔和青色渐变微发光背景 `ic_launcher_background.xml`、Android 13+ 动态主题单色图标 `ic_launcher_monochrome.xml`，以及 `mipmap-anydpi-v26` 自适应与圆形图标声明）
- [x] **Task 10.14**: UI 全面升级为 Material Design 3 Expressive (MD3E) 标准（24dp 大圆角卡片 `shapes.large`、`surfaceContainer` 柔和对比色阶、顶栏动态服务状态胶囊药丸 `● 服务已运行` / `▲ 服务未启用`、4 枚手势专属矢量底部导航图标、带对勾图标的签名级 `Switch` 开关）
- [x] **Task 10.15**: 全面清理项目本体无用原型与死代码（彻底删除 `SideGestureBar.java`、`TouchBarView.java`、`iOSWhiteBar.java` 等 6 个旧版 Java View，以及 `gesture_settings_*.xml` 等 9 个旧版 XML 布局；清理 `AccessibilityServiceGesture` 与 `GlobalState` 死代码引用）

---

## 三、工作日志记录 (Changelog & Milestones)

| 日期与时间 | 执行任务 | 变更说明与完成内容 | 产物 / 状态 |
| :--- | :--- | :--- | :--- |
| 2026-09-20 | 阶段 1：构建系统升级 | 1. 升级 Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.3.21<br/>2. 适配 targetSdk 36 (Android 16), compileSdk 36<br/>3. 引入 Compose/Material 3/Coroutines/DataStore/Shizuku 依赖<br/>4. 修复 Manifest exported 属性，完成首次成功编译 | `assembleDebug` 编译成功通过 (BUILD SUCCESSFUL) |
| 2026-09-20 | 阶段 2：基础核心架构 | 1. 实现 `HapticsManager` 现代高品质线性马达触觉反馈<br/>2. 实现 `Action` 手势动作模型与编解码器<br/>3. 实现 `AppConfigRepository` 基于 DataStore 的响应式配置<br/>4. 实现 `ActionDispatcher` 统一动作分发器 | 阶段 2 代码编译通过 (BUILD SUCCESSFUL) |
| 2026-09-20 | 阶段 3：小白条与手势核心 | 1. 实现 `ModernWhiteBarView` 原生高效 Canvas 绘制与物理缩放<br/>2. 实现 `ModernWhiteBar` 低延迟手势检测（单击/长按打开指定应用、滑动切应用）<br/>3. 挂载至 `AccessibilityServiceGesture`，无缝替换旧版悬浮视图 | 阶段 3 代码编译通过 (BUILD SUCCESSFUL) |
| 2026-09-21 | 阶段 4：Shizuku 模块集成 | 1. 封装 `ShizukuManager`，支持 Binder 监听、权限申请与特权 Shell 执行<br/>2. `ActionDispatcher` 接入特权启动与 Shell 调度，实现安全降级<br/>3. 彻底移除旧版 `adb_process` 目录及本地 HTTP 服务，`AdbProcessExtractor` 与 `RemoteAPI` 迁移至安全桩代码 | 阶段 4 代码编译通过 (BUILD SUCCESSFUL) |
| 2026-09-21 | 阶段 5：两侧边缘手势适配 | 1. 实现 `ModernSideGestureBar` 现代化两侧触控条<br/>2. 适配 Android 10+ `setSystemGestureExclusionRects` 防止原生侧滑冲突<br/>3. 适配现代 `WindowMetrics`，精准响应横竖屏旋转与窗口尺寸变更 | 阶段 5 代码编译通过 (BUILD SUCCESSFUL) |
| 2026-09-21 | 阶段 6：Compose 设置界面 | 1. 采用 Material 3 + Compose 重写 `SettingsActivity`<br/>2. 实现小白条配置页（实时外观预览、尺寸滑块、手势映射）<br/>3. 实现带图标与实时搜索的应用选择弹窗 `AppPickerDialog`<br/>4. 实现两侧边缘手势配置页与 Shizuku 状态卡片 | 阶段 6 代码编译通过 (BUILD SUCCESSFUL) |
| 2026-09-21 | 阶段 7：代码清理与独立签名 | 1. 清理全部旧版 XML Fragments (`FragmentBasic`, `FragmentWhiteBar` 等)<br/>2. 修改 applicationId 为 `com.omarea.gesture.modern`，支持共存<br/>3. 创建专属签名密钥 `app/keystore/release.jks`，配置自动签名构建 | 成功生成已签名的 `app-release.apk` |
| 2026-09-21 | 阶段 8：原版功能深度复刻 | 1. 界面确立为原版 4 Tab 布局，彻底删除三段式手势<br/>2. 复刻悬停时长（100~600ms）、原版 7 档色阶电量指示、游戏防误触、低功耗模式等<br/>3. 重命名无障碍服务为 `ModernGestureService`，解决与旧版服务的状态混淆 | 功能对齐完成，代码编译通过 |
| 2026-09-21 | 阶段 9：体验优化与真机联调 | 1. 添加 `QUERY_ALL_PACKAGES` 彻底解决应用选择器仅展示部分应用的问题<br/>2. 实现色号文字字体带色（`ColorHexText`），调色盘 HEX 输入双向联动<br/>3. 修复桌面热区预览残留问题（退出设置立即清除辅助线）<br/>4. 真机 (OnePlus PLF110 / Android 16) 实测验证侧滑返回、小白条返回桌面、电量同步 | 真实设备调试通过，截图验证正常 |
| 2026-09-21 | 阶段 10：交接文档与方案确立 | 1. 编写 [HANDOVER.md](file:///d:/Users/0/Downloads/project/EdgeGesture/HANDOVER.md) 完整交接文档<br/>2. 确立 5 项具体实施方案（双击彻底删除、Android 16 震动适配、左右手势从底向上改造、删除动画模式+小白条跟手物理动效、动态平滑渐变） | 交接文档与重构跟踪表同步完毕 |
| 2026-09-21 | 阶段 10：5项任务落地与真机调优 | 1. 彻底移除双击判定延迟（0ms 瞬发单击），删除设置双击项<br/>2. 左右边缘手势从下往上贴底，支持左右独立宽度，删除多余卡片<br/>3. 彻底删除动画模式，小白条实现 0.90x 微缩放与带阻尼物理跟手回弹<br/>4. 实现小白条电量指示平滑连续动态渐变<br/>5. **震动彻底修复**：查阅并严格对齐原版 `Gesture.java`，系统触感改回 `VIRTUAL_KEY`，自定义震感直接调用 `vibrator.vibrate(OneShot, 255)`，去除 `USAGE_TOUCH` 导致的无焦点被杀问题<br/>6. **热区高度修复**：彻底移除锁死 45dp 的多余 padding 与强制下限，热区窗口与画面文字 1:1 响应用户滑块（支持 8dp~80dp） | 代码全部完成，Release 重新构建与真机部署通过 |
| 2026-09-21 | 阶段 10：防烧屏、热区宽度与震动终极修复 | 1. **防烧屏微位移**：实现每 60 秒平滑微偏移小白条像素位置，防止 OLED 烧屏，设置界面提供开关<br/>2. **热区宽度独立可调**：新增 `touchWidthDp`（60dp~400dp），解耦视觉宽度与触控感应范围<br/>3. **震动双保险终极修复**：针对设备关闭全局触感（`haptic_feedback_enabled = 0`）环境，直接调用 `Vibrator.createOneShot(ms, 255)` 硬件脉冲，确保 100% 触发马达振动 | 全部完成，真机调试验证通过：<br/>• `vibrator_manager` 实测 100% 触发 `Step=15ms(amplitude=1.00)` 硬件级触感反馈<br/>• 触控热区宽度（200dp）实测生效（热区内触发振动与动作，热区外不截获点击）<br/>• 防烧屏开关与平滑微位移动画在 Compose 界面与 WindowManager 中正常生效 |
| 2026-09-21 | 阶段 10：热区宽度解卡、0%透明度与文字精简 | 1. **热区宽度解卡**：修复 `ModernWhiteBarView.onMeasure` 覆盖测量宽度的 bug，热区宽度现严格跟随滑块自由设定<br/>2. **透明度支持 0%**：调色盘支持 0%~100%，小白条页面增加直接滑块；透明度为 0 时本体与描边完全隐藏，只留触控热区<br/>3. **文字全面精简**：删除所有界面的冗余长篇大论说明，UI 恢复纯净极简 | 全部完成，重新构建并安装部署 |
| 2026-09-21 | 阶段 10：图标重绘、MD3E 美化与死代码清理 | 1. **应用图标重绘**：打造 Material You 自适应矢量图标体系（前景 `ic_launcher_foreground.xml`、渐变发光背景 `ic_launcher_background.xml`、单色主题图标 `ic_launcher_monochrome.xml`）<br/>2. **MD3E UI 全面美化**：引入 24dp 表现力大圆角、`surfaceContainer` 现代色阶、顶栏实时服务状态胶囊芯片、4 枚手势专属矢量底部导航图标、带勾选图标的 `Switch`<br/>3. **死代码全面清理**：删除 6 个旧版 Java 悬浮 View 与 9 个旧版 XML 布局，清理无效 import 与废弃符号 | 全部完成，`assembleRelease` 编译打包成功 (BUILD SUCCESSFUL) |




