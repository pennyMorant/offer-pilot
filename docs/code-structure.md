# 代码目录与文件规模规划

本文定义目录职责与文件上限，现有功能已完成对应目录整理。尚未实现的业务文件在功能开发时创建，不提前创建空壳。根目录 `AGENTS.md` 为后续开发约束，具体进度见 `implementation-progress.md`。

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
│   ├── OverudDatabase.java            [120] 数据库版本与 DAO
│   ├── entity/
│   │   ├── HourlyQualityRecord.java   [120] 每小时质量累计，不含逐单明细
│   │   ├── StatisticsCheckpoint.java  [80] 每平台一个去重指纹
│   │   ├── AttemptRecord.java         [120] 一个接单恢复槽位
│   │   └── ExposureRecord.java        [120] 每小时覆盖时间
│   ├── dao/
│   │   ├── HourlyQualityDao.java      [120] 汇总读写与查询
│   │   ├── QualityQueries.java        [100] 共用统计投影
│   │   ├── AcceptanceDao.java         [120] 接单恢复状态
│   │   └── RegionDao.java             [120] 覆盖时间与清理
│   ├── projection/
│   │   └── QualitySummary.java        [120] 统计查询投影
│   └── migration/
│       ├── AggregateSchema.kt        [120] 汇总表定义
│       └── DatabaseMigrations.kt     [150] 旧报价聚合后移除明细
├── repository/
│   ├── StatisticsRepository.kt       [180] 贡献修正、去重、保留期
│   ├── StatisticsSnapshot.kt         [100] 页面统计快照
│   ├── AcceptanceRepository.kt       [180]
│   └── RegionRepository.kt           [180]
├── preferences/
│   ├── PolicyStore.kt                [150]
│   ├── RuleJsonCodec.kt              [150]
│   └── TrackerCheckpointStore.kt     [120] 每平台固定一个指纹
└── mapping/
    └── QualityAccumulator.kt         [150] 内存样本更新汇总
