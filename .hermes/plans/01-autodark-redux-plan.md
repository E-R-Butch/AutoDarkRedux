# AutoDark Redux — 重构计划

## Phase 1 审查结果

| 维度 | 现状 | 目标 |
|------|------|------|
| compileSdk | 31 | 35 |
| targetSdk | 31 | 35 |
| AGP | 7.0.4 | 8.7+ |
| Kotlin | 1.6.10 | 2.1+ |
| 仓库 | jcenter() 已弃用 | 仅 mavenCentral + google |
| Java/Kotlin 混合 | 49 Java / 59 Kotlin | 全 Kotlin |
| AOSP 壁纸代码 | 38 文件，从 AOSP 复制 | 评估是否需要，如不需要则移除 |
| Shizuku 子模块 | 未初始化，依赖个人 fork | 改为官方 Shizuku API |
| Xposed hooks | 3 文件，功能有限 | 保留，清理 |
| 测试 | 零测试 | 至少加核心逻辑单元测试 |
| 过期 API | 28 处 | 全部修复 |
| UI | DataBinding + 自定义 View | 迁移到 Jetpack Compose（可选，视复杂度） |
| 架构 | MVVM 已有雏形 | 完善 ViewModel + Repository 分层 |
| 暗色壁纸功能 | 依赖 Shizuku + AOSP 代码 | 评估后决定保留/移除 |

## Phase 2 — 分步计划

### Step 1: 工具链升级
- AGP 7.0.4 → 8.7
- Kotlin 1.6.10 → 2.1
- compileSdk/targetSdk 31 → 35
- Gradle wrapper → 8.11
- 移除 jcenter()，改用 mavenCentral()
- 更新所有依赖到最新稳定版
- 修复编译兼容性问题

### Step 2: 代码清理
- Java → Kotlin 转换（剩余 49 个 Java 文件）
- 移除 `com.android.wallpaper` 下的 AOSP 副本（38 文件）— 改为依赖官方 Wallpaper API
- 移除 `hidden-api-dark` 模块 — 用官方 hiddenapibypass 替代
- 清理 28 处 deprecated API 调用
- 统一命名规范（Java 风格 → Kotlin 风格）
- 移除无用资源和 dead code

### Step 3: 架构重构
- 明确 MVVM 分层：Repository → ViewModel → Fragment/Activity
- 抽取 `DarkModeScheduler` 为独立模块
- GPS 定位逻辑从 ViewModel 中抽离到 Repository
- 移除 DataBinding，改用 ViewBinding 或 Compose
- 暗色壁纸功能评估：如果 Shizuku 子模块难以维护，考虑移除或改为可选编译

### Step 4: UI 现代化
- Material 3 升级（Material 1.5.0 → Material 3 / Material You）
- 支持 Dynamic Color（Monet）
- 主界面重新设计（更简洁的信息架构）
- 暗色模式时间选择器改为更直观的交互
- 移除冗余的动画和自定义 Widget

### Step 5: 质量加固
- 添加核心逻辑单元测试（DarkTimeUtil, DarkModeSettings）
- 添加 CI（GitHub Actions 构建 + 测试）
- 更新 README + 中英文档
- 清理 fastlane 元数据
- LICENSE 保留 MIT + 原作者署名

## 风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| Shizuku API 子模块初始化失败 | 暗色壁纸功能不可用 | 改为官方 Shizuku 依赖，可选编译 |
| AOSP 壁纸代码移除后功能缺失 | 壁纸选择器崩溃 | 先确认依赖关系再移除 |
| Kotlin 2.1 兼容性问题 | 编译失败 | 渐进式升级，保留回退 |
| Java → Kotlin 转换引入 bug | 运行时错误 | 逐个模块转换 + 测试 |

## 不做

- 不改包名（保持 F-Droid 兼容）
- 不添加网络请求（保持离线 App 定位）
- 不添加数据收集/遥测
- 不扩展功能范围（只做重构，不加新功能）
