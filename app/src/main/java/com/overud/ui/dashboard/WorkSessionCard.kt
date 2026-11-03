@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkSessionCard(state: DashboardUiState, onWork: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (state.working) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "work",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.working) "工作进行中" else "尚未开始工作",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(state.message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onWork,
                enabled = state.working || state.connection.authorized,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(if (state.working) "结束工作" else "开始工作")
            }
        }
    }
}
