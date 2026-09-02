# AutoDark Redux

[![works badge](https://cdn.jsdelivr.net/gh/nikku/works-on-my-machine@v0.2.0/badge.svg)][project_link]
[![API](https://img.shields.io/badge/API-29--36-brightgreen.svg)](https://android-arsenal.com/api?level=36)
[![Material 3](https://img.shields.io/badge/Material-3%20%7C%20Dynamic%20Color-blue)](https://m3.material.io)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple)](https://kotlinlang.org)

A small Android app to schedule dark mode On/Off — **Redux edition**.

Fork of [0ranko0P/AutoDark](https://github.com/0ranko0P/AutoDark) (MIT), modernized for Android 12–16 with Material 3 Expressive.

> 原版 2019 年停更于 `SDK 31 / Material 2 / DataBinding`，本分支一次性迁移到 `SDK 36 / Material 3 + Compose + Monet`。

## What's New in Redux

| 维度 | 原版 | Redux |
| :--- | :--- | :--- |
| **SDK** | 31 (Android 12) | **36 (Android 16)** + 兼容 29–36 |
| **Design** | Material 2 | **Material 3 + Dynamic Color (Monet) + Themed Icons + Edge-to-Edge** |
| **UI** | DataBinding + PreferenceFragment | **Compose Material 3** (主设置页) + View 渐进迁移 |
| **Wallpaper** | 2500 行 AOSP 拷贝 + Shizuku 强依赖 | **100 行 WallpaperRepository** (`WallpaperManager.setStream`)，Shizuku 可选 |
| **Toolchain** | AGP 7.0.4 / Gradle 7.0.2 / Kotlin 1.6.10 / jcenter | **AGP 8.7.3 / Gradle 8.11 / Kotlin 2.0.20 / mavenCentral** |

### ✨ 核心功能
*   **跟随壁纸变色** - `DynamicColors.applyToActivitiesIfAvailable()`，换壁纸全 App 自动染成同色系
*   **主题图标** - `monochrome` 层，桌面图标跟壁纸同色 (Android 13+，16 强制)
*   **日夜双壁纸** - 像 iOS 一样，日/夜各一张，调度时自动 `WallpaperManager.setStream()` 切换，无需 Shizuku
*   **Compose 预览** - 设置最底部 `✨ Material 3 Compose 预览` 进入新版主界面

## Requirements

*   AOSP Android 12–16 (API 29–36). OEM 深度定制 (Flyme/MIUI) 可能不兼容系统深色调度
*   需要 `android.permission.WRITE_SECURE_SETTINGS` (adb 授予：`adb shell pm grant me.ranko.autodark android.permission.WRITE_SECURE_SETTINGS`)
*   可选：Shizuku / Sui 仅用于动态壁纸 (`LiveWallpaper`)，静态壁纸无需

## Build

```bash
# JDK 17 (AGP 8.7 要求)
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/src/main/java/me/ranko/autodark/
├── data/WallpaperRepository.kt      # 100 行新仓库，替代 670 行 Helper
├── core/DarkModeSettings.kt         # 调度核心
├── ui/compose/                      # Compose Material 3
│   ├── theme/Theme.kt               # dynamicLight/DarkColorScheme
│   ├── MainScreen.kt                # 新主设置页
│   └── MainComposeActivity.kt
├── ui/MainFragment.kt               # 旧设置 (保留，入口在底部)
└── ui/DarkWallpaper*                # 旧壁纸逻辑 (待删 AOSP)
```

## Screenshots

<p align="middle">
    <img src="https://raw.githubusercontent.com/0ranko0P/AutoDark/master/fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.png" width="200" />
    <img src="https://raw.githubusercontent.com/0ranko0P/AutoDark/master/fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.png" width="200" />
</p>

> 新版 Monet 截图待补

## Credits

*   Original: [0ranko0P/AutoDark](https://github.com/0ranko0P/AutoDark) MIT
*   Redux: E-R-Butch

## License

MIT License

Copyright (c) 2019 0ranko0P

[project_link]: https://github.com/E-R-Butch/AutoDarkRedux
[fdroid_link]: https://f-droid.org/packages/me.ranko.autodark
