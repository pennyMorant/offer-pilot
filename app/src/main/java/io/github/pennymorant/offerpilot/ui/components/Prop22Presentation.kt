package io.github.pennymorant.offerpilot.ui.components

import io.github.pennymorant.offerpilot.model.Prop22Estimate
import io.github.pennymorant.offerpilot.model.Prop22Status
import kotlin.math.roundToLong

fun prop22Money(cents: Double?): String = cents?.let { money(it.roundToLong()) } ?: "—"

fun prop22Issue(estimate: Prop22Estimate): String? = when (estimate.status) {
    Prop22Status.ESTIMATED -> null
    Prop22Status.INVALID_SETTINGS -> "Prop 22 设置无效，请重新保存"
    Prop22Status.RATE_YEAR_UNSUPPORTED -> "当前仅配置 2026 费率，其他年份暂不估算"
    Prop22Status.UNSUPPORTED_OFFER -> "非 USD、追加单或叠单暂不估算，避免重复计算有效时间"
    Prop22Status.MISSING_ROUTE -> "缺少可信总耗时或机动车总里程，无法估算"
    Prop22Status.MISSING_AMOUNT -> "缺少有效报价，仅显示保底参考，不能估算额外补差"
}

fun prop22Compact(estimate: Prop22Estimate?): String {
    estimate ?: return ""
    val issue = prop22Issue(estimate)
    return if (issue != null) "Prop 22：$issue\n"
    else "Prop 22 情景补差 +${prop22Money(estimate.extraCents)}\n" +
        "假设小费 ${estimate.assumedTipPercent}%，周期实际补差可能为 0\n"
}
