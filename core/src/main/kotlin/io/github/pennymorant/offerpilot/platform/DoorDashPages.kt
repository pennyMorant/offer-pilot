package io.github.pennymorant.offerpilot.platform

import io.github.pennymorant.offerpilot.model.*

/** DoorDash labels remain separate from Uber's Match action and trip vocabulary. */
object DoorDashPages {
    val rules = PlatformPageRules(
        quoteActions = setOf("accept", "接受", "接单"),
        confirmation = setOf("confirm acceptance", "confirm order acceptance", "确认接单", "确认接受订单"),
        accepted = setOf("order accepted", "delivery accepted", "订单已接受", "接单成功"),
        pickup = setOf("confirm pickup", "confirm pick up", "确认取餐"),
        delivery = setOf("complete delivery", "complete delivery steps", "confirm delivery", "confirm drop-off",
            "confirm dropoff", "确认送达", "完成配送"),
        completed = setOf("delivery complete", "delivery completed", "order delivered", "订单已送达", "配送完成", "送达成功"),
        cancelled = setOf("order cancelled", "order canceled", "delivery cancelled", "delivery canceled", "订单已取消"),
        offline = setOf("dash now", "schedule a dash", "you're offline", "you are offline", "当前离线", "您已离线"),
        history = setOf("trip history", "行程历史", "order history", "delivery history", "past deliveries", "earnings activity", "历史订单", "配送历史"),
        addon = setOf("add to route", "add-on order", "add a delivery", "追加订单", "追加配送"),
    )

    fun classify(text: String, source: OfferSource = OfferSource.UI_NODES) =
        PageClassifier.classify(Platform.DOORDASH, text, rules, source)
}
