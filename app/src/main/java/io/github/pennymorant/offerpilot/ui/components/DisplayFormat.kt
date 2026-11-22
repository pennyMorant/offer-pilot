@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.pennymorant.offerpilot.ui.components

import java.util.Locale

fun money(cents: Long) = "$${"%.2f".format(Locale.US, cents / 100.0)}"
