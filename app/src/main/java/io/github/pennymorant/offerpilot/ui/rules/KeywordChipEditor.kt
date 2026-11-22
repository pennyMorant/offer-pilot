@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun KeywordChipEditor(
    title: String,
    placeholder: String,
    rawCsv: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    val tags = remember(rawCsv) {
        rawCsv.split(',', '，').map(String::trim).filter(String::isNotEmpty).distinct()
    }
    var adding by remember(title) { mutableStateOf(false) }
    var draft by remember(title) { mutableStateOf("") }

    fun commitDraft() {
        val cleaned = draft.trim().trim(',', '，')
        if (cleaned.isNotEmpty() && cleaned !in tags) {
            onChange((tags + cleaned).joinToString(", "))
        }
        draft = ""
        adding = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    enabled = enabled,
                    onClick = { onChange(tags.filterNot { it == tag }.joinToString(", ")) },
                    label = { Text(tag, style = MaterialTheme.typography.labelLarge) },
                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "移除", modifier = Modifier.size(16.dp)) },
                )
            }
            AssistChip(
                onClick = { adding = !adding },
                enabled = enabled,
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("添加", style = MaterialTheme.typography.labelLarge) },
            )
        }
        AnimatedVisibility(
            visible = adding,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitDraft() }),
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
                FilledTonalButton(onClick = ::commitDraft, enabled = enabled && draft.isNotBlank(),
                    shapes = ButtonDefaults.shapes()) { Text("加入") }
            }
        }
    }
}
