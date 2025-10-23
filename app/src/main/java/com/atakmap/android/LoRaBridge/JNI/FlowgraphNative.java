package com.atakmap.android.LoRaBridge.JNI;

public class FlowgraphNative {

    public static native int run_flowgraph_with_fd(int fd);

    public static native void run_flowgraph();

    public static native void shutdown();

}
