package com.overud.bridge;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
interface IPrivilegedService {
    Bundle snapshot() = 0;
    ParcelFileDescriptor screenshot(String expectedPackage) = 1;
    String accept(String expectedToken, String expectedPackage) = 2;
    void destroy() = 16777114;
}
