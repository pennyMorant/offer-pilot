package io.github.pennymorant.offerpilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RegionPicker(regions: List<String>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = regions.filter { regionLabel(it).contains(query, ignoreCase = true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择地区") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it },
                singleLine = true, label = { Text("搜索网格") })
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                item { TextButton(onClick = { onSelect(null) }) { Text("全部地区") } }
                items(matches, key = { it }) { region ->
                    TextButton(onClick = { onSelect(region) }) { Text(regionLabel(region)) }
                }
                if (matches.isEmpty()) item { Text("没有匹配的地区") }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
