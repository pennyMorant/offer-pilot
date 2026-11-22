package io.github.pennymorant.offerpilot.di

import androidx.lifecycle.ViewModel
import io.github.pennymorant.offerpilot.ui.imagetest.ImageTestViewModel
import io.github.pennymorant.offerpilot.ui.diagnostics.NodeDiagnosticViewModel
import io.github.pennymorant.offerpilot.capture.ImportedImageRecognizer
import io.github.pennymorant.offerpilot.bridge.NodeDiagnosticReader

internal fun developerViewModel(type: Class<*>, container: AppContainer): ViewModel? = when (type) {
    ImageTestViewModel::class.java -> ImageTestViewModel(ImportedImageRecognizer(container.appContext), container.session)
    NodeDiagnosticViewModel::class.java -> NodeDiagnosticViewModel(container.session, NodeDiagnosticReader(container.bridge))
    io.github.pennymorant.offerpilot.ui.rules.RulePreviewViewModel::class.java -> io.github.pennymorant.offerpilot.ui.rules.RulePreviewViewModel(container.session)
    else -> null
}
