# AutoDark Redux 全量迁移计划 — 支持最新 Android + Material 3 Expressive

> **For Hermes:** 分 5 阶段、23 个原子任务，每个 2-5 分钟，TDD + 频繁提交

**Goal:** 从 `SDK31/Material2/DataBinding` 一次性迁移到 `SDK36/Material3 Expressive/Dynamic Color + Themed Icons + Edge-to-Edge`，同时将 `DarkWallpaper` 从 2500 行重依赖瘦身到 100 行，无痛兼容 Android 12-16。

**Architecture:** 保留包名 `me.ranko.autodark`，View 体系渐进式引入 Compose（主设置页先上），Repository 分层解耦，`DarkWallpaper` 可选编译。

**Tech Stack:** AGP 8.7+ / Kotlin 2.1+ / Gradle 8.11 / SDK 36 / Material 3 `1.12+` / `DynamicColors` / `WindowInsets` / `PickVisualMedia` / `UiModeManager#setApplicationNightMode`

---

## Phase 1 — 工具链升级 (地基，必须最先)

### Task 1: 升级 Gradle Wrapper 7.0.2 -> 8.11
**Files:** Modify: `gradle/wrapper/gradle-wrapper.properties`
**Step:** `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11-bin.zip` 删除 `distributionSha256Sum`，执行 `./gradlew wrapper --gradle-version 8.11`
**Verify:** `./gradlew --version` 显示 8.11
**Commit:** `chore: bump gradle wrapper to 8.11`

### Task 2: 升级 AGP 7.0.4 -> 8.7.3 + 清理仓库
**Files:** Modify: `build.gradle: ver_gradle=8.7.3`, `settings.gradle` 移除 `jcenter()` 只留 `google()` `mavenCentral()` `jitpack.io`，`allprojects` 同步
**Step:** `buildscript.repositories { google(); mavenCentral() }` ，同步 `classpath "com.android.tools.build:gradle:$ver_gradle"`
**Verify:** `./gradlew help` 无 `jcenter` 警告
**Commit:** `chore: agp 8.7.3 + remove jcenter`

### Task 3: 升级 Kotlin 1.6.10 -> 2.1.0 + KSP 替代 kapt
**Files:** Modify: `build.gradle: ver_kotlin=2.1.0, ver_kotlin_coroutines=1.8.1`，`app/build.gradle` 将 `kotlin-kapt` -> `ksp` (新增 `id 'com.google.devtools.ksp' version '2.1.0-1.0.29'`)
**Step:** `kotlinOptions.jvmTarget=17`，`compileOptions.sourceCompatibility=17`
**Verify:** `./gradlew :app:compileDebugKotlin` 通过
**Commit:** `chore: kotlin 2.1.0 + ksp + jvm 17`

### Task 4: 升级 compileSdk/targetSdk 31 -> 36 + 依赖大版本对齐
**Files:** Modify: `app/build.gradle: compileSdk 36, targetSdk 36, buildTools 36.0.0`，`build.gradle: ver_appcompat=1.7.0, ver_material=1.12.0, ver_core=1.13.0, ver_lifecycle=2.8.7, ver_activity=1.9.3, ver_glide=4.16.0, ver_timber=5.0.1, ver_hiddenapi=4.3` 等
**Step:** `defaultConfig { minSdk 29 -> 26 保持? 保持29 }`，同步后 `Sync`
**Verify:** `./gradlew :app:assembleDebug` 能编过（预期有废弃警告，不阻塞）
**Commit:** `chore: sdk 36 + deps aligned to material 1.12`

