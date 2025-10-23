package com.atakmap.android.LoRaBridge.JNI;
public final class UsbHackrfManagerHolder {
    private static volatile UsbHackrfManager INSTANCE;
    private UsbHackrfManagerHolder() {}
    public static void set(UsbHackrfManager m) { INSTANCE = m; }
    public static UsbHackrfManager get() { return INSTANCE; }
}