# EdgeGesture 现代重构版开发交接文档

## 1. 项目基础信息与开发环境

- **项目根目录**：`d:\Users\0\Downloads\project\EdgeGesture`
- **应用包名**：`com.omarea.gesture.modern`（主包名 `com.omarea.gesture`）
- **开发语言与技术栈**：Kotlin + Jetpack Compose (Material 3) + Flow / Jetpack DataStore + 原生 Android Canvas / Accessibility Service。
- **当前连接调试真机**：
  - 设备型号：`OnePlus PLF110` (一加 Ace 2 Pro，Android 16 / SDK 36，ColorOS)
  - 设备序列号：`3B6F59EAZ2KDVN9C`
- **开发工具路径**：
  - JDK：`D:\Program Files\Android\Android Studio1\jbr`
  - ADB：`D:\Program Files\platform-tools\adb.exe`
- **编译与安装命令**：
  - 编译 Release 包：`./gradlew assembleRelease`
  - 安装并覆盖：`& "D:\Program Files\platform-tools\adb.exe" -s 3B6F59EAZ2KDVN9C install -r "d:\Users\0\Downloads\project\EdgeGesture\app\build\outputs\apk\release\app-release.apk"`

---

## 2. 核心设计原则与用户红线约束

1. **界面结构**：保持原版的 **4 个 Tab**（基础设置、两侧边缘手势、底部小白条、其他设置）。
2. **三段式手势**：**坚决不做三段式手势**（用户明确要求去掉，严禁添加）。
3. **真机调试安全**：用户手机自带系统小白条，ADB 模拟操作或调试时**绝对不要碰手机系统本身的导航条/小白条**，只操作应用界面和项目开发的悬浮控件。
4. **代码提交**：所有修改在本地验证，**严禁 push 到 GitHub**。

---

## 3. 已完成工作（Prior Completed Work）

