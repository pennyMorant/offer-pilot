package io.github.pennymorant.offerpilot.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.pennymorant.offerpilot.ui.components.ToolSectionRow

@Composable
fun DeveloperToolLinks(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ToolSectionRow(
            icon = Icons.Rounded.ImageSearch,
            title = "订单图片识别",
            detail = "导入报价截图验证 OCR 与字段解析",
            index = 0,
            count = 3,
            onClick = { onSelect("image") },
        )
        ToolSectionRow(
            icon = Icons.Rounded.AccountTree,
            title = "临时节点诊断",
            detail = "抓取当前前台窗口节点与边界区域",
            index = 1,
            count = 3,
            onClick = { onSelect("nodes") },
        )
        ToolSectionRow(
            icon = Icons.Rounded.Code,
            title = "文本规则调试",
            detail = "输入模拟文本测试解析与规则评估",
            index = 2,
            count = 3,
            onClick = { onSelect("rules") },
        )
    }
}
