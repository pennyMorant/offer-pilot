package com.overud.ui.components

import androidx.compose.runtime.Composable

@Composable
fun QualityStandardNote() {
    EmptyStateCard("历史质量标准 v2",
        "优质：每英里报价 ≥ $2 且预估毛时薪 ≥ $25。" +
        "低价值：每英里报价 < $1 或毛时薪 < $15。" +
        "其他完整样本归为普通，字段不足单列。标准不随自动接单开关改变。")
}
