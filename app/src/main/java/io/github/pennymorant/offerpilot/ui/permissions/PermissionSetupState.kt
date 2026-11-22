package io.github.pennymorant.offerpilot.ui.permissions

enum class SetupPermission(val title: String, val explanation: String, val impact: String, val action: String) {
    NOTIFICATIONS("工作通知", "显示工作状态、订单提醒和停止工作入口。", "未开启时可能错过订单提醒。", "允许通知"),
    LOCATION("区域定位", "工作期间使用位置分析当前区域的历史订单质量，大致位置也可使用。", "未开启时，当前区域分析会受到限制。", "允许位置访问"),
    OVERLAY("订单分析悬浮窗", "在 Uber 和 DoorDash 上方直接查看订单评分与收益分析。", "未开启时，需要返回 OfferPilot 查看分析。", "去开启悬浮窗"),
    LISTENER("订单通知读取", "通过 Uber 和 DoorDash 的通知及时触发订单识别。", "未开启时，通知辅助识别不可用。", "去开启通知使用权"),
    SHEVERY("Shevery 页面读取", "读取订单页面，并执行你明确开启的自动接单操作。", "需要安装并启动 Shevery 服务；授权本身不会开启自动接单。", "连接并授权"),
}

enum class SetupStage { WELCOME, PERMISSION, SUMMARY }

data class PermissionSetupState(
    val visible: Boolean = true,
    val stage: SetupStage = SetupStage.WELCOME,
    val current: SetupPermission? = null,
    val granted: Set<SetupPermission> = emptySet(),
    val busy: Boolean = false,
    val message: String? = null,
)
