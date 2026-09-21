# EdgeGesture (边缘手势 / 小白条手势增强)

<p align="center">
  <a href="https://github.com/Aa-creat/EdgeGesture/actions/workflows/build.yml">
    <img src="https://github.com/Aa-creat/EdgeGesture/actions/workflows/build.yml/badge.svg" alt="Build Status" />
  </a>
  <img src="https://img.shields.io/badge/Target_SDK-36_(Android_16)-brightgreen.svg" alt="Target SDK 36" />
  <img src="https://img.shields.io/badge/Min_SDK-26_(Android_8.0)-blue.svg" alt="Min SDK 26" />
  <img src="https://img.shields.io/badge/Kotlin-2.3.21-purple.svg" alt="Kotlin 2.3.21" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-orange.svg" alt="Compose M3" />
</p>

一款专为现代 Android 设计的**轻量、丝滑、极简**的边缘手势与小白条（WhiteBar）手势导航工具。

本项目基于原作者 [helloklf/EdgeGesture](https://github.com/helloklf/EdgeGesture) 进行了全方位的**现代化渐进式重构**，全面采用 **Kotlin + Jetpack Compose + Material 3 + Shizuku + DataStore** 架构，原生适配 **Android 16 (API 36)**，重写了核心手势引擎与设置界面。

---

## 🌟 核心特性

### 1. 🔘 小白条手势核心 (Modern WhiteBar)
- **极速响应与震感**：轻量级 Canvas 实时绘制，配备基于物理力学的弹簧回弹动画（Spring Animation）。
- **高频操作深度优化**：
  - **单击 (Click)**：轻触机械刻度震感，秒级启动目标应用。
  - **长按 (Long Press)**：精准 320ms 长按判定与沉稳触感，松手直达预设应用。
  - **左右滑动**：快速切换上一应用或执行指定动作。
  - **向上滑动 / 悬停**：返回桌面 / 呼出最近任务列表。
- **外观全自由调节**：实时预览宽度、高度、圆角、底边距、背景透明度等参数。

### 2. ⚡ Shizuku 官方特权加速 (Privileged Execution)
- **Binder IPC 直连**：取代旧版过时且占用内存的 `adb_process` 本地 HTTP 守护进程 (`:8906`)，更加纯净安全。
- **突破后台限制**：利用 Shizuku 特权启动应用，彻底规避 Android 10+ 对后台启动 Activity 的限制，做到瞬间启动。
- **100% 优雅降级**：未安装或未授权 Shizuku 时，应用自动平稳降级为纯无障碍服务模式，无需 Root 亦可完整使用。

### 3. 🛡️ 现代边缘手势与防冲突 (Side Gestures)
- **系统手势排除区域**：适配 Android 10+ `setSystemGestureExclusionRects()`，防止侧边手势与系统自带的侧滑返回手势打架。
- **多端设备自适应**：基于现代 `WindowMetrics` 计算坐标与边界，完美适配横竖屏旋转、折叠屏与平板设备。

### 4. 🎨 现代 Material 3 设置界面
- **Jetpack Compose 驱动**：摒弃旧版过时的 XML Layout、TabHost 和旧版 Fragments，界面更流畅、更美观。
- **Material You 动态主题**：支持 Android 12+ Monet 动态取色，与系统风格自然融合。
- **现代化应用选择器 (`AppPickerDialog`)**：内置应用图标异步加载、实时模糊搜索，一键绑定手势动作。

---

## 📊 现代化重构对比

| 模块 / 维度 | 旧版架构 (Java 8) | 现代重构版 (Kotlin + Compose) |
| :--- | :--- | :--- |
| **开发语言与 SDK** | Java 8, compileSdk 29, targetSdk 29 | **Kotlin 2.3.21, compileSdk 36, targetSdk 36 (Android 16)** |
| **构建系统** | Gradle 6.5 + AGP 4.1.1 (使用已废弃的 jcenter) | **Gradle 9.5.0 + AGP 9.3.1 + Version Catalog (toml)** |
| **UI 架构** | 传统 XML 布局 + TabHost + 旧版 Fragments | **Jetpack Compose + Material 3 (Material You 动态主题)** |
| **配置存储** | `SharedPreferences` (主线程阻塞风险) | **Jetpack DataStore (Preferences)** (响应式 StateFlow) |
| **特权增强模式** | 自建 Jar + 本地 HTTP 守护进程 (`:8906`) | **官方 Shizuku (Binder IPC)** (安全规范、零后台驻留) |
| **触感反馈** | 旧式 `Vibrator` 粗暴振动 | **`HapticsManager` 现代线性马达触感** (单击微震 + 沉稳长按) |
| **手势防冲突** | 无 | **适配 Android 10+ `setSystemGestureExclusionRects()`** |

---

## 🚀 快速上手

### 1. 下载安装
- 前往 [GitHub Releases](https://github.com/Aa-creat/EdgeGesture/releases) 或 [GitHub Actions Artifacts](https://github.com/Aa-creat/EdgeGesture/actions) 下载最新的 `app-debug.apk`。

### 2. 授权与配置
1. **授予无障碍服务权限**：
   - 打开应用，点击顶部的提示横幅跳转系统设置，在“无障碍 / 已安装的服务”中开启 **EdgeGesture**。
2. **配置小白条手势**：
   - 在主界面“小白条”标签页中开启开关。
   - 点击“单击操作”或“长按操作”，在应用列表中选择您需要快捷打开的 App。
3. **（可选）授权 Shizuku**：
   - 若您的设备已配置 [Shizuku](https://shizuku.rikka.app/)，切换至“Shizuku”标签页点击“申请授权”即可激活特权加速。

---

## 🛠️ 本地构建指南 (Build from Source)

### 环境要求
- **JDK**：OpenJDK 17 或 JDK 21+
- **Android SDK**：Android SDK Platform 36 (Android 16)
- **Gradle**：已内置 Gradle Wrapper 9.5.0

### 编译步骤
```bash
# 1. 克隆代码仓库
git clone https://github.com/Aa-creat/EdgeGesture.git
cd EdgeGesture

# 2. 编译 Debug APK
# Linux / macOS:
./gradlew assembleDebug

# Windows (PowerShell):
.\gradlew.bat assembleDebug
```

编译完成后，APK 文件将生成在：
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 CI / CD (GitHub Actions)

本项目配置了完整的 GitHub Actions 自动化持续集成流水线 [`.github/workflows/build.yml`](.github/workflows/build.yml)：
- **自动构建**：每当向 `master` / `main` 分支提交代码或发起 PR 时，CI 会自动校验并构建 Debug APK。
- **构件上传**：每次成功构建后，APK 构件将自动作为 Artifact 保存供直接下载。
- **自动发布 Release**：推送形如 `v1.3.0` 的 Git Tag 时，流水线将自动创建 GitHub Release 并附带编译好的 APK 资产。

---

## 📜 历史更新记录

<details>
<summary>点击展开查看 v1.0.1 ~ v1.2.0 历史日志</summary>

【1.2.0】
- 增加横竖屏使用小白条的开关，以及动作和外观自定义选项
- 优化细节与体验

【1.1.10】
- 增加横屏时手势区域更换成小白条选项

【1.1.9】
- 横屏时缩小底部手势区域降低游戏误触几率

【1.1.8】
- 增加三星OneUI独有的优化提示

【1.1.7】
- 修复了发现的问题

【1.1.6】
- 大幅（35%~90%）降低内存占用

【1.1.5】
- 震动反馈调节加入，根据自己的喜好调节震动强度

【1.1.4】
- 修复 底部热区高度调为0会遮挡全屏的问题
- 修复 手势动作设置项错位
- 增加悬停手势触发时长选项

【1.1.3】
- 增加热区灵敏度与宽度调整
- 进入设置界面时热区高亮显示

【1.1.2】
- 修复了一些动画细节问题

【1.1.0】
- 增加更多自定义选项
- 增加桌面图标显示开关

【1.0.1 ~ 1.0.6】
- 基础功能上线与 Android 5.0+ 兼容支持

</details>

---

## 🤝 致谢与许可

- 原项目由 [@helloklf](https://github.com/helloklf) 开发维护。
- 本项目遵循开源许可协议，欢迎提交 Issue 与 Pull Request 共同改进！
