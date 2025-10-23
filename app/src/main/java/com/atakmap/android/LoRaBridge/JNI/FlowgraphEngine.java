package com.atakmap.android.LoRaBridge.JNI;

import com.atakmap.coremap.log.Log;

import android.hardware.usb.UsbDeviceConnection;
import android.os.ParcelFileDescriptor;
import java.util.concurrent.*;

public final class FlowgraphEngine {
    private static final FlowgraphEngine I = new FlowgraphEngine();
    public static FlowgraphEngine get() { return I; }

    private final Object lock = new Object();
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FlowgraphThread");
        t.setDaemon(true);
        return t;
    });
    private Future<?> task;
    private enum State { STOPPED, STARTING, RUNNING, STOPPING }
    private volatile State state = State.STOPPED;
    private android.hardware.usb.UsbDeviceConnection heldConn;

    public boolean isRunning() { synchronized (lock) { return state == State.RUNNING; } }



    public void startWithConnection(UsbDeviceConnection conn) {
        synchronized (lock) {
            if (state == State.RUNNING || state == State.STARTING) {
                Log.w("FlowgraphEngine", "start ignored: state=" + state);
                return;
            }
            this.heldConn = conn;


            Log.i("FlowgraphEngine", "start enter fd=" + safeFd(conn) + Thread.currentThread().getName());

            state = State.STARTING;
            task = exec.submit(() -> {
                synchronized (lock) { state = State.RUNNING; }
                int rawFd = safeFd(heldConn);
                int fdForNative = rawFd;

                try {
                    // 复制出一份新的 fd
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(rawFd);
                    fdForNative = pfd.detachFd();
                    Log.i("FlowgraphEngine", "dup via PFD: " + rawFd + " -> " + fdForNative);
                } catch (Throwable e) {
                    Log.w("FlowgraphEngine", "dup via ParcelFileDescriptor failed, fallback to original fd", e);
                }

                try {

                    Log.i("FlowgraphEngine", "run_flowgraph_with_fd(" + fdForNative + ") begin");
                    int rc = FlowgraphNative.run_flowgraph_with_fd(fdForNative);
                    Log.i("FlowgraphEngine", "run_flowgraph_with_fd exit rc=" + rc);
                } catch (Throwable t) {
                    Log.e("FlowgraphEngine", "flowgraph thread crashed", t);
                } finally {
                synchronized (lock) {
                        state = State.STOPPED;
                }
            }
            });
        }
    }
    public void stop() {
        Log.i("FlowgraphEngine", "stop() called, state=" + state);
        synchronized (lock) {
            if (state == State.STOPPED) { Log.i("FlowgraphEngine", "stop() ignored: already STOPPED"); return; }
            state = State.STOPPING;
        }
        try {
            Log.i("FlowgraphEngine", "call FlowgraphNative.shutdown()");
            FlowgraphNative.shutdown();
            Future<?> f; synchronized (lock) { f = task; }
            if (f != null) {
                try { f.get(15000, TimeUnit.MILLISECONDS); Log.i("FlowgraphEngine","stop wait done"); }
                catch (TimeoutException te) { Log.w("FlowgraphEngine", "stop wait timeout, cancel"); f.cancel(true); }
                catch (Throwable ignored) { Log.w("FlowgraphEngine", "stop wait threw", ignored); f.cancel(true); }
            }
        } finally {
            closeConnQuietly();
            synchronized (lock) { state = State.STOPPED; task = null; }
            Log.i("FlowgraphEngine", "stop() complete, state=STOPPED");
        }
    }

    private void closeConnQuietly() {
        try {
            if (heldConn != null) {
                Log.i("FlowgraphEngine", "close UsbDeviceConnection fd=" + safeFd(heldConn));
                heldConn.close();
            }
        } catch (Throwable ignore) {}
        heldConn = null;
    }

    private static int safeFd(android.hardware.usb.UsbDeviceConnection c) {
        if (c == null) return -1;
        try { return c.getFileDescriptor(); } catch (Throwable t) { return -2; }
    }
}