### Task 5: 修复 Android 15/16 强制变更
**Files:** Modify: `app/src/main/AndroidManifest.xml` (加 `FOREGROUND_SERVICE` 类型), `app/src/main/java/me/ranko/autodark/services/*.kt`, `Utils/`
**Step:** 
- `READ_EXTERNAL_STORAGE` -> `READ_MEDIA_IMAGES` (API 33+ 分支)
- `FOREGROUND_SERVICE` 声明 `android:foregroundServiceType="specialUse"` 或移除未使用 service
- `Window` 去掉 `FLAG_LAYOUT_NO_LIMITS` 准备 Phase 4
**Verify:** `adb install` 在 Android 16 模拟器不崩
**Commit:** `fix: android 16 compat - storage perm + fgs type`

---

## Phase 2 — 代码瘦身 (删 2500 行重依赖)

### Task 6: 新建轻量 WallpaperRepository (100行)
**Files:** Create: `app/src/main/java/me/ranko/autodark/data/WallpaperRepository.kt`
**Code:**
```kotlin
class WallpaperRepository @Inject constructor(@ApplicationContext val ctx: Context) {
  private val wm = WallpaperManager.getInstance(ctx)
  private val prefs = ctx.getSharedPreferences("dual_wallpaper", MODE_PRIVATE)
  suspend fun save(uri: Uri, isDark: Boolean, flag: Int) // copy to files/dual_wallpaper/
  suspend fun apply(isDark: Boolean) // for each flag -> wm.setStream(FileInputStream(path), null, false, flag)
  fun hasDual(): Boolean
  suspend fun clear()
}
```
**Verify:** 单元测试 `WallpaperRepositoryTest` mock WallpaperManager
**Commit:** `feat: add lightweight WallpaperRepository`

### Task 7: 迁移 DarkWallpaperHelper 调用点到新 Repository
**Files:** Modify: `app/src/main/java/me/ranko/autodark/core/DarkModeSettings.kt: onAlarm/onBoot -> repo.apply()`，`app/src/main/java/me/ranko/autodark/ui/DarkWallpaperPickerViewModel.kt: persist() -> repo.save()`
**Step:** 保留旧 `DarkWallpaperHelper` 暂时兼容，新代码走 `WallpaperRepository`
**Verify:** `./gradlew test` + 手动切深色模式壁纸仍切换
**Commit:** `refactor: route dark wallpaper through new repo`

### Task 8: 删除 AOSP 壁纸副本 (38 文件)
**Files:** Remove: `app/src/main/java/com/android/wallpaper/**` (asset/model/module/picker/util/widget)
**Step:** `git rm -r app/src/main/java/com/android/wallpaper`，检查 `DarkWallpaperFragment` 引用改成 `FileAsset`/`Uri`
**Verify:** `./gradlew :app:compileDebugKotlin` 报错清单逐个修
**Commit:** `chore: remove 38 AOSP wallpaper files`

### Task 9: 删除 hidden-api-dark + Shizuku-API 子模块依赖 (可选编译)
**Files:** Modify: `settings.gradle` 删除 `hidden-api-dark`, `Shizuku-API/*` 6个 include，`app/build.gradle` 删除 `implementation project(':hidden-api-dark')` 等，新增 `hiddenapibypass:4.3` 已在 Task4
**Step:** `LargeWallpaperHelper` 中 `ShizukuApi` 调用改为 `if (BuildConfig.FLAVOR == "shizuku")` 可选
**Verify:** Clean build 通过，`./gradlew :app:assembleDebug` 无 Shizuku
**Commit:** `chore: remove hidden-api-dark + shizuku submodules`

### Task 10: Java -> Kotlin 批量转换 (49 files)
**Files:** Modify: `app/src/main/java/me/ranko/autodark/ui/widget/*.java` (5 files), `app/src/main/java/com/android/wallpaper` 已删，剩余 `Exception/CommandExecuteError.java` 等
**Step:** Android Studio `Code -> Convert Java File to Kotlin`，逐个 `git mv` + 修复 `!!` / `lateinit`
**Verify:** `./gradlew lint` 无 Java
**Commit:** `chore: java to kotlin migration`

