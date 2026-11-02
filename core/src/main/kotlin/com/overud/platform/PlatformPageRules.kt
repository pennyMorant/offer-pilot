package com.overud.platform

/** Exact normalized labels, separately maintained by each platform adapter. */
data class PlatformPageRules(
    val quoteActions: Set<String>,
    val confirmation: Set<String>,
    val accepted: Set<String>,
    val pickup: Set<String>,
    val delivery: Set<String>,
    val completed: Set<String>,
    val cancelled: Set<String>,
    val offline: Set<String>,
    val history: Set<String>,
    val addon: Set<String>,
)
