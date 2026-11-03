package com.overud.model

/** Service type is independent of platform and must not be inferred from map labels. */
enum class OfferService(val label: String) {
    UNKNOWN("类型未确认"), DELIVERY("餐饮配送"), PACKAGE("包裹配送"),
}