### Task 11: 清理 28 处 Deprecated API
**Files:** Modify: 多处 `ProgressDialog`, `getColor(int)` -> `getColor(int, theme)`, `setNavigationBarColor` -> `WindowInsetsController`, `ActivityOptions` 等
**Step:** `Analyze -> Inspect Code` 导出 deprecated 列表逐个修
**Verify:** `lint --check Deprecated` 0 遗留
**Commit:** `fix: remove 28 deprecated APIs`

---

## Phase 3 — 架构重构 (MVVM + Repository)

### Task 12: 抽离 DarkModeScheduler
**Files:** Create: `app/src/main/java/me/ranko/autodark/domain/DarkModeScheduler.kt`，Modify: `MainViewModel.kt` 移除调度逻辑
**Step:** `AlarmManager` + `SunriseSunsetCalculator` + `DarkTimeUtil` 抽到独立 `Scheduler`，ViewModel 只 `scheduler.schedule(start, end)`
**Verify:** `DarkModeSchedulerTest` 验证日出日落计算
**Commit:** `refactor: extract DarkModeScheduler`

### Task 13: 抽离 LocationRepository
**Files:** Create: `app/src/main/java/me/ranko/autodark/data/LocationRepository.kt`，Modify: `MainFragment.kt` 移除 GPS 逻辑
**Step:** `FusedLocationProvider` 封装，`MainViewModel` 注入
**Verify:** Mock location 测试通过
**Commit:** `refactor: extract LocationRepository`

### Task 14: DataBinding -> ViewBinding + 去 ObservableField
**Files:** Modify: `app/build.gradle: buildFeatures { dataBinding false; viewBinding true }`，`MainActivity.kt` `FragmentDarkWallpaperBinding` 等
**Step:** `ObservableField` -> `StateFlow`/`LiveData`，`BindingAdapters` 重写为 Compose 或手动绑定
**Verify:** `./gradlew :app:assembleDebug`
**Commit:** `refactor: databinding to viewbinding + stateflow`

---

## Phase 4 — UI 现代化 (Material 3 Expressive + Dynamic Color)

### Task 15: 主题迁移 Material2 -> Material3
**Files:** Modify: `app/src/main/res/values/styles.xml` `values-night/styles.xml` `values/themes.xml` (新建)
**Step:**
```xml
<style name="AppTheme" parent="Theme.Material3.DayNight.NoActionBar">
  <!-- 删除 colorPrimary 等硬编码 -->
</style>
<style name="AppTheme.Dynamic" parent="ThemeOverlay.Material3.DynamicColors.DayNight"/>
```
`app/src/main/res/values/colors.xml` 删除 `primary/primaryVariant/secondary` 改用 `?attr/colorPrimaryContainer` 等 token
**Verify:** 浅/深主题自动切换正常
**Commit:** `feat: material 3 theme migration`

### Task 16: 启用 Dynamic Color (Monet)
**Files:** Modify: `app/src/main/java/me/ranko/autodark/AutoDarkApplication.kt`，`app/src/main/AndroidManifest.xml` `application.android:theme="@style/AppTheme.Dynamic"`
**Step:** `DynamicColors.applyToActivitiesIfAvailable(this)`，Compose 侧 `dynamicLightColorScheme()` fallback
**Verify:** 换壁纸后 App 主色跟随变化 (Android 12+)
**Commit:** `feat: dynamic color monet`

### Task 17: Themed Icon (Adaptive + Monochrome)
**Files:** Create: `app/src/main/mipmap-anydpi-v26/ic_launcher.xml`，`ic_launcher_round.xml`，Create: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
**Step:** 提供单色图层，`android:theme` 自动染壁纸色 (Android 13+，16强制)
**Verify:** 在 Launcher 桌面长按 `Themed icons` 开关看效果
**Commit:** `feat: themed adaptive icon`

