package com.atakmap.android.LoRaBridge.JNI;

public class FlowgraphNative {
    static {

        System.loadLibrary("atak");

    }

    public static native void run_flowgraph();

    // 初始化
    public native void initFlowgraph(String bindAddr, String sendToAddr);

    // 发送消息到 Rust
    public native void sendMessage(byte[] payload);

    // 设置接收回调

    // 关闭
    public static native void shutdown();

    // 回调接口
    public interface MessageCallback {
        void onMessageReceived(byte[] payload);
    }

    public static native void initRustLogging();
}
