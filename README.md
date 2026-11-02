# Overud

Android 外卖司机助手。基于 Shevery/Shizuku 采集 Uber Eats、DoorDash 报价，计算订单价值，并按用户规则自动接单。

UI 使用 Jetpack Compose 与 **Material 3 Expressive 1.5.0-beta02**，包括 Expressive 主题、动态组件形状及运动方案。界面提供工作台、订单历史、区域记录和接单规则。

## 当前实现

- Android 工程与独立 Kotlin 核心模块。
- Shevery 授权、Binder 生命周期、UserService、UI 自动化节点采集与截图管道。
- 目标平台白名单、保守报价解析及端侧 ML Kit OCR；OCR 单独识别的结果仅用于展示。
- 报价金额、每英里报价、预计毛时薪、配置车辆成本后的贡献估计。
- 自动接单总开关、平台开关、自定义最低报价/每英里报价/时薪，以及距离、耗时、叠单和追加单要求。
- 动作前报价指纹复核、全局执行协调、持久化接单日志、未知结果暂停和用户核对恢复。
- Room 本地报价记录、位置网格、按平台记录的采集覆盖时间与区域汇总。
- 规则文本预览：不执行接单、不写入真实报价历史。

目前是第一版开发实现，并非两个平台所有版本的完整适配。解析器只接受能够明确辨认字段及总距离/总耗时口径的报价，无法辨认时跳过自动接单。默认币种为 USD，距离界面使用英里。

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
5. 接单结果未确认或有配送占用时，后续自动接单暂停。确认平台当前没有配送后，使用“核对接单结果 / 配送状态”恢复可用状态。

结束工作、进程重启和规则修改都会关闭自动接单；保存的阈值保留。关闭开关不取消已经接下的平台订单。通知辅助触发不会在后台不可见页面上直接接单。

## 工程结构

```text
core/                         订单模型、解析、收益规则、执行门控、区域索引及测试
app/src/main/java/com/overud/
  bridge/                     Shevery 连接及特权 UI 自动化
  capture/                    工作服务、OCR 调度所需截图、悬浮窗、通知和定位
  data/                       Room 数据库与规则持久化
  ui/                         Material 3 Expressive 页面与主题
  OverudRuntime.kt            采集、评估、记录及接单协调
docs/architecture.md          完整目标架构及自动接单设计
```

## 已检查及尚未完成的边界

核心规则测试、Debug APK 编译和 Android Lint 已通过；在 Android 15 模拟器上检查启动和主要页面。模拟器没有真实平台账号与 Shevery 服务，因此尚未确认真实订单界面的覆盖率和实际接单行为。

区域汇总使用目标平台在前台且采集正常时的工作时长代理，不能代表平台真实在线时长或整个市场需求。当前没有地图供应商、地址地理编码、路线服务、目的地概率模型、实际配送收入结算、多步平台确认适配或云端同步。区域数据不足时明确显示未知，不生成模拟热区。

自动接单已实现语义按钮动作和明确成功文本识别，未明确识别成功时始终保留结果待核对状态；不会将卡片消失当作接单成功。

## 参考

- [目标架构](docs/architecture.md)
- [代码目录与文件上限](docs/code-structure.md)
- [Material 3 版本说明](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Shevery](https://github.com/HmnDev-Tech/shevery)
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)
