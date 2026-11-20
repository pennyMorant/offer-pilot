package com.overud.model

/** Ephemeral background notification preview; never persisted and never authorizes auto-accept. */
data class BackgroundOfferAlert(
    val platform: Platform,
    val amountPreview: String?,
    val snippet: String,
    val receivedAt: Long,
)

object NotificationAlertParser {
    private val money = Regex("(?:US\\$|\\$)\\s*\\d{1,4}(?:,\\d{3})*(?:\\.\\d{2})?")
    private val ignore = Regex(
        "you(?:'re| are) online|running in background|using your location|battery|foreground|正在后台运行|定位服务",
        RegexOption.IGNORE_CASE,
    )
    private val offerHint = Regex(
        "new (?:order|delivery|offer|request|trip)|opportunity|accept|deliver|pickup|新订单|新配送|接单|\\$",
        RegexOption.IGNORE_CASE,
    )

    fun parse(platform: Platform, rawText: String, now: Long): BackgroundOfferAlert? {
        val clean = rawText.lines().map(String::trim).filter(String::isNotBlank).joinToString(" · ")
        if (clean.isBlank() || ignore.containsMatchIn(clean)) return null
        val amount = money.find(clean)?.value?.replace(" ", "")
        if (amount == null && !offerHint.containsMatchIn(clean)) return null
        return BackgroundOfferAlert(platform, amount, clean.take(64), now)
    }
}
