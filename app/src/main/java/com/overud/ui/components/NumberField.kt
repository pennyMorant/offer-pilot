@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.overud.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable fun NumberField(label: String, value: String, unit: String, onChange: (String) -> Unit, enabled: Boolean = true, error: String? = null) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, suffix = { Text(unit) },
        isError = error != null, supportingText = if (error != null) { { Text(error) } } else null,
        enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
}
