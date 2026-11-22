package io.github.pennymorant.offerpilot.ui.rules

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pennymorant.offerpilot.ui.components.ToggleRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun VoiceSettingsCard(viewModel: VoiceViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleRow("驾驶语音播报", "开始工作后播报新报价与关键配送状态；开关立即保存", settings.enabled,
            onChange = viewModel::enabled)
        ToggleRow("播报 Prop 22 情景补差", "只在已启用估算且字段足够时播报，不代表实际到账",
            settings.includeProp22, onChange = viewModel::prop22)
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()) {
            Text(status, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
        FilledTonalButton(onClick = viewModel::preview, shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("试听中文") }
        OutlinedButton(onClick = viewModel::stop, shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("停止当前播报") }
        TextButton(shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
            runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        }) { Text("系统文字转语音设置") }
        Text("需要系统离线中文语音，音量与输出设备遵循系统设置。不会强制打开扬声器或改变音量；" +
            "图片测试不自动播报，试听不接单。", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