```

不新增 OfferRecord、TripRecord 等逐单明细表。Repository 封装事务及累计，DAO 分开处理质量、接单恢复和覆盖时间。统计规则见 `aggregation-storage.md`。

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
├── statistics/
│   ├── StatisticsScreen.kt            [220]
│   └── StatisticsViewModel.kt         [150]
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

用户没有要求时不主动运行测试或模拟器。保留现有 `ValueEngineTest.kt`；本轮仅更新业务类的包引用和评估入口，未增加用例。

后续若用户需要维护核心测试，最多按价值规则、报价解析、接单状态三类组织，每个文件上限 120 行；只覆盖金额/单位错误、报价混淆、重复接单、未知结果等会影响实际订单的边界。不新增 UI 截图测试、为简单数据类写测试、追求覆盖率指标或重复生产实现。

CI 默认只编译 Debug APK，不默认运行测试。现有用例不删除、不扩张，也不增加强制覆盖率门槛。

## 7. 已完成整理

1. `OverudRoot.kt` 已拆分；工作台卡片、规则预览和公共组件独立，页面使用 ViewModel / UiState。
2. `OverudRuntime.kt` 已移除；工作状态、采集、接单、区域记录与依赖装配独立。
3. `Models.kt` 已拆分；收益计算与接单条件判断分开，两个平台使用独立解析入口与共享文本解析实现。
4. data、bridge、capture、geo、overlay 已归档；数据库已改为汇总结构，提供 v1→v2 迁移代码。

本轮没有扩张测试体系。以静态审核检查包引用、依赖方向和物理行数；这不等同于编译或设备运行结果。

## 8. 汇总分析新增职责

- core/analytics：AnalysisBucket、QualityEvaluator、QualitySample、MetricHistogram，各文件上限 150 行；纯模型文件仍按 120 行上限。
- app/analytics/QualitySummaryReducer.kt：统计投影分组累计，上限 150 行。
- ui/components：AnalysisSelection、AnalysisUiState、AnalysisFilters、RegionPicker、QualityCard、QualityCardState、QualityPresentation、QualityStandardNote；模型上限 120 行，组件/格式化上限 150 行。
- 不恢复逐单历史页面。地区和时段均使用统计快照，页面不直接访问数据库。

## 9. 目的地与配送状态职责

- core/platform/DeliveryLocationParser.kt、DeliveryScreenParser.kt：明确标签的位置与配送界面解析，上限 200 行。
- core/model/DestinationAssessment.kt、DeliveryPhase.kt：目的地和配送状态模型，上限 120 行。
- core/analytics/DestinationEvaluator.kt：以预计时段选择历史区域证据，上限 200 行。
- core/region/DestinationCoordinates.kt：显式坐标解析，上限 120 行。
- app/geo/AndroidGeocodingProvider.kt、DestinationResolver.kt：异步系统查询与有界内存缓存，各上限 180 行。
- app/analytics/DestinationController.kt：隔离报价、解析和历史证据状态，上限 180 行。
- app/acceptance/AcceptanceObserver.kt：配送占用、明确完成及恢复门控，上限 150 行。
- ui/components/DestinationPresentation.kt、DestinationSummary.kt：参考口径与展示，各上限 150 行。

不因接入地址解析或配送状态而增加逐单明细表。

## 10. 送达后等待观测职责

- core/model/WaitingAssessment.kt、core/analytics/WaitingObservation.kt：等待证据和聚合输入，各上限 120 行。
- core/analytics/WaitingEvaluator.kt：按时段选择等待证据，上限 200 行。
- app/analytics/WaitingRecorder.kt：单个内存观测区间及中断处理，上限 200 行。
- data/database/entity/RegionWaitingRecord.java、dao/WaitingDao.java、projection/WaitingSummary.java、migration/WaitingSchema.kt：小时汇总及 v2→v3 迁移，各上限 120 行。
- data/repository/WaitingRepository.kt：等待累计事务和历史快照，上限 180 行。
- ui/components/WaitingPresentation.kt：完成样本均值和中断数口径，上限 150 行。

不新增逐单等待表或区间恢复日志。

## 11. 配送观测汇总职责

- core/analytics/DeliveryMeasurement.kt：观测版本与时长有效范围，上限 120 行。
- app/analytics/DeliveryRecorder.kt：一个候选和当前配送的内存关联，完成/中断处理，上限 200 行。
- data/database/entity/DeliveryAggregate.java、dao/DeliveryDao.java、projection/DeliverySummary.java、migration/DeliverySchema.kt：小时结果累计及 v3→v4 迁移，各上限 120 行。
- data/repository/DeliveryRepository.kt：完成/计时/配对独立分母的累计事务，上限 180 行。
- ui/components/DeliveryCardState.kt：纯展示模型，上限 120 行；DeliveryPresentation.kt、DeliveryCard.kt：筛选分组、均值格式和展示，各上限 150 行。

地区和时段页继续经 ViewModel/UiState 获取汇总，不读取逐单配送，不直接访问数据库。

## 12. 车辆成本与时薪口径

- core/model/HourlyBasis.kt：毛报价与扣配置车辆成本后口径，上限 120 行。
- core/model/Assessment.kt：贡献、车辆成本和成本后时薪估计，上限 120 行。
- ValueEngine / EligibilityEvaluator 继续分别负责计算与硬条件，维持原有各 200 行上限。
- ui/components/VehicleCostPresentation.kt、VehicleCostSummary.kt：同一套估算格式和卡片展示，各上限 150 行。
- Policy / AcceptanceRules、规则偏好、草稿、预览与平台继承继续在原职责中处理，不增设存储表。

不把用户配置成本混入固定历史质量标准，不将成本后报价时薪称为净时薪或完整周期时薪。

## 13. 区域质量接单要求

- core/model/RegionalRequirement.kt、DestinationEvidence.kt：要求和临时报价证据，各上限 120 行。
- core/valuation/RegionalEligibilityEvaluator.kt：当前报价、时效、精度、范围和样本门控，上限 200 行。
- app/work/OfferDecision.kt：在现有工作快照中核对统计修订后评估报价，上限 120 行。
- data/preferences/RegionalRuleCodec.kt：公共/平台配置共用编码与校验，上限 120 行。
- ui/rules/RegionalRequirementFields.kt：开关和阈值输入，上限 150 行。
- DestinationController 继续负责异步解析和证据发布，上限 180 行；不在回调中执行点击。

不持久化临时目的地证据，不新增逐单表或查询日志。

## 14. 平台页面与报价观察

- core/model/PlatformPage.kt：页面类型、来源与信号，上限 120 行。
- core/platform/PlatformPageRules.kt：独立平台词表模型，上限 120 行。
- UberEatsPages.kt、DoorDashPages.kt、PageClassifier.kt：各平台标签与冲突分类，各上限 200 行。
- QuoteMoneyParser.kt、RouteMetricsParser.kt、TextOfferParser.kt：金额、指标作用范围和标准报价构造，各上限 200 行。
- QuoteObservationResolver.kt：完整观察选择及来源冲突，上限 200 行；其中 QuoteObservation 模型不超过 120 行。
- ui/components/PlatformPagePresentation.kt、OfferFieldPresentation.kt：页面与指标口径格式化，各上限 150 行。

页面规则不放进 Android 工作流；隐藏 API 仍只在 bridge。页面观察、原始文字与来源差异不持久化。

## 15. 平台接单动作

- core/platform/AcceptanceActionPolicy.kt：首步动作词表选择、完整标签匹配和页面门控，上限 120 行。
- app/bridge/AcceptTargetResolver.kt：活动窗口、报价指纹、节点与可点击父节点复核，上限 180 行。
- AutoAcceptController 负责确认等待任务及超时状态，沿用 200 行上限；超时不释放未决占用，不增加数据库记录。
- core/platform/AcceptanceConfirmationPolicy.kt：确认页报价锚点与动作匹配，上限 120 行。
- app/acceptance/AcceptanceConfirmation.kt：一个内存确认上下文、连接/规则/意愿复核与一次性执行，上限 180 行。


## 16. 手动图片测试入口

- capture/ImportedImageRecognizer.kt：用户选图解码、缩放、OCR 及资源释放，上限 150 行，不连接实时采集或接单。
- ui/imagetest/ImageTestUiState.kt：纯内存预览状态，上限 120 行。
- ui/imagetest/ImageTestViewModel.kt：异步识别、过期回调隔离与已保存规则评估，上限 180 行。
- ui/imagetest/ImageTestDialog.kt：选图器、平台选择、字段与 OCR 原文展示，上限 150 行。

仅从工作台进入，不新增持久化数据或测试记录。关闭清空，结果不获得自动点击资格。

## 17. Prop 22 估算

- core/model/Prop22Policy.kt、Prop22Estimate.kt：假设配置与结果，各上限 120 行。
- core/valuation/Prop22Estimator.kt：2026 费率、可信总量及单单情景补差，上限 200 行。
- data/preferences/Prop22Preferences.kt：全局参数读写与损坏回退，上限 120 行。
- ui/rules/Prop22Input.kt：草稿与校验，上限 120 行；Prop22Fields.kt：设置组件，上限 150 行。
- ui/components/Prop22Presentation.kt、Prop22Summary.kt：共用说明与展示，各上限 150 行。

估算不写统计、不改变自动接单条件；不假装知道实际小费或平台结算周期应收额。

## 18. 驾驶语音

- speech/SpeechEngine.kt：中文离线 TTS 初始化、音频焦点、播放和释放，上限 180 行。
- speech/VoiceController.kt：全局语音设置、实时状态订阅、短期去重与试听，上限 180 行。
- speech/OrderAnnouncement.kt：播报文案及可信字段选择，上限 150 行。
- ui/rules/VoiceViewModel.kt：语音设置和动作入口，上限 120 行；VoiceSettingsCard.kt：设置组件，上限 150 行。

语音只消费现有 WorkState，不调用接单动作或写入统计。偏好单独保存，不修改自动接单意愿。

## 19. 结构化窗口与采集唤醒

- core/model/UiNodeSnapshot.kt：节点、边界与选择结果模型，上限 120 行。
- core/platform/NodePageAssembler.kt：页面信号保留、报价容器筛选，上限 200 行。
- app/bridge/WindowNodeMapper.kt：Android 节点到纯模型映射，上限 150 行。
- app/bridge/WindowChangeRelay.kt：事件过滤和合并，上限 150 行；IWindowEvents.aidl 上限 120 行。
- app/capture/DeviceInteractionGuard.kt、CaptureLease.kt：设备可交互检查与工作代次/超时保护，各上限 120 行。
- CaptureScheduler 继续串行协调采集，上限 180 行；CapturePipeline 继续保持 200 行上限。

节点树不落库，事件只传唤醒信号，不持有截图或订单历史。过期动作不开始点击，不能宣称撤回已提交系统的动作。

## 20. 报价区域 OCR

- NodePageText 增加独立 OCR 容器索引，仍按模型 120 行上限；NodePageAssembler 维持 200 行上限。
- bridge/ScreenshotFrame.kt：截图请求与节点快照身份核对，上限 120 行。
- ScreenshotReader.kt：同帧检查、默认显示屏裁剪、有界管道与位图释放，维持 120 行上限。
- WindowReader 输出临时裁剪边界和结构指纹；OcrRecognizer 只接收同帧截图，来源始终标记 OCR。

无证据不猜裁剪坐标，越界不静默修正；截图不落盘，不新增明细表。

## 21. 临时节点诊断

- bridge/WindowDiagnosticCodec.kt：手动节点传输及容量校验，上限 120 行。
- bridge/NodeDiagnosticReader.kt：单个诊断读取及连接一致性，上限 120 行。
- ui/diagnostics/NodeDiagnosticState.kt：单份内存状态，上限 120 行。
- NodeDiagnosticViewModel.kt：倒计时、超时、工作代次与关闭清理，上限 180 行。
- NodeDiagnosticDialog.kt、NodeRegionDiagram.kt：节点列表、文字显示开关和几何区域图，各上限 150 行。

诊断读取不接入统计或自动点击；普通采集不传原始节点列表，关闭清空临时数据。

## 22. 运行状态与权限诊断

- device/DeviceStatus.kt：只读设备状态，上限 120 行；DeviceStatusReader.kt：标准系统状态读取，上限 150 行。
- ui/diagnostics/RuntimeDiagnosticRows.kt：状态解释和设置动作映射，上限 150 行。
- RuntimeDiagnosticsViewModel.kt：状态合并、手动刷新与授权入口，上限 180 行。
- RuntimeDiagnosticsDialog.kt：生命周期刷新、状态展示和用户触发设置跳转，上限 150 行。

不增加数据库记录或权限声明，不自动修改系统设置，不将可选权限缺失合并成核心功能不可用。

## 23. Shevery 原生接入与 Uber 卡片

- model/OfferService.kt、TextRegion.kt：订单类型及临时 OCR 行几何，各上限 120 行。
- platform/UberOfferCard.kt、SpatialPageText.kt：实际卡片模板与空间阅读顺序，各上限 200 行。
- capture/OcrPageText.kt：ML Kit 行到纯模型转换，上限 150 行；实时与选图共用。
- data/database/migration/OfferServiceSchema.kt：v4→v5 累计表类型维度，上限 120 行。
- 原有连接、去重、统计、目的地及 UI 文件继续遵守各自上限。不得因类型分组扩展逐单存储。

具体接入依据与尚未运行的截图核对目标见 `shevery-uber-repair.md`。

## 24. 页面重设计与动态主题

- 最低 API 33。data/preferences/AppearancePreferences.kt：偏好、主题模式与可观察状态，上限 120 行。
- ui/settings/AppearanceViewModel.kt 上限 180 行；SettingsScreen.kt 上限 250 行。
- ui/components/AnalysisPage.kt、AnalysisOverview.kt、QualityRow.kt：筛选、概览和紧凑分析展示，上限 150 行；其中纯状态模型仍遵守 120 行限制。
- ui/rules/RuleInputValidation.kt、RuleSummary.kt：输入提示与摘要，上限 150 行。规则是否有效仍由既有解析器和 core 判断。
- 图像测试、节点诊断、文本规则调试 UI 与专属 ViewModel 改放 `app/src/debug/java`，继承原文件上限。ImportedImageRecognizer、NodeDiagnosticReader 也迁到 debug。
- `debug` 与 `release` 分别提供 DeveloperViewModels.kt（上限 120 行）、DeveloperTools.kt（上限 150 行）；公共源集禁止引用测试专属页面或 ViewModel。
- 规则页不再承载外观、语音或权限设置。根导航统一处理未保存草稿离开提示。
