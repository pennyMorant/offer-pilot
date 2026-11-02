package com.overud.platform

import java.util.Locale

enum class DeliveryScreenState { UNKNOWN, ACCEPTED, ACTIVE, COMPLETED }

object DeliveryScreenParser {
    private val active = setOf("confirm pickup", "confirm pick up", "complete delivery", "confirm delivery",
        "confirm dropoff", "confirm drop-off", "slide to complete delivery", "确认取餐", "确认送达", "完成配送")
    private val completed = setOf("delivery complete", "delivery completed", "order delivered",
        "订单已送达", "配送完成", "送达成功")
    private val accepted = setOf("order accepted", "delivery accepted", "订单已接受", "接单成功")
    private val history = setOf("trip history", "order history", "delivery history", "past deliveries",
        "earnings activity", "历史订单", "配送历史", "行程历史")
    private val quoteAction = Regex("^(accept|match|接受|接单)(\\b|\\s|$)", RegexOption.IGNORE_CASE)

    fun parse(text: String): DeliveryScreenState {
        val lines = text.lines().map { it.trim().trimEnd('!', '.', '。', '！').lowercase(Locale.ROOT) }.toSet()
        if (lines.any { it in history }) return DeliveryScreenState.UNKNOWN
        if (lines.any(quoteAction::containsMatchIn) && Regex("(?:US\\$|\\$)\\s*\\d").containsMatchIn(text)) {
            return DeliveryScreenState.UNKNOWN
        }
        val delivering = lines.any { it in active }
        val delivered = lines.any { it in completed }
        return when {
            delivering && delivered -> DeliveryScreenState.UNKNOWN
            delivering -> DeliveryScreenState.ACTIVE
            delivered -> DeliveryScreenState.COMPLETED
            lines.any { it in accepted } -> DeliveryScreenState.ACCEPTED
            else -> DeliveryScreenState.UNKNOWN
        }
    }
}
