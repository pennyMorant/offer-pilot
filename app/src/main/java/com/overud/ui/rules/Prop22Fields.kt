package com.overud.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.overud.ui.components.NumberField
import com.overud.ui.components.ToggleRow

private val laWagePresets = listOf(
    "加州底薪" to "16.90",
    "LA City" to "17.87",
    "Santa Monica" to "17.81",
    "Pasadena" to "18.04",
    "West Hollywood" to "19.65",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable fun Prop22Fields(input: Prop22Input, saving: Boolean, onChange: (Prop22Input) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ToggleRow("启用加州 Prop 22 估算", "确认用于加州取餐；两个平台共用此设置，仅作参考",
            input.enabled, enabled = !saving) { onChange(input.copy(enabled = it)) }
        if (input.enabled) {
            Text("大洛杉矶常见城市时薪速选（可手动修改）", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                laWagePresets.forEach { (city, wage) ->
                    FilterChip(
                        selected = input.wage == wage,
                        enabled = !saving,
                        onClick = { onChange(input.copy(wage = wage)) },
                        label = { Text("$city $$wage") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            NumberField("取餐地适用最低时薪", input.wage, "USD/h",
                { onChange(input.copy(wage = it)) }, enabled = !saving)
            Text("2026 州标准为 $16.90；取餐城市标准更高时请修改。跨城市取餐需更新设置，不会自动匹配当地时薪。",
                style = MaterialTheme.typography.bodySmall)
            ToggleRow("使用机动车", "开启时按 2026 标准 $0.37/有效英里计算",
                input.motorVehicle, enabled = !saving) { onChange(input.copy(motorVehicle = it)) }
            NumberField("假设报价中的小费比例", input.tipPercent, "%",
                { onChange(input.copy(tipPercent = it)) }, enabled = !saving)
            Text("0% 表示将整笔报价当作平台收入计算差额，不代表实际没有小费。只能输入 0–100 的整数。",
                style = MaterialTheme.typography.bodySmall)
            Text("额外补差按单单情景估算；结算周期其他收入可能抵消差额，实际补差可能为 0。" +
                "不计入自动接单门槛或历史质量统计。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
