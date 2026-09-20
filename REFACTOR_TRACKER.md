# EdgeGesture 渐进式重构工作表 (Worksheet & Log)

> 本工作表用于实时记录 EdgeGesture 项目从旧 Java 架构向现代 Android (Kotlin + Compose + Shizuku) 渐进式重构的每一步进展与状态。每次完成相应阶段或任务后均在此更新。

---

## 一、重构总体状态看板 (Dashboard)

- **当前阶段**：阶段准备 (Phase 0 -> Phase 1)
- **项目状态**：规划完成，等待开始执行
- **核心目标**：
  1. 保留并升级无障碍服务（AccessibilityService）作为核心手势基础
  2. 优先重构 iOS 风格小白条（WhiteBar）核心功能：单击、长按打开应用及线性马达振动反馈
  3. 引入官方 Shizuku 替换过时的 `adb_process` 本地 HTTP 守护进程
  4. 采用 Kotlin + Jetpack Compose + Material 3 + DataStore 重建现代架构与设置界面
  5. 渐进式演进，确保各阶段代码持续可编译、可运行

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
- [ ] **Task 3.1**: 用 Kotlin 重写小白条视图 `ModernWhiteBarView`，支持物理弹簧缩放动画
- [ ] **Task 3.2**: 实现低延迟单击与长按手势识别，触发瞬间配合线性马达触感
- [ ] **Task 3.3**: 实现应用选择与启动通道（支持直接启动与无障碍辅助启动）
- [ ] **Task 3.4**: 挂载至 `AccessibilityServiceGesture`，完成小白条独立验证

### 阶段 4：Shizuku 特权模块集成 (Shizuku Integration)
- [ ] **Task 4.1**: 封装 `core/shizuku/ShizukuManager.kt`（权限检查、授权监听、Binder 调用）
- [ ] **Task 4.2**: 接入 Shizuku 特权动作通道（瞬时启动应用绕过 5s 延迟、执行特权 Shell）
- [ ] **Task 4.3**: 废弃并清理旧版 `adb_process` 目录及本地 HTTP 服务 (`RemoteAPI.java`)

### 阶段 5：两侧边缘手势与防冲突适配 (Side & Bottom Gestures)
- [ ] **Task 5.1**: 现代化重构两侧边缘手势触控条（`SideGestureBar.kt`）
- [ ] **Task 5.2**: 现代化重构三段式手势（`ThreeSectionView.kt`）
- [ ] **Task 5.3**: 适配 Android 10+ `setSystemGestureExclusionRects()` 系统手势防冲突
- [ ] **Task 5.4**: 适配现代 `WindowMetrics`，支持横竖屏旋转与折叠屏/平板

### 阶段 6：Compose 设置界面与可视化调节 (Modern Settings UI)
- [ ] **Task 6.1**: 搭建 Compose + Material 3 主设置界面框架（支持 Monet 动态主题色）
- [ ] **Task 6.2**: 小白条配置页（开关、尺寸、颜色、单击/长按动作绑定、触感调节）
- [ ] **Task 6.3**: 现代应用选择器弹窗（带应用图标、实时搜索）
- [ ] **Task 6.4**: Shizuku 状态卡片与一键引导授权组件
- [ ] **Task 6.5**: 边缘手势可视化调节面板

### 阶段 7：代码清理与全功能验证 (Cleanup & Verification)
- [ ] **Task 7.1**: 清理遗留 Java 类、无用资源与过时权限声明
- [ ] **Task 7.2**: 全面功能回归测试与性能/内存开销优化

---

## 三、工作日志记录 (Changelog & Milestones)

| 日期与时间 | 执行任务 | 变更说明与完成内容 | 产物 / 状态 |
| :--- | :--- | :--- | :--- |
| 2026-09-20 | 阶段 1：构建系统升级 | 1. 升级 Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.3.21<br/>2. 适配 targetSdk 36 (Android 16), compileSdk 36<br/>3. 引入 Compose/Material 3/Coroutines/DataStore/Shizuku 依赖<br/>4. 修复 Manifest exported 属性，完成首次成功编译 | `assembleDebug` 编译成功通过 (BUILD SUCCESSFUL) |
| 2026-09-20 | 阶段 2：基础核心架构 | 1. 实现 `HapticsManager` 现代高品质线性马达触觉反馈<br/>2. 实现 `Action` 手势动作模型与编解码器<br/>3. 实现 `AppConfigRepository` 基于 DataStore 的响应式配置<br/>4. 实现 `ActionDispatcher` 统一动作分发器 | 阶段 2 代码编译通过 (BUILD SUCCESSFUL) |

