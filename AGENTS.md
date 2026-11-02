# 项目开发约定

## 工作方式

- 以实现用户确认的架构和功能为主，不擅自扩大工作范围。
- 用户未要求时，不主动运行测试、模拟器检查或扩大测试体系。需要运行时遵从用户当次指令。
- 测试保持精简，现有核心用例优先维护；不为简单 UI、数据类、getter/setter 添加测试。
- 用户已授权本轮按“小项完成 → Git 提交 → 静态审核 → 继续”推进。发现问题单独修复并提交；不推送或发布。

## 目录与依赖

- 目录规划及每类文件上限见 `docs/code-structure.md`，新增文件按该规划归属。
- 保留 `app` 和 `core` 两个 Gradle 模块，不提前拆成大量模块。
- `core` 只放纯 Kotlin 业务逻辑，不依赖 Android、Compose、Room 或 Shevery。
- `app` 依赖 `core`，禁止反向依赖。
- UI 依赖 ViewModel/UiState；页面不直接访问数据库、Binder 或执行接单动作。
- 平台识别规则放进独立平台适配器；Android 隐藏 API 仅允许出现在 `bridge`。
- 共享代码必须有明确业务含义，不新增笼统的 `Utils.kt`、`Helpers.kt`、`Manager.kt`。

## 文件大小

以物理行数计数，空行和注释计入。不得用压缩代码、超长行或删除必要说明来规避上限。

| 文件类型 | 单文件硬上限 |
| --- | ---: |
| 普通 Kotlin/Java 文件 | 250 行 |
| 页面 Screen | 250 行 |
| ViewModel / 工作流协调器 | 200 行 |
| UI 组件 / 主题 / 格式化 | 150 行 |
| 平台解析器 / 接单执行器 / 权限服务 | 200 行 |
| 模型 / Entity / DAO / AIDL | 120 行 |
| Activity / Application / 根导航 / 系统 Service | 150 行 |
| 测试文件 | 120 行 |
| Gradle 脚本 / Manifest / 单个资源 XML / CI 配置 | 150 行 |

多个规则同时适用时取最小值；`docs/code-structure.md` 标注的更低上限优先。手写源文件绝不超过 300 行；规划内文件必须遵守对应的更低上限。生成代码、Gradle Wrapper、Room schema JSON 和文档不受源文件上限约束。

接近上限时先按职责拆分，不按行数切成 `Part1`、`Part2`。函数通常控制在 50 行内；复杂 Compose 布局优先拆成具名组件。禁止为了保持文件短小把业务规则挪进 UI。

## 已完成的目录整理

- UI 已按 navigation、dashboard、orders、regions、rules、components、theme 归档，页面经 ViewModel 获取状态。
- 原 `OverudRuntime.kt` 已删除，职责分入 work、capture、acceptance、analytics 和 di。
- 原 `Models.kt` 已拆入 model、valuation、platform、acceptance 和 region。
- data 已按 database/entity、database/dao、repository、mapping、preferences 归档。
- 隐藏 API 封装在 bridge；定位与悬浮窗分别位于 geo、overlay。
- `ValueEngineTest.kt` 保留现有少量用例，不随目录整理扩大测试数量。

新增功能继续遵守对应文件上限；不能在超限文件继续堆叠功能。
