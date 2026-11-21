package com.overud.ui.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionSetupDialog(step: SetupPermission, onRequest: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(step.title) },
        text = { Text("${step.explanation}\n\n${step.ordinal + 1} / ${SetupPermission.entries.size} · 可稍后在设置中补齐权限。") },
        confirmButton = { TextButton(onClick = onRequest) { Text("继续授权") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("稍后设置") } },
    )
}
