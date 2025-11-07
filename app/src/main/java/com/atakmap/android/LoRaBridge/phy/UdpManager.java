package com.atakmap.android.LoRaBridge.phy;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * UdpManager (Singleton)
 * - 统一唯一的 UDP 通道
 * - 显式路由两类负载：
 *     Chat:     "LORA|..."
 *     Generic:  "LORA_COTX|..."
 * - 由 Lifecycle 负责 start/stop
 */
public class UdpManager {

    public interface ByteHandler { void accept(byte[] data); }

    public static final String HDR_CHAT = "LORA|";
    public static final String HDR_COT  = "LORA_COTX|";
    private static final String TAG = "UdpManager";
    private static volatile UdpManager INSTANCE;

    public static UdpManager getInstance() {
        if (INSTANCE == null) {
            synchronized (UdpManager.class) {
                if (INSTANCE == null) INSTANCE = new UdpManager();
            }
        }
        return INSTANCE;
    }

    private final int RX_PORT = 1383;
    private final int TX_PORT = 1382;

    private DatagramSocket socket;
    private Thread receiveThread;
    private volatile boolean running = false;

    private volatile ByteHandler chatHandler;
    private volatile ByteHandler cotHandler;

    private volatile String mirrorHost = "192.168.0.213";

    private final ExecutorService exec = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private UdpManager() {}

    public void setChatHandler(ByteHandler handler) { this.chatHandler = handler; }
    public void setCotHandler(ByteHandler handler)  { this.cotHandler = handler; }

    public synchronized void start() {
        if (running) return;
        try {
            socket = new DatagramSocket(RX_PORT);
            running = true;
            receiveThread = new Thread(this::rxLoop, "Udp-Rx");
            receiveThread.start();
            Log.d(TAG, "UDP started on " + RX_PORT);
        } catch (IOException e) {
            Log.e(TAG, "UDP start failed", e);
        }
    }

    public synchronized void stop() {
        running = false;
        if (socket != null) socket.close();
        if (receiveThread != null) {
            receiveThread.interrupt();
            try { receiveThread.join(500); } catch (InterruptedException ignored) {}
            receiveThread = null;
        }
        exec.shutdownNow();
        Log.d(TAG, "UDP stopped");
    }

    public void sendChat(byte[] body) {
        if (body == null) return;
        byte[] payload = withHeader(HDR_CHAT, body);
        sendAsync(payload);
    }

    public void sendCot(byte[] body) {
        if (body == null) return;
        byte[] payload = withHeader(HDR_COT, body);
        sendAsync(payload);
    }

    public void setMirrorHost(String hostOrNull) { this.mirrorHost = hostOrNull; }

    // ---------- 内部 ----------
    private void rxLoop() {
        byte[] buf = new byte[4096];
        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                byte[] data = Arrays.copyOf(p.getData(), p.getLength());
                exec.execute(() -> route(data));
            } catch (IOException e) {
                if (running) Log.e(TAG, "UDP recv error", e);
            }
        }
    }

    private void route(byte[] data) {
        try {
            String prefix = new String(data, 0, Math.min(data.length, 24), StandardCharsets.UTF_8);
            if (prefix.startsWith(HDR_COT)) {
                if (cotHandler != null) cotHandler.accept(data);
                else Log.w(TAG, "Cot payload but cotHandler == null, drop");
                return;
            }
            if (prefix.startsWith(HDR_CHAT)) {
                if (chatHandler != null) chatHandler.accept(stripHeader(data, HDR_CHAT.length()));
                else Log.w(TAG, "Chat payload but chatHandler == null, drop");
                return;
            }
            Log.w(TAG, "Unknown UDP payload head, drop. head=" + prefix);
        } catch (Throwable t) {
            Log.e(TAG, "route failed", t);
        }
    }

    private static byte[] stripHeader(byte[] data, int headerLen) {
        int n = Math.max(0, data.length - headerLen);
        byte[] out = new byte[n];
        System.arraycopy(data, headerLen, out, 0, n);
        return out;
    }

    private void sendAsync(byte[] payload) {
        exec.execute(() -> send(payload));
    }

    private void send(byte[] payload) {
        if (payload == null || payload.length == 0) return;
        ensureTxSocket();
        try {
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            socket.send(new DatagramPacket(payload, payload.length, localhost, TX_PORT));
            Log.d(TAG, "➡ local " + payload.length);
        } catch (Exception e) {
            Log.e(TAG, "UDP send failed", e);
        }
    }

    private void ensureTxSocket() {
        if (socket != null) return;
        try { socket = new DatagramSocket(); }
        catch (SocketException e) { Log.e(TAG, "create tx socket failed", e); }
    }

    private static byte[] withHeader(String header, byte[] body) {
        byte[] h = header.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[h.length + body.length];
        System.arraycopy(h, 0, out, 0, h.length);
        System.arraycopy(body, 0, out, h.length, body.length);
        return out;
    }
}
