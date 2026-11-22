package io.github.pennymorant.offerpilot.bridge;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import io.github.pennymorant.offerpilot.bridge.IWindowEvents;
interface IPrivilegedService {
    Bundle snapshot() = 0;
    ParcelFileDescriptor screenshot(in Bundle expectedFrame) = 1;
    String accept(String expectedToken, String expectedPackage, long deadlineElapsed) = 2;
    String confirmAcceptance(String originalToken, String expectedToken, String expectedPackage, long deadlineElapsed) = 3;
    void registerWindowEvents(IWindowEvents listener) = 4;
    Bundle diagnosticSnapshot() = 5;
    boolean launchPlatform(String packageName) = 6;
    String decline(String expectedToken, String expectedPackage, long deadlineElapsed) = 7;
    void destroy() = 16777114;
}
