package com.overud.ui.components

import androidx.compose.runtime.Composable

@Composable
fun QualityStandardNote() {
    EmptyStateCard("质量标准 v1",
        "优质：报价 ≥ $8、每英里 ≥ $2、预估毛时薪 ≥ $25，同时满足。" +
        "低价值：报价 < $5、每英里 < $1、毛时薪 < $15，任一满足。" +
        "其他完整样本归为普通，字段不足单列。标准不随自动接单开关改变。")
}
