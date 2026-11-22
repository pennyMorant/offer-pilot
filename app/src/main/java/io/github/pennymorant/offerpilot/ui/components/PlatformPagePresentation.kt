package io.github.pennymorant.offerpilot.ui.components

import io.github.pennymorant.offerpilot.model.*

fun platformPagePresentation(page: PlatformPage?): String? {
    page ?: return null
    val label = when (page.type) {
        PlatformPageType.UNKNOWN -> "当前页面尚未识别"
        PlatformPageType.OFFER -> "新报价"
        PlatformPageType.ADDON_OFFER -> "追加单报价 · 当前保留手动接单"
        PlatformPageType.ACCEPT_CONFIRMATION -> "接单确认页 · 当前保留手动确认"
        PlatformPageType.ACCEPTED -> "已接受订单"
        PlatformPageType.PICKUP -> "取餐阶段"
        PlatformPageType.DELIVERY -> "送达阶段"
        PlatformPageType.COMPLETED -> "配送完成页"
        PlatformPageType.CANCELLED -> "取消状态页 · 请核对配送占用"
        PlatformPageType.OFFLINE -> "离线或开始工作页面"
        PlatformPageType.HISTORY -> "历史页面 · 不解析为新报价"
        PlatformPageType.AMBIGUOUS -> "页面信息冲突 · 本帧不接单"
    }
    val source = if (page.source == OfferSource.OCR) " · 截图识别，仅供参考" else ""
    return "${page.platform.label} · $label$source"
}
