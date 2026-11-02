# 代码目录与文件规模规划

本文是目标结构，未列为已经完成的重构。根目录 `AGENTS.md` 为后续开发约束。

## 1. 模块划分

只保留两个模块：`core` 负责纯业务逻辑，`app` 负责 Android 能力、存储和界面。现阶段通过包边界组织功能，不引入多模块编译、复杂依赖注入框架或无实际用途的接口层。

目录中的数字为每个文件的物理行数硬上限；同类新增文件继承根目录规范。

## 2. 根目录

```text
overud/
├── AGENTS.md                          开发约束
├── README.md                          启动方式与实现进度
├── settings.gradle.kts                [100] 模块与仓库
├── build.gradle.kts                   [100] 根插件配置
├── gradle.properties                  [100] 构建参数
├── gradle/                            Wrapper 与依赖目录
├── app/                               Android 应用
├── core/                              纯 Kotlin 业务
├── docs/
│   ├── architecture.md                产品与技术架构
│   ├── code-structure.md              本文
│   └── screenshots/                   必要界面记录，不做自动截图体系
└── .github/workflows/android.yml       [100] 仅编译，不默认运行测试
```

## 3. 业务模块 core

文件位于 `core/src/main/kotlin/com/overud/`。

```text
model/
├── Platform.kt                        [60] 平台定义
├── Offer.kt                           [120] 报价及字段口径
├── Trip.kt                            [120] 实际配送、停靠点、结果
├── Policy.kt                          [120] 自动接单要求与合法性
└── Assessment.kt                      [120] 价值指标、判断理由、版本
platform/
├── OfferParser.kt                     [80] 平台适配器契约
├── UberEatsParser.kt                  [200] Uber Eats 报价解析
├── DoorDashParser.kt                  [200] DoorDash 报价解析
└── OfferIdentity.kt                   [120] 指纹与报价身份规则
valuation/
├── ValueEngine.kt                    [200] 收益和成本计算
└── EligibilityEvaluator.kt           [200] 所有接单硬条件评估
acceptance/
├── AcceptanceGate.kt                 [120] 全局执行占用、状态门控
└── AcceptanceState.kt                [80] 接单状态及动作结果
region/
├── RegionCell.kt                     [100] 区域网格与索引
└── RegionMetrics.kt                  [150] 区域指标、样本等级
```

计算与规则分开：ValueEngine 只算指标，EligibilityEvaluator 根据规则判断是否允许接单。平台解析器只产出标准模型，不访问数据库、不点击页面。只有确实复用的解析逻辑才抽到共享文件。

## 4. Android 应用 app

文件位于 `app/src/main/java/com/overud/`。允许保留现有 Java Room 文件，不为统一语言额外改写。

```text
OverudApp.kt                           [80] 应用启动与依赖容器
MainActivity.kt                        [120] Compose 入口、系统权限请求
di/
└── AppContainer.kt                    [150] 手动装配依赖
work/
├── WorkSessionController.kt           [180] 开始/结束工作、可接单状态
└── WorkState.kt                       [100] 工作状态
bridge/
├── SheveryBridge.kt                   [180] 连接、授权、Binder 生命周期
├── BridgeStatus.kt                    [80] 连接状态
├── PrivilegedService.kt               [150] AIDL 入口，委托具体能力
├── UiAutomationConnection.kt          [180] 隐藏 API 与连接管理
├── WindowReader.kt                    [180] 节点遍历、文本、窗口身份
└── ScreenshotReader.kt                [120] 截图和跨进程传输
capture/
├── WorkService.kt                     [150] 前台服务生命周期与通知
├── CaptureScheduler.kt                [180] 采集触发、节流、生命周期
├── CapturePipeline.kt                 [200] 识别流程，委托独立组件
├── OcrRecognizer.kt                   [150] 端侧 OCR 封装
├── OfferTracker.kt                    [180] 报价去重、更新与失效
└── OfferNotificationListener.kt       [100] 通知辅助触发
acceptance/
├── AutoAcceptController.kt            [200] 开关、规则复核、执行协调
├── AcceptActionExecutor.kt            [180] 平台动作分发
├── UberAcceptAction.kt                [180] Uber 按钮与确认步骤
├── DoorDashAcceptAction.kt            [180] DoorDash 按钮与确认步骤
└── AcceptanceObserver.kt              [150] 成功证据、超时和结果未知
geo/
├── WorkLocationTracker.kt             [150] 工作期间位置采样
├── DestinationResolver.kt             [180] 地址与目的地精度
└── RouteProvider.kt                   [80] 路线提供者边界
analytics/
├── RegionRecorder.kt                  [180] 区域与覆盖时长记录
├── RegionAnalyzer.kt                  [200] 按平台/时段汇总区域价值
└── DestinationEvaluator.kt            [180] 后续等待与接单机会
overlay/
├── OfferOverlay.kt                    [150] 窗口建立、更新、销毁
└── OverlayContent.kt                  [150] 报价摘要与立即暂停
```

