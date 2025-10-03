package com.atakmap.android.LoRaBridge.JNI;

public class FlowgraphNative {

    public static native int  run_flowgraph_with_fd(int fd);

    public static native void run_flowgraph();

    public static native void shutdown();
    // 初始化
    public native void initFlowgraph(String bindAddr, String sendToAddr);

    // 发送消息到 Rust
    public native void sendMessage(byte[] payload);

    public static native void initRustLogging();

    // 如果以后需要回调：
    public interface MessageCallback {
        void onMessageReceived(byte[] payload);
    }
    private static final java.util.concurrent.atomic.AtomicBoolean LOG_INIT =
            new java.util.concurrent.atomic.AtomicBoolean(false);

}