1. **色号字体带颜色**：
   - 创建了 [`ColorText.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/components/ColorText.kt)（组件 `ColorHexText`）。
   - 色号文本（如 `#FFFFFF`）字体本身直接渲染为对应颜色，外加半透明自适应对比度背景胶囊。
2. **调色盘与色号绑定**：
   - [`ColorPickerDialog.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/components/ColorPickerDialog.kt) 支持调色盘、HEX 文本框输入以及 RGB/透明度滑块双向实时联动。
3. **返回桌面热区高亮残留修复**：
   - 修复了从设置页切回桌面时物理屏幕边缘热区辅助线未消失的问题（`SettingsActivity.kt` 退出与暂停时触发 `refreshTestMode` 清除）。
4. **真机基础验证**：
   - 验证了侧滑返回、小白条上滑回桌面、电量广播监听等核心链路的可用性。

---

## 4. 当前待办任务清单与具体实现方案（Pending Tasks & Implementation）

新窗口打开后，请直接按照以下 5 项任务开始执行修改与调试：

### 任务 1：双击逻辑彻底删除（零延迟单击）
- **背景**：当前小白条为了支持双击，在单击抬起时加入了 280ms 延迟等待，导致单击操作迟钝。
- **修改文件**：
  - [`ModernWhiteBar.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBar.kt)：
    - 删除 `lastClickTime`、`pendingClickRunnable` 及 280ms 延时调度逻辑。
    - `ACTION_UP` 判定为点击时，立即执行 `hapticsManager.performHapticFeedback` 并触发 `clickAction`，达到 0ms 瞬时响应。
  - [`WhiteBarScreen.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/screens/WhiteBarScreen.kt)：
    - 从手势动作列表和弹窗中彻底删除“双击”这一项。
  - [`AppConfigRepository.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/core/config/AppConfigRepository.kt)：
    - 清理或废弃 `doubleClickAction`。

### 任务 2：震动彻底修复（适配 Android 16 / ColorOS）
- **原因诊断**：
  1. OnePlus PLF110 (Android 16 / ColorOS) 严格拦截后台无属性震动。在 API 33+ 上调用 `vibrator.vibrate(effect)` 如果不传递 `VibrationAttributes.USAGE_TOUCH`，会被系统触控过滤器丢弃。
  2. `view.performHapticFeedback` 未带 `FLAG_IGNORE_VIEW_SETTING`，悬浮窗 View 默认不聚焦导致无法震动。
  3. `HapticsManager.kt` 没有注入 `AppConfigRepository`，完全忽略了用户在 `BasicSettingsScreen` 中设置的“系统默认震感”开关以及自定义轻触时长（`vibratorTapTimeMs`）和悬停时长（`vibratorHoverTimeMs`）。
- **修改文件**：
  - [`HapticsManager.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/core/haptics/HapticsManager.kt)：
    - 构造器改为 `HapticsManager(private val context: Context, private val configRepository: AppConfigRepository)`。
    - 在 `performHapticFeedback` 中传入 `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING`。
    - 确保相关 View（`ModernWhiteBarView`、左右侧滑栏 View）设置 `isHapticFeedbackEnabled = true`。
    - 在 `vibrate()` 中判断 `configRepository.basicConfig.value.vibratorUseSystem`：
      - **系统震感**：API 33+ 使用 `VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build()` 配合 `createPredefined` / `Composition`。
      - **自定义震感**：调用 `vibrator.cancel()`，根据动作类型读取 `vibratorTapTimeMs` 或 `vibratorHoverTimeMs`，执行 `vibrator.vibrate(VibrationEffect.createOneShot(duration, 255), attributes)`。
  - [`AccessibilityServiceGesture.java`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/AccessibilityServiceGesture.java)：
    - 实例化改为 `new HapticsManager(this, repo)`。

### 任务 3：左右边缘手势热区改造（对齐原版）
- **需求规格**：
  1. **高度从下至上**：严格对齐原版逻辑，从屏幕底部（0%）向上延伸至顶部（100%）。
     - 左侧条：`params.gravity = Gravity.START | Gravity.BOTTOM`，`params.x = 0`，`params.y = 0`。
     - 右侧条：`params.gravity = Gravity.END | Gravity.BOTTOM`，`params.x = 0`，`params.y = 0`。
     - 高度公式：`heightPx = (screenHeight * heightPercent).toInt()`，比例范围 `0.0f .. 1.0f`。
  2. **宽度独立可调**：在左侧卡片和右侧卡片内分别添加“热区宽度”调节滑块（范围 6dp ~ 40dp）。
  3. **删除基本外观**：彻底删除 [`SideGestureScreen.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/screens/SideGestureScreen.kt) 最下方的“基本外观”卡片。
- **修改文件**：
  - [`ModernSideGestureBar.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/gesture/ModernSideGestureBar.kt)
  - [`SideGestureScreen.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/screens/SideGestureScreen.kt)

### 任务 4：动画模式彻底删除 & 小白条系统同款跟手物理动效
- **需求规格**：
  1. **动画模式彻底删除**：
     - 用户指令：“动画模式说明，不用补充了，整个动画模式删了，使用系统小白条的动画行不行？”
     - 从 [`OtherSettingsScreen.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/screens/OtherSettingsScreen.kt) 中彻底移除“动画模式”卡片（移除返回桌面动画、应用切换动画单选及相关说明）。
     - 从 [`AppConfigRepository.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/core/config/AppConfigRepository.kt) 中移除 `backHomeAnimation` 和 `appSwitchAnimation`。
     - 所有返回桌面和多任务动作直接调用系统底层全局动作（`performGlobalAction(GLOBAL_ACTION_HOME)` / `performGlobalAction(GLOBAL_ACTION_RECENTS)`），由系统（ColorOS / Android 16）走原生桌面转场，杜绝原版透明 Activity 伪动画带来的闪烁卡顿与权限问题。
  2. **小白条系统同款跟手物理动效**：
     - 在 [`ModernWhiteBar.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBar.kt) 与 [`ModernWhiteBarView.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBarView.kt) 中实现：
       - `ACTION_DOWN`：小白条平滑微缩（0.9x）；
       - `ACTION_MOVE`：实时计算手指位移 `dx`, `dy`，加阻尼衰减后通过 `translationX` 和 `translationY` 驱动小白条跟手物理移动；
       - `ACTION_UP` / `ACTION_CANCEL`：松手或触发时，利用 `OvershootInterpolator` / 弹性弹簧曲线平滑回弹至居中原位。

### 任务 5：现代平滑动态渐变（WhiteBar 电量指示）
- **需求规格**：
  1. 在 [`AppConfigRepository.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/core/config/AppConfigRepository.kt) 的 `WhiteBarConfig` 中增加 `batterySmoothGradient: Boolean = true` 及持久化方法。
  2. 在 [`ModernWhiteBarView.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBarView.kt) 中：
     - 当开启平滑动态渐变时，使用 `LinearGradient` 沿当前电量进度条执行多阶颜色平滑过渡（科技蓝 -> 湖青 -> 翠绿 -> 柠檬黄绿 -> 暖橙 -> 警示赤红）；
     - 当关闭时，保持原版 7 档色阶。
  3. 在 [`WhiteBarScreen.kt`](file:///d:/Users/0/Downloads/project/EdgeGesture/app/src/main/java/com/omarea/gesture/ui/settings/screens/WhiteBarScreen.kt) 中提供“现代平滑动态渐变”与“原版 7 档阶梯色阶”的切换选项。

---

## 5. 关键文件路径一览

| 模块 | 关键文件路径 |
| :--- | :--- |
| **全局配置中心** | `app/src/main/java/com/omarea/gesture/core/config/AppConfigRepository.kt` |
| **触感震动管理** | `app/src/main/java/com/omarea/gesture/core/haptics/HapticsManager.kt` |
| **小白条控制器** | `app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBar.kt` |
| **小白条绘制视图** | `app/src/main/java/com/omarea/gesture/ui/whitebar/ModernWhiteBarView.kt` |
| **侧边手势控制器** | `app/src/main/java/com/omarea/gesture/ui/gesture/ModernSideGestureBar.kt` |
| **底部小白条界面** | `app/src/main/java/com/omarea/gesture/ui/settings/screens/WhiteBarScreen.kt` |
| **侧边手势界面** | `app/src/main/java/com/omarea/gesture/ui/settings/screens/SideGestureScreen.kt` |
| **其他设置界面** | `app/src/main/java/com/omarea/gesture/ui/settings/screens/OtherSettingsScreen.kt` |
| **基础设置界面** | `app/src/main/java/com/omarea/gesture/ui/settings/screens/BasicSettingsScreen.kt` |
| **服务核心入口** | `app/src/main/java/com/omarea/gesture/AccessibilityServiceGesture.java` |
| **现代服务声明** | `app/src/main/java/com/omarea/gesture/ModernGestureService.kt` |
