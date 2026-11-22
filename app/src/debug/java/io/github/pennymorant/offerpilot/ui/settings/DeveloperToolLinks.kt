package io.github.pennymorant.offerpilot.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperToolLinks(onSelect: (String) -> Unit) {
    listOf("image" to "订单图片识别", "nodes" to "临时节点诊断", "rules" to "文本规则调试").forEach { (key, title) ->
        TextButton(
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = { onSelect(key) },
        ) { Text(title) }
    }
}
