package com.overud.bridge

import android.os.Bundle
import android.os.ParcelFileDescriptor

/** Minimal AIDL entry point running under the Shevery/Shizuku identity. */
class PrivilegedService : IPrivilegedService.Stub() {
    private val connection = UiAutomationConnection()
    private val windows = WindowReader(connection)
    private val screenshots = ScreenshotReader(connection)
    private val acceptance = AcceptTargetResolver(connection, windows)

    override fun snapshot(): Bundle = windows.snapshot()
    override fun screenshot(expectedPackage: String): ParcelFileDescriptor? = screenshots.screenshot(expectedPackage)
    override fun accept(expectedToken: String, expectedPackage: String): String = acceptance.accept(expectedToken, expectedPackage)
    override fun confirmAcceptance(originalToken: String, expectedToken: String, expectedPackage: String): String =
        acceptance.confirm(originalToken, expectedToken, expectedPackage)
    override fun destroy() {
        connection.close()
        kotlin.system.exitProcess(0)
    }
}