### Task 18: Edge-to-Edge 强制适配 (Android 16)
**Files:** Modify: `MainActivity.kt` `BaseListActivity.kt` `DarkWallpaperPickerActivity.kt` `app/src/main/res/layout/*.xml`
**Step:** `WindowCompat.setDecorFitsSystemWindows(window, false)` + `ViewCompat.setOnApplyWindowInsetsListener` + `android:fitsSystemWindows=false`，所有 `paddingTop = statusBarsInsets.top`
**Verify:** 在 Android 16 上状态栏/导航栏不遮挡，键盘不顶飞
**Commit:** `fix: edge-to-edge for android 16`

### Task 19: 主设置页 Compose 重写 (M3 Expressive)
**Files:** Create: `app/src/main/java/me/ranko/autodark/ui/compose/MainScreen.kt` `SettingsCard.kt` `TimePickerDialog.kt`，Modify: `MainFragment.kt` -> `MainComposeFragment.kt`
**Step:** `Scaffold + TopAppBar + Card + ListItem + Switch`，时间选择用 `TimePicker` (M3)，`PreferenceCategory` -> `LazyColumn` + `Card`
**Verify:** 深色模式时间轴、自动按钮、Xposed卡片均为 M3 样式
**Commit:** `feat: compose main settings screen m3 expressive`

### Task 20: 壁纸选择器 Compose 简化 (iOS 双壁纸体验)
**Files:** Create: `app/src/main/java/me/ranko/autodark/ui/compose/DualWallpaperScreen.kt`，Modify: `DarkWallpaperFragment.kt` -> 轻量版或删除
**Step:** 两个 `Card(日间壁纸/夜间壁纸)` + `PickVisualMedia` + `WallpaperRepository.save()`，预览用 `AsyncImage`
**Verify:** 选两张图 -> 切深色模式自动换壁纸
**Commit:** `feat: dual wallpaper picker compose`

---

## Phase 5 — 质量加固

### Task 21: 核心逻辑单元测试
**Files:** Create: `app/src/test/java/me/ranko/autodark/Utils/DarkTimeUtilTest.kt` `DarkModeSchedulerTest.kt` `WallpaperRepositoryTest.kt`
**Step:** 覆盖 `isNight()`, `calculateSunrise()`, `apply()` 边界
**Verify:** `./gradlew :app:testDebugUnitTest` 绿
**Commit:** `test: core unit tests`

### Task 22: CI (GitHub Actions)
**Files:** Create: `.github/workflows/android.yml`
**Step:** `actions/setup-java@v4 + gradle-actions + ./gradlew test assembleDebug`
**Verify:** Push 触发 CI 绿
**Commit:** `ci: add github actions`

### Task 23: 文档 + 清理
**Files:** Modify: `README.md` (中英，附 M3 截图)，`fastlane/metadata/` 更新截图，`LICENSE` 保留 MIT
**Step:** 更新 screenshots 文案，标注 `Dynamic Color + Themed Icons`
**Verify:** README 渲染正常
**Commit:** `docs: update readme for redux`

---

## 风险与回退

| 风险 | 缓解 |
|---|---|
| AOSP 壁纸代码移除后裁剪失效 | Task 8 先在分支跑通再合主线，用 `PickVisualMedia` + `WallpaperManager.setStream` 无需裁剪 |
| Kotlin 2.1 编译失败 | Task 3 独立分支，失败回退 1.8 |
| Material 3 颜色 token 覆盖不全 | Task 15 逐个 `?attr/` 替换，保留 `colors.xml` 兜底 |
| Shizuku 用户投诉动态壁纸不可用 | Task 9 做成 `flavorShizuku` 可选编译，README 注明 |

## 不做

- 不改包名
- 不加网络请求/遥测
- 不改 F-Droid 元数据结构

## 验证总表

```bash
./gradlew :app:assembleDebug          # 编过
./gradlew :app:testDebugUnitTest      # 单测绿
adb install -r app-debug.apk          # Android 16 模拟器：换壁纸->App跟色，图标主题化，EdgeToEdge 不遮挡，日夜两张壁纸自动切
```
