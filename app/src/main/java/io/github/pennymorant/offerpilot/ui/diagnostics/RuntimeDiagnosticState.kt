package io.github.pennymorant.offerpilot.ui.diagnostics

enum class DiagnosticAction { SHEVERY, NOTIFICATIONS, LISTENER, OVERLAY, LOCATION, LOCATION_SETTINGS, BATTERY, APP_SETTINGS }

enum class DiagnosticGroup(val title: String, val description: String) {
    CONNECTION("Shevery 连接", "页面读取与接单动作所需的连接"),
    PERMISSIONS("应用权限", "通知、悬浮分析与区域定位"),
    BACKGROUND("后台运行", "系统省电与后台限制"),
    RUNTIME("当前运行状态", "工作、屏幕与最近采集情况"),
}

enum class DiagnosticTone { READY, ATTENTION, INFO, UNKNOWN }

data class RuntimeDiagnosticRow(
    val title: String,
    val status: String,
    val detail: String,
    val group: DiagnosticGroup,
    val tone: DiagnosticTone,
    val action: DiagnosticAction? = null,
)
