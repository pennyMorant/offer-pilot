package com.overud.ui.diagnostics

import com.overud.bridge.BridgeStatus
import com.overud.device.DeviceStatus
import com.overud.work.WorkState

enum class DiagnosticAction { SHEVERY, NOTIFICATIONS, LISTENER, OVERLAY, LOCATION, LOCATION_SETTINGS, BATTERY, APP_SETTINGS }
data class RuntimeDiagnosticRow(val title: String, val status: String, val detail: String, val action: DiagnosticAction? = null)

fun runtimeDiagnosticRows(work: WorkState, bridge: BridgeStatus, device: DeviceStatus): List<RuntimeDiagnosticRow> {
    fun flag(value: Boolean?, yes: String, no: String) = when (value) { true -> yes; false -> no; null -> "尚未读取或无法读取" }
    return listOf(
        RuntimeDiagnosticRow("Shevery 服务", if (bridge.connected) "已连接" else "未连接",
            "需先在手机启动 Shevery。${bridge.message}", DiagnosticAction.SHEVERY),
        RuntimeDiagnosticRow("Shevery 授权", if (bridge.authorized) "已授权" else "未授权",
            "用于节点读取与接单动作；授权仍由系统确认。", DiagnosticAction.SHEVERY),
        RuntimeDiagnosticRow("特权采集进程", if (bridge.serviceReady) "已绑定" else "未绑定",
            "开始工作后连接；绑定成功不代表平台页面已适配。"),
        RuntimeDiagnosticRow("工作状态", when {
            !work.working -> "未开始工作"
            work.diagnosticActive -> "手动节点诊断中"
            work.pending -> "配送占用或结果待核对"
            work.automatic -> "自动接单开启"
            else -> "工作中，自动接单关闭"
        }, work.message),
        RuntimeDiagnosticRow("本进程最近一次平台采集", work.lastCaptureAt?.let {
            java.text.DateFormat.getTimeInstance().format(java.util.Date(it))
        } ?: "尚无记录", "这是最近读取到支持平台的时间，不代表当前报价已成功识别或接单。"),
        RuntimeDiagnosticRow("屏幕", flag(device.interactive, "已亮屏", "熄屏"), "熄屏后不执行自动接单。"),
        RuntimeDiagnosticRow("锁屏", flag(device.locked, "已锁定", "未锁定"), "解锁不会自动重新开启接单。"),
        RuntimeDiagnosticRow("应用通知", flag(device.notifications, "允许显示", "未允许显示"),
            "控制工作通知及快捷关闭语音按钮是否可见；不同于通知读取权限。", DiagnosticAction.NOTIFICATIONS),
        RuntimeDiagnosticRow("工作通知渠道", flag(device.workChannel, "未禁用或尚未创建", "已禁用"),
            "渠道在开始工作时创建；应用通知总开关也需允许。", DiagnosticAction.NOTIFICATIONS),
        RuntimeDiagnosticRow("通知读取", flag(device.notificationListener, "已授权", "未授权"),
            "辅助接收平台通知，不替代前台页面识别。", DiagnosticAction.LISTENER),
        RuntimeDiagnosticRow("悬浮窗", flag(device.overlay, "已授权", "未授权"),
            "影响平台页面上的分析卡片，不影响应用内查看。", DiagnosticAction.OVERLAY),
        RuntimeDiagnosticRow("定位权限", when {
            device.fineLocation == true -> "精确定位"
            device.coarseLocation == true -> "仅大致定位"
            device.fineLocation == false && device.coarseLocation == false -> "未授权"
            else -> "尚未读取或无法读取"
        }, "影响地区统计与位置精度；更改授权后重新开始工作。", DiagnosticAction.LOCATION),
        RuntimeDiagnosticRow("系统定位", flag(device.locationEnabled, "开启", "关闭"),
            "仅授予权限不代表系统定位已开启。", DiagnosticAction.LOCATION_SETTINGS),
        RuntimeDiagnosticRow("系统电池优化", flag(device.batteryExempt, "已豁免", "未豁免"),
            "只显示系统标准状态，不保证后台永不被清理。", DiagnosticAction.BATTERY),
        RuntimeDiagnosticRow("后台限制", flag(device.backgroundRestricted, "已限制", "未报告限制"),
            "厂商自启动、省电和后台锁定可能还需手动设置，当前无法统一检测。", DiagnosticAction.APP_SETTINGS),
    )
}
