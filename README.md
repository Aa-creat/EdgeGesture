# EdgeGesture (边缘手势 / 小白条)

[![Build APK](https://github.com/Aa-creat/EdgeGesture/actions/workflows/build.yml/badge.svg)](https://github.com/Aa-creat/EdgeGesture/actions/workflows/build.yml)

轻量、极简的屏幕边缘与小白条手势导航应用。原生适配 Android 16。

基于 [helloklf/EdgeGesture](https://github.com/helloklf/EdgeGesture) 渐进式重构，采用 Kotlin + Compose + Material 3 + Shizuku + DataStore 架构。

## 主要特性

- **小白条手势**：支持单击、长按（320ms）秒级启动指定应用，左右滑动切换应用，线性马达震感反馈。
- **边缘手势**：两侧边缘滑动与悬停，适配 Android 10+ 手势排除区域（防系统侧滑返回冲突）。
- **Shizuku 特权模式**：替代旧版后台常驻守护进程，支持特权极速启动与后台限制突破。
- **双开共存**：新版包名为 `com.omarea.gesture.modern`，可与旧版同时安装进行功能对比。
- **极致精简**：开启 R8 全量压缩优化，安装包仅约 1.5 MB。

## 下载

前往 [GitHub Actions](https://github.com/Aa-creat/EdgeGesture/actions) 下载最新构建的 `EdgeGesture-APKs`，或在 Releases 中获取。

## 编译

```bash
# Debug 编译
./gradlew assembleDebug

# Release 编译（体积仅 1.5MB）
./gradlew assembleRelease
```

## 更新日志

【2.0.0 重构版】
- 重写小白条手势引擎与线性马达触感反馈
- 采用 Jetpack Compose + Material 3 重建设置界面
- 引入 Shizuku 官方 API 替换旧版 `adb_process` 本地 HTTP 守护进程
- 适配 Android 16 (API 36) 及防侧滑手势冲突
- 独立包名 `com.omarea.gesture.modern` 支持与原版并存比对
- 启用 R8 极致瘦身，安装包体积缩减至约 1.5 MB

<details>
<summary>展开查看 1.0 ~ 1.2 历史日志</summary>

【1.2.0】
- 增加横竖屏使用小白条的开关，以及动作和外观自定义选项

【1.1.10】
- 增加横屏时手势区域更换成小白条选项

【1.1.9】
- 横屏时缩小底部手势区域降低游戏误触几率

【1.1.8】
- 增加三星OneUI独有的优化提示

【1.1.0 ~ 1.1.7】
- 优化动画与内存占用，增加手势自定义与桌面图标开关

</details>

## 致谢

原项目由 [@helloklf](https://github.com/helloklf) 开发。