`PrivilegedService` 不再混合连接、遍历、截图和接单规则。`WorkService` 只负责系统服务生命周期。自动接单业务必须统一经过 AutoAcceptController，UI、通知和采集模块不能直接点击接单。

### 数据存储

```text
data/
├── database/
│   ├── OverudDatabase.java            [120] Room 数据库及迁移注册
│   ├── entity/
│   │   ├── OfferRecord.java           [120]
│   │   ├── AttemptRecord.java         [120]
│   │   ├── ExposureRecord.java        [120]
│   │   └── TripRecord.java            [120]
│   └── dao/
│       ├── OfferDao.java              [120] 报价查询
│       ├── AcceptanceDao.java         [120] 接单日志与占用状态
│       ├── RegionDao.java             [120] 覆盖时长与区域聚合
│       └── TripDao.java               [120] 实际配送记录
├── repository/
│   ├── OfferRepository.kt             [180]
│   ├── AcceptanceRepository.kt        [180]
│   ├── RegionRepository.kt            [180]
│   └── TripRepository.kt              [180]
├── preferences/
│   └── PolicyStore.kt                 [150] 接单规则持久化
└── mapping/
    └── OfferMapping.kt                [120] 数据库与业务模型转换
```

Repository 封装读写与事务，不为每个实体机械地创建空接口。DAO 不能继续同时承担订单、接单和区域三类查询。

### UI

```text
ui/
├── navigation/
│   ├── OverudRoot.kt                  [150] Scaffold、导航、页面装配
│   └── Destination.kt                 [60] 页面定义
├── theme/
│   ├── Theme.kt                       [100] Material 3 Expressive 装配
│   ├── Color.kt                       [120] 浅色/深色语义颜色
│   └── Type.kt                        [100] 字体层级
├── dashboard/
│   ├── DashboardScreen.kt             [250]
│   ├── DashboardViewModel.kt          [180]
│   ├── DashboardUiState.kt            [100]
│   ├── WorkSessionCard.kt             [150]
│   └── AutoAcceptCard.kt              [150]
├── orders/
│   ├── OrdersScreen.kt                [220]
│   ├── OrdersViewModel.kt             [150]
│   └── OfferDetailSheet.kt            [150]
├── regions/
│   ├── RegionsScreen.kt               [220]
│   ├── RegionsViewModel.kt            [180]
│   └── RegionCard.kt                  [150]
├── rules/
│   ├── RulesScreen.kt                 [220]
│   ├── RulesViewModel.kt              [180] 草稿、校验、保存
│   ├── RulesUiState.kt                [120]
│   ├── EarningsFields.kt              [150]
│   ├── DistanceTimeFields.kt          [150]
│   ├── PlatformPreferences.kt         [150]
│   └── RulePreviewDialog.kt           [150]
└── components/
    ├── OfferCard.kt                   [150]
    ├── EmptyStateCard.kt              [100]
    ├── StatusChip.kt                  [80]
    ├── NumberField.kt                 [100]
    └── DisplayFormat.kt               [100]
```

页面只组合布局和分发用户操作。输入解析、规则保存和错误处理放到 RulesViewModel；接单按钮调用控制器，不把业务执行代码嵌入 Composable。

## 5. 上限执行方式

- 普通代码默认最多 250 行，模型/DAO 最多 120 行，组件最多 150 行；具体文件以上表为准。
- 250 行页面在约 200 行时就应考虑提取有明确含义的区域组件。
- 同一文件不混放模型、数据库操作、界面和业务执行。
- 不按固定数量切文件、不创建只有一次无意义转发的层，也不使用 `Part1/Part2`。
- 本规范不是要求立即创建所有空文件；实现对应职责时再创建。

## 6. 测试保持精简

用户没有要求时不主动运行测试或模拟器。初期保留现有 `ValueEngineTest.kt`，当前 74 行，避免为目录重构增加用例。

后续若用户需要维护核心测试，最多按价值规则、报价解析、接单状态三类组织，每个文件上限 120 行；只覆盖金额/单位错误、报价混淆、重复接单、未知结果等会影响实际订单的边界。不新增 UI 截图测试、为简单数据类写测试、追求覆盖率指标或重复生产实现。

CI 默认只编译 Debug APK，不默认运行测试。现有用例不删除、不扩张，也不增加强制覆盖率门槛。

## 7. 整理顺序

1. 先拆 `OverudRoot.kt`（当前 341 行）：根导航 → 四个页面 → 对话框与复用组件。
2. 再拆 `OverudRuntime.kt`（当前 250 行）：工作状态 → 采集流程 → 接单协调 → 区域记录。
3. 拆 `Models.kt`（当前 91 行）：模型与规则计算分开，随后拆两个平台解析器。
4. 最后按职责移动 data、bridge、capture，收敛依赖方向。

以上整理不同时扩张新功能或测试体系。文件行数是本次规划时的快照，不作为持续状态。
