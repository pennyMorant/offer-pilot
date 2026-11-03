package com.overud.bridge

import android.os.Bundle
import android.os.ParcelFileDescriptor

/** Minimal AIDL entry point running under the Shevery/Shizuku identity. */
class PrivilegedService : IPrivilegedService.Stub() {
    private val connection = UiAutomationConnection()
    private val windows = WindowReader(connection)
    private val screenshots = ScreenshotReader(connection, windows)
    private val acceptance = AcceptTargetResolver(connection, windows)

    override fun snapshot(): Bundle = windows.snapshot()
    override fun registerWindowEvents(listener: IWindowEvents?) = connection.changes.register(listener)
    override fun screenshot(expectedFrame: Bundle): ParcelFileDescriptor? = screenshots.screenshot(expectedFrame)
    override fun accept(expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String =
        acceptance.accept(expectedToken, expectedPackage, deadlineElapsed)
    override fun confirmAcceptance(originalToken: String, expectedToken: String, expectedPackage: String, deadlineElapsed: Long): String =
        acceptance.confirm(originalToken, expectedToken, expectedPackage, deadlineElapsed)
    override fun destroy() {
        connection.close()
        kotlin.system.exitProcess(0)
    }
}
