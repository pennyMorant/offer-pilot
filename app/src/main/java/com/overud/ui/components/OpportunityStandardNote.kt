package com.overud.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun OpportunityStandardNote() {
    QualityStandardNote()
    Text(
        "区域机会指数只在同一平台和区域累计至少 1 小时观察、10 个可分类报价、" +
            "10 个里程效率样本和 10 个时间效率样本后计算。权重为：历史优质比例 45%、" +
            "每观察小时报价量 30%、毛报价每英里与时薪效率 25%。每小时 3 张、$2/mi 和 $25/h " +
            "分别达到对应项目满分。订单类型筛选使用该类型报价数，但观察时长是平台在线观察时间。",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        "指数只用于比较个人历史观察区域，不包含前往区域的成本、实时供需或下一单概率。",
        style = MaterialTheme.typography.bodySmall,
    )
}
