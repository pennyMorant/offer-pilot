package com.overud.platform

import com.overud.model.*

/** Explicit label vocabulary, not a claim of coverage for every Uber Driver release. */
object UberEatsPages {
    val rules = PlatformPageRules(
        quoteActions = setOf("accept", "match", "接受", "接单"),
        confirmation = setOf("confirm acceptance", "confirm delivery acceptance", "确认接单", "确认接受订单"),
        accepted = setOf("delivery accepted", "order accepted", "接单成功", "订单已接受"),
        pickup = setOf("confirm pickup", "confirm pick up", "确认取餐"),
        delivery = setOf("complete delivery", "confirm delivery", "confirm dropoff", "confirm drop-off",
            "slide to complete delivery", "确认送达", "完成配送"),
        completed = setOf("delivery complete", "delivery completed", "order delivered", "配送完成", "订单已送达", "送达成功"),
        cancelled = setOf("delivery cancelled", "delivery canceled", "order cancelled", "order canceled", "订单已取消"),
        offline = setOf("you're offline", "you are offline", "go online", "当前离线", "您已离线"),
        history = setOf("order history", "历史订单", "trip history", "delivery history", "past deliveries", "earnings activity", "行程历史", "配送历史"),
        addon = setOf("add a delivery", "add-on delivery", "追加配送", "追加订单"),
    )

    fun classify(text: String, source: OfferSource = OfferSource.UI_NODES) =
        PageClassifier.classify(Platform.UBER_EATS, text, rules, source)
}
