# Overud

Android 外卖司机助手。基于 Shevery/Shizuku 采集 Uber Eats、DoorDash 报价，计算订单价值，并按用户规则自动接单。

UI 使用 Jetpack Compose 与 **Material 3 Expressive 1.5.0-beta02**。界面提供工作台、时段质量统计、地区质量对比和接单规则。

## 当前实现

- Android 工程与独立 Kotlin 核心模块。
- Shevery 授权、Binder 生命周期、UserService、UI 自动化节点采集与截图管道。
- 目标平台白名单、保守报价解析及端侧 ML Kit OCR；OCR 单独识别的结果仅用于展示。
- 报价金额、每英里报价、预计毛时薪、配置车辆成本后的贡献估计。
- 自动接单总开关、平台开关；公共要求与 Uber Eats / DoorDash 独立要求，支持跟随公共设置。
- 最低报价、每英里报价、毛时薪、总距离、耗时、取餐距离分别启停；至少保留一项最低收益要求。支持商家和目的地排除关键词。
- 编辑不同范围时保留各自草稿，统一保存；保存期间暂停自动接单，成功后需主动重新开启。
- 动作前报价指纹复核、全局执行协调、单个持久化接单恢复状态、未知结果暂停。
- 订单模型在内存分析后更新 Room 汇总行，不保存逐单报价、地址、截图或轨迹明细。
- 按平台、位置网格、当地小时和质量版本累计报价数量、质量分类、指标累计值和区间分布，默认保留 90 天。
- 统计页支持平台、地区、星期和早餐/午餐/下午/晚餐/夜间筛选；地区页可比较相同时段的不同地区。
- 各项均值使用独立有效样本数，未知字段单列；固定质量标准独立于自动接单设置。
- 明确标签的商家/送达位置解析，支持坐标及 Android 系统地理编码；地址只进入有界内存缓存。
- 当前报价与悬浮窗显示目的地区域历史质量、预计送达时段、参考范围及样本量；不输出未经校准的接单概率。
- 配送状态识别：进行中暂停接单，明确完成后解除占用，并按用户保留的开关意愿继续；未知状态保留人工核对。
- 规则文本预览不执行接单，也不写入真实统计。

目前是第一版开发实现，并非两个平台所有版本的完整适配。只对已开启的条件检查其必需字段；需要距离/耗时时必须确认总口径，缺少信息则跳过。未启用的条件不强制要求对应字段，OCR 单独识别的报价始终只展示。默认币种为 USD，距离界面使用英里。

## 构建

需要 Android SDK 37、JDK 21 或兼容的 Android Studio JBR。最低系统 Android 11。

在 Android Studio 打开根目录，或配置 `local.properties`：

```properties
sdk.dir=/path/to/Android/sdk
```

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 启动手机上的 Shevery，在 Overud 工作台点击“检查连接并授权”。
2. 在规则页保存接单要求；按需开启悬浮窗、通知读取及位置权限。
3. 点击“开始工作”，在平台 App 中打开报价界面。
4. 需要自动执行时开启“自动接单”；所有要求通过且当前报价复核一致时才执行。
5. 进行中的配送会暂停自动接单。系统观察到进行中界面，并持续识别同平台完成界面后，解除占用；开关仍开启时继续接单。
6. 未确认的结果保持暂停。确认平台当前没有配送后，可使用“核对接单结果 / 配送状态”；人工核对会关闭开关，需要主动重新开启。

结束工作、进程重启和规则修改都会关闭自动接单；保存的阈值保留。关闭开关不取消已经接下的平台订单。通知辅助触发不会在后台不可见页面上直接接单。

## 工程结构

```text
core/                         model、platform、valuation、acceptance、region、analytics
app/src/main/java/com/overud/
  di/                         依赖装配与 ViewModel 工厂
  work/                       工作时段与共享工作状态
  bridge/                     Shevery 连接及特权 UI 自动化
  capture/                    工作服务、采集调度、OCR、报价追踪和通知
  acceptance/                 自动接单协调、复核和确认
  analytics/                  区域覆盖时长记录与质量汇总
  geo/                        工作期间定位
  overlay/                    悬浮报价与立即暂停
  data/                       database、repository、mapping、preferences
  ui/                         navigation、各页面 ViewModel、组件与主题
docs/architecture.md          完整目标架构及自动接单设计
```

## 已检查及尚未完成的边界

目录重构前的初始版本曾通过核心规则测试、Debug APK 编译及 Android Lint，并检查过 Android 15 模拟器页面。后续目录整理曾仅做静态审核。2026-11-02 已为安装交付执行 `:app:assembleDebug` 并成功，未运行测试或模拟器；编译成功不代表真实平台适配通过。尚未确认真实平台订单覆盖率和实际接单行为。

区域汇总使用目标平台在前台且采集正常时的工作时长代理，不能代表平台真实在线时长或整个市场需求。已接入 Android 系统地址解析和目的地历史质量参考；已加入送达后持续观察平台页面的等待汇总和目的地参考（完成/中断分开）。地图展示、路线服务、下一单概率/总体等待预测、配送收入结算及云端同步尚未接入；多步接单确认已接入严格字段对应的基础版，尚未确认真实平台覆盖率。系统地址解析可能依赖网络，基础报价计算和自动接单不等待它。

数据库已增加 v1→v2 迁移代码：将旧报价汇总后移除明细表，旧数据按 UTC 和“口径未知”单列。另有 v2→v3 的等待汇总表迁移，不补造历史等待样本。迁移尚未执行。当前区间分布已累计，页面展示均值、质量计数和优质比例，不声称提供精确中位数或任意重算历史。

自动接单已实现语义按钮动作和明确成功文本识别，未明确识别成功时始终保留结果待核对状态；不会将卡片消失当作接单成功。追加单已能提取明确标签的增量金额和路线，但既有配送上下文及后续动作复核尚未接入，因此当前仍保留手动接单。

## 参考

- [目标架构](docs/architecture.md)
- [代码目录与文件上限](docs/code-structure.md)
- [分项提交与静态审核记录](docs/implementation-progress.md)
- [汇总存储与质量口径](docs/aggregation-storage.md)
- [目的地评估与配送状态](docs/destination-delivery.md)
- [Material 3 版本说明](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Shevery](https://github.com/HmnDev-Tech/shevery)
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)

送达后等待观测的起止条件、样本限制和汇总字段见 [等待观测说明](docs/observed-waiting.md)。

配送完成观测及报价耗时对比已接入地区/时段统计页，只保存小时汇总；手动或重启后起点未知的配送不补造时长。数据库增加 v3→v4 迁移代码，详见 [配送观测说明](docs/delivery-observations.md)。

车辆成本估算已在报价卡片、悬浮窗和规则预览中展示；最低时薪可按毛报价或扣配置车辆成本后计算，支持平台独立要求。当前估算不含后续等待和回程，不等于净利润，详见 [车辆成本说明](docs/vehicle-cost.md)。

自动接单新增可选目的地区域质量要求，默认关闭，可自定义最低历史优质比例与样本数，支持平台独立覆盖。开启后解析不明、时段样本不足或证据过期均不通过，详见 [区域接单要求](docs/regional-acceptance.md)。

已接入 Uber Eats / DoorDash 独立页面词表、总量/增量字段口径及完整来源观察选择。历史与冲突页不解析成新报价，OCR 不获得自动执行资格。当前覆盖和限制见 [平台页面说明](docs/platform-pages.md)。

本次安装包及操作说明见 [安装交付说明](docs/install-handoff.md)。
