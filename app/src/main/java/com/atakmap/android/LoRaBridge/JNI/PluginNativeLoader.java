package com.atakmap.android.LoRaBridge.JNI;

import android.content.Context;
import android.system.Os;
import android.system.ErrnoException;
import android.util.Log;

public final class PluginNativeLoader {
    private static final String TAG = "PluginNativeLoader";
    private static boolean sLoaded = false;

    public static synchronized void init(Context ctx) {
        if (sLoaded) return;

        try { Os.setenv("SOAPY_SDR_PLUGIN_PATH", ctx.getApplicationInfo().nativeLibraryDir, true); }
        catch (ErrnoException ignore) {}
        try { Os.setenv("SOAPY_SDR_LOG_LEVEL", "DEBUG", true); }
        catch (ErrnoException ignore) {}
        /*
        try { Os.setenv("LIBUSB_DEBUG", "1", true); }
        catch (ErrnoException ignore) {}

         */

        // 集中加载一次（如已加载则忽略“already opened”）
        safeLoad("usb1.0");
        safeLoad("hackrf");
        safeLoad("SoapySDR");
        safeLoad("HackRFSupport");
        safeLoad("atak");   // 你的 Rust JNI 库

        sLoaded = true;
    }

    private static void safeLoad(String lib) {
        try {
            System.loadLibrary(lib);
        } catch (UnsatisfiedLinkError e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("already opened by ClassLoader")) {
                Log.w(TAG, lib + " already loaded, ignore");
            } else {
                throw e;
            }
        }
    }
}


