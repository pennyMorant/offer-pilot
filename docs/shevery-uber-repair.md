# Shevery 接入与 Uber 实际卡片修复

## 接入依据

核对 Shevery 官方仓库的原生客户端契约和服务器分发实现：

- https://github.com/HmnDev-Tech/shevery/blob/main/shevery-api/README.md
- https://github.com/HmnDev-Tech/shevery/blob/main/shevery-api/api/src/main/java/com/hamondev/shevery/api/SheveryApi.java
- https://github.com/HmnDev-Tech/shevery/blob/main/server/src/main/java/rikka/shizuku/server/ShizukuService.java
- https://github.com/HmnDev-Tech/shevery/blob/main/server/src/main/java/rikka/shizuku/server/BinderSender.kt

原生客户端需要声明 `com.hamondev.shevery.permission.PRIVILEGED_API`。只声明旧的 `moe.shizuku.manager.permission.API_V23` 会落到依赖兼容 stub 的旧路径。当前 Manifest 增加原生权限，同时保留旧权限用于旧服务器兼容。

官方服务对原生客户端优先尝试 `.shevery` Provider，随后回退 `.shizuku`，并同时发送两套 Binder extra。因此继续复用现有 Shizuku Provider 和 UserService 传输层，不引入没有发布坐标的 SDK 依赖，也不要求用户安装兼容 stub。

连接流程记录本次工作绑定意愿：授权或 Binder 到达后继续绑定；12 秒超时；远程 snapshot 没有报告读取错误、事件注册成功后才发布 serviceReady。代次保护阻止停止工作后的旧回调恢复连接。异常由工作状态订阅关闭自动接单意愿；连接恢复不自动开启接单。

## 真实 Uber 卡片

独立 Exclusive 模板以 Package / Delivery 标记、单一报价、紧随报价的分钟与英里、Guaranteed (incl. tip)、Accept 定位字段。继续先检查全页历史、完成、追加及冲突信号，不因裁剪绕过页面分类。

- Package：取件地址可带下一行美国邮编；评分、Verified 不作为地址或金额。
- Delivery：商家与目的地按卡片行序提取。
- 地图区域不参与卡片金额、路线和地址提取。
- 没有明确取餐距离就保持未知，不用总里程替代。
- 包裹与餐饮均标记报价含小费；没有小费明细，Prop 22 仍采用用户配置的假设，不将 Guaranteed 当成额外补差。
- 模板缺字段、来源冲突或仅 OCR 识别不获得自动接单资格。

OCR 保留行边界并按空间顺序组行，实时采集和选图入口共用处理。节点裁剪允许根据卡片标记、报价和按钮定位，不再必须先解析出完整地址。最终点击仍由实时节点、窗口身份、按钮唯一性及截止时间复核决定。

## 截图核对目标（尚未执行图片测试）

| 类型 | 报价 | 分钟 | 英里 | 报价/英里 | 报价折算时薪 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Package | $16.06 | 55 | 17.8 | $0.90 | $17.52 |
| Delivery | $5.01 | 24 | 2.3 | $2.18 | $12.53 |

这些是从用户截图人工读取的预期结果，不是本轮运行 OCR 的结果。没有复制截图或完整订单地址进仓库。

## 统计和交互

数据库 v4→v5 只给 hourly_quality 增加 serviceKind 累计维度，旧记录标记 UNKNOWN。DAO、分组展示和目的地区域报价质量使用相同类型；不将旧未知类型历史当作餐饮或包裹证据。去重检查点仍每平台一个，没有新增逐单表。

同一报价短时补全类型时保持跟踪身份并修正原累计贡献；后续缺失类型不把已经确认的累计退回 UNKNOWN。手动选图仍只产生临时展示，不调用统计、定位或接单。

## 本轮审核边界

完成源码静态阅读、差异空白检查、调用引用核对和文件行数检查。未运行测试、编译、模拟器或真机操作；未声称实际设备已连接成功或真实图片已通过 OCR。其他 Uber 卡片变体、确认页及配送完成页仍需各自页面证据，不把这两张报价截图当作完整配送闭环的验证。
