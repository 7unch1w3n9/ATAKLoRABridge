
package com.atakmap.android.LoRaBridge.phy;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atakmap.android.LoRaBridge.ChatMessage.MessageSyncService;


public class UdpManager {
    private static final String TAG = "UdpManager";
    private DatagramSocket socket;
    private Thread receiveThread;
    private boolean running = false;
    private final MessageSyncService syncService;
    //private final MessageConverter converter;
    private final int RX_PORT = 1383;
    private final int TX_port = 1382;


    private final ExecutorService processingExecutor = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    public UdpManager(MessageSyncService syncService, MessageConverter converter) {
        this.syncService = syncService;
        //this.converter = converter;
    }

    public void start() {
        try {
            socket = new DatagramSocket(RX_PORT);
            running = true;
            receiveThread = new Thread(this::receiveLoop);
            receiveThread.start();
        } catch (IOException e) {
            Log.e(TAG, "UDP启动失败", e);
        }
    }
    private void receiveLoop() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                Log.d(TAG, "📦 Payload (UTF-8): " + new String(data));
                processingExecutor.execute(() -> {
                    try {
                        syncService.handleFlowgraphMessage(data);
                    } catch (Exception e) {
                        Log.e(TAG, "处理LoRa消息失败", e);
                    }
                });
            } catch (IOException e) {
                if (running) Log.e(TAG, "UDP接收错误", e);
            }
        }
    }
    public void sendAsync(byte[] payload) {
        com.atakmap.coremap.log.Log.d(TAG, "Message sent to Flowgraph: " + Arrays.toString(payload));
        processingExecutor.execute(() -> send(payload));
    }
    private void send(byte[] payload) {
        if (socket == null) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                Log.e(TAG, "Socket 初始化失败", e);
                return; // 不能发
            }
        }

        try {
            InetAddress localAddr = InetAddress.getByName("127.0.0.1");
            DatagramPacket localPacket = new DatagramPacket(payload, payload.length, localAddr, TX_port);
            socket.send(localPacket);
            Log.d(TAG, "📤 已发送给本地 Rust Flowgraph，长度: " + payload.length);

            // ✅ 再发给电脑（抓包用）
            InetAddress pcAddr = InetAddress.getByName("192.168.0.213"); // ⚠️
            DatagramPacket pcPacket = new DatagramPacket(payload, payload.length, pcAddr, TX_port);
            socket.send(pcPacket);
            Log.d(TAG, "📤 已发送给电脑: " + pcAddr.getHostAddress() + ":" + TX_port);
        } catch (Exception e) {
            Log.e(TAG, "UDP 发送失败", e);
        }
    }

/*
    public void sendToWasm(ChatMessageEntity message) {
        if (!running) {
            Log.w(TAG, "Cannot send - UDP manager not running");
            return;
        }

        try {
            // 使用转换器编码消息
            byte[] payload = converter.encodeMessage(message);

            if (payload == null || payload.length == 0) {
                Log.w(TAG, "Empty payload for message: " + message.getId());
                return;
            }

            InetAddress address = InetAddress.getByName("127.0.0.1");
            DatagramPacket packet = new DatagramPacket(payload, payload.length, address, TX_PORT);
            socket.send(packet);

            Log.d(TAG, "Sent " + payload.length + " bytes to WASM for message: " + message.getId());
        } catch (Exception e) {
            Log.e(TAG, "UDP send error for message: " + message.getId(), e);
        }
    }

    private void listenForIncoming() {
        byte[] buffer = new byte[4096];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // 复制实际接收到的数据
                byte[] received = Arrays.copyOf(packet.getData(), packet.getLength());
                Log.d(TAG, "Received " + received.length + " bytes from WASM");

                // 处理接收到的LoRa消息
                handleLoRaMessage(received);
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "UDP receive error", e);
                }
            }
        }
    }

    private void handleLoRaMessage(byte[] raw) {
        try {
            Log.d(TAG, "Processing payload (" + raw.length + " bytes)");

            // 使用转换器解码消息
            ChatMessageEntity message = converter.decodeMessage(raw);

            if (message == null) {
                Log.w(TAG, "Converter returned null for payload");
                return;
            }

            // 标记为物理层来源
            message.setOrigin("PHY");

            // 将消息实体传递给同步服务
            syncService.processIncomingPhyMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert payload to message", e);
        }
    }
*/

    public void stop() {
        running = false;
        if (socket != null) socket.close();
        if (receiveThread != null) {
            receiveThread.interrupt();
            try {
                receiveThread.join(500);
            } catch (InterruptedException e) {
                Log.w(TAG, "等待接收线程退出失败", e);
            }
        }

        processingExecutor.shutdownNow();
    }
}
