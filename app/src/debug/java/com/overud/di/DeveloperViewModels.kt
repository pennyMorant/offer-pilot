package com.overud.di

import androidx.lifecycle.ViewModel
import com.overud.ui.imagetest.ImageTestViewModel
import com.overud.ui.diagnostics.NodeDiagnosticViewModel
import com.overud.capture.ImportedImageRecognizer
import com.overud.bridge.NodeDiagnosticReader

internal fun developerViewModel(type: Class<*>, container: AppContainer): ViewModel? = when (type) {
    ImageTestViewModel::class.java -> ImageTestViewModel(ImportedImageRecognizer(container.appContext), container.session)
    NodeDiagnosticViewModel::class.java -> NodeDiagnosticViewModel(container.session, NodeDiagnosticReader(container.bridge))
    com.overud.ui.rules.RulePreviewViewModel::class.java -> com.overud.ui.rules.RulePreviewViewModel(container.session)
    else -> null
}
