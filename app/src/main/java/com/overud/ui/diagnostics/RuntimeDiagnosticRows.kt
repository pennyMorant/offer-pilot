package com.overud.ui.diagnostics

import com.overud.bridge.BridgeStatus
import com.overud.device.DeviceStatus
import com.overud.work.WorkState

fun runtimeDiagnosticRows(work: WorkState, bridge: BridgeStatus, device: DeviceStatus): List<RuntimeDiagnosticRow> {
    fun flag(value: Boolean?, yes: String, no: String) = when (value) { true -> yes; false -> no; null -> "待检查" }
    fun tone(value: Boolean?) = when (value) {
        true -> DiagnosticTone.READY
        false -> DiagnosticTone.ATTENTION
        null -> DiagnosticTone.UNKNOWN
    }
    val location = when {
        device.fineLocation == true || device.coarseLocation == true -> true
        device.fineLocation == false && device.coarseLocation == false -> false
        else -> null
    }
    return listOf(
        RuntimeDiagnosticRow("Shevery 服务", if (bridge.connected) "服务在线" else "服务未连接",
            bridge.message, DiagnosticGroup.CONNECTION, tone(bridge.connected),
            if (bridge.connected) null else DiagnosticAction.SHEVERY),
        RuntimeDiagnosticRow("应用授权", if (bridge.authorized) "已授权" else "等待授权",
            "允许 OfferPilot 读取订单页面并执行接单动作。授权不会自动开启工作或自动接单。",
            DiagnosticGroup.CONNECTION, tone(bridge.authorized), if (bridge.authorized) null else DiagnosticAction.SHEVERY),
        RuntimeDiagnosticRow("页面读取", when {
            bridge.serviceReady -> "读取连接就绪"
            !work.working -> "开始工作后检查"
            else -> "读取连接未就绪"
        }, "开始工作时建立采集连接；连接就绪不代表所有订单页面均可识别。", DiagnosticGroup.CONNECTION,
            if (bridge.serviceReady) DiagnosticTone.READY else if (work.working) DiagnosticTone.ATTENTION else DiagnosticTone.INFO),
        RuntimeDiagnosticRow("应用通知", flag(device.notifications, "已允许", "未开启"),
            "显示工作通知、订单提醒和快捷关闭语音按钮。", DiagnosticGroup.PERMISSIONS,
            tone(device.notifications), DiagnosticAction.NOTIFICATIONS),
        RuntimeDiagnosticRow("工作通知渠道", flag(device.workChannel, "可用 / 待创建", "已关闭"),
            "首次开始工作时创建通知渠道；应用通知总开关也需开启。", DiagnosticGroup.PERMISSIONS,
            if (device.workChannel == true) DiagnosticTone.INFO else tone(device.workChannel), DiagnosticAction.NOTIFICATIONS),
        RuntimeDiagnosticRow("通知读取", flag(device.notificationListener, "已授权", "未授权"),
            "通过平台订单通知辅助触发识别，与应用通知显示权限相互独立。", DiagnosticGroup.PERMISSIONS,
            tone(device.notificationListener), DiagnosticAction.LISTENER),
        RuntimeDiagnosticRow("订单分析悬浮窗", flag(device.overlay, "已授权", "未授权"),
            "在平台页面上查看分析卡片；未开启仍可在应用内查看。", DiagnosticGroup.PERMISSIONS,
            tone(device.overlay), DiagnosticAction.OVERLAY),
        RuntimeDiagnosticRow("定位权限", when {
            device.fineLocation == true -> "精确位置"
            device.coarseLocation == true -> "大致位置"
            location == false -> "未授权"
            else -> "待检查"
        }, "用于区域统计。大致位置也可使用；更改权限后请重新开始工作。", DiagnosticGroup.PERMISSIONS,
            tone(location), DiagnosticAction.LOCATION),
        RuntimeDiagnosticRow("系统定位", flag(device.locationEnabled, "已开启", "已关闭"),
            "区域分析需要同时开启手机定位和应用定位权限。", DiagnosticGroup.PERMISSIONS,
            tone(device.locationEnabled), DiagnosticAction.LOCATION_SETTINGS),
        RuntimeDiagnosticRow("电池优化", flag(device.batteryExempt, "已豁免", "使用系统优化"),
            "如后台采集被中断，可检查省电设置。豁免不能保证系统永不清理进程。", DiagnosticGroup.BACKGROUND,
            if (device.batteryExempt == false) DiagnosticTone.INFO else tone(device.batteryExempt), DiagnosticAction.BATTERY),
        RuntimeDiagnosticRow("后台限制", flag(device.backgroundRestricted, "已限制", "未报告限制"),
            "厂商的自启动、省电与后台锁定设置需在系统中单独确认。", DiagnosticGroup.BACKGROUND,
            tone(device.backgroundRestricted?.not()), DiagnosticAction.APP_SETTINGS),
        RuntimeDiagnosticRow("工作状态", when {
            !work.working -> "未开始工作"
            work.diagnosticActive -> "手动节点诊断中"
            work.pending -> "配送占用 / 待核对"
            work.automatic -> "自动接单开启"
            else -> "工作中 · 手动接单"
        }, work.message, DiagnosticGroup.RUNTIME, DiagnosticTone.INFO),
        RuntimeDiagnosticRow("最近一次平台采集", work.lastCaptureAt?.let {
            java.text.DateFormat.getTimeInstance().format(java.util.Date(it))
        } ?: "本次运行暂无记录", "仅表示读取到支持平台的时间，不代表订单识别或接单成功。",
            DiagnosticGroup.RUNTIME, DiagnosticTone.INFO),
        RuntimeDiagnosticRow("屏幕", flag(device.interactive, "已亮屏", "已熄屏"),
            "熄屏期间不执行自动接单。", DiagnosticGroup.RUNTIME, tone(device.interactive)),
        RuntimeDiagnosticRow("锁屏", flag(device.locked, "已锁定", "未锁定"),
            "解锁不会自动重新开启接单。", DiagnosticGroup.RUNTIME, tone(device.locked?.not())),
    )
}
