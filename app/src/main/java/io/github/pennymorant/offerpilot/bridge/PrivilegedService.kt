package io.github.pennymorant.offerpilot.bridge

import android.os.Bundle
import android.os.ParcelFileDescriptor

/** Minimal AIDL entry point running under the Shevery/Shizuku identity. */
class PrivilegedService : IPrivilegedService.Stub() {
    private val connection = UiAutomationConnection()
    private val windows = WindowReader(connection)
    private val screenshots = ScreenshotReader(connection, windows)
    private val acceptance = AcceptTargetResolver(connection, windows)

    override fun snapshot(): Bundle = windows.snapshot()
    override fun diagnosticSnapshot(): Bundle = windows.snapshot(includeNodes = true)
    override fun registerWindowEvents(listener: IWindowEvents?) = connection.changes.register(listener)
    override fun screenshot(expectedFrame: Bundle): ParcelFileDescriptor? = screenshots.screenshot(expectedFrame)
    override fun accept(expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String =
        acceptance.accept(expectedToken, expectedPackage, deadlineElapsed)
    override fun confirmAcceptance(originalToken: String, expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String =
        acceptance.confirm(originalToken, expectedToken, expectedPackage, deadlineElapsed)
    override fun launchPlatform(packageName: String): Boolean {
        val platform = io.github.pennymorant.offerpilot.model.Platform.fromPackage(packageName) ?: return false
        return runCatching {
            connection.get().executeShellCommand("monkey -p ${platform.packageName} -c android.intent.category.LAUNCHER 1")?.close()
            true
        }.getOrDefault(false)
    }
    override fun decline(expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String =
        acceptance.decline(expectedToken, expectedPackage, deadlineElapsed)
    override fun destroy() {
        connection.close()
        kotlin.system.exitProcess(0)
    }
}
