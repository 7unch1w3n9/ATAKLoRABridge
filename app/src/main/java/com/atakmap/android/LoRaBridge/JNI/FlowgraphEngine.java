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

    public boolean isBusy() {
        synchronized (lock) {
            return state == State.STARTING || state == State.RUNNING || state == State.STOPPING;
        }
    }
    private volatile CountDownLatch runningGate = new CountDownLatch(0);
    private volatile CountDownLatch terminated = new CountDownLatch(0);


    public void startWithConnection(UsbDeviceConnection conn) {
        synchronized (lock) {
            if (isBusy()) {
                Log.w("FlowgraphEngine", "start ignored: state=" + state);
                return;
            }
            heldConn = conn;
            state = State.STARTING;

            final int rawFd = safeFd(conn);
            Log.i("FlowgraphEngine", "start enter fd=" + rawFd + " " + Thread.currentThread().getName());


            runningGate = new CountDownLatch(1);

            task = exec.submit(() -> {
                synchronized (lock) { state = State.RUNNING; }

                int fdForNative = rawFd;

                try {
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(rawFd);
                    fdForNative = pfd.detachFd();
                    Log.i("FlowgraphEngine", "dup via PFD: " + rawFd + " -> " + fdForNative);
                } catch (Throwable e) {
                    Log.w("FlowgraphEngine", "dup via PFD failed, fallback to original fd", e);
                }



                try {
                    Log.i("FlowgraphEngine", "run_flowgraph_with_fd(" + fdForNative + ") begin");
                    int rc = FlowgraphNative.run_flowgraph_with_fd(fdForNative);
                    Log.i("FlowgraphEngine", "run_flowgraph_with_fd exit rc=" + rc);

                    try {
                        runningGate.await();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Throwable t) {
                    Log.e("FlowgraphEngine", "flowgraph thread crashed", t);
                } if (conn != null) {
                    conn.close();
                    Log.i("FlowgraphEngine", "closed UsbDeviceConnection immediately after dup FD");
                }
                heldConn = null;
            });
        }
    }
    public void stop() {
        Log.i("FlowgraphEngine", "stop() called, state=" + state);

        synchronized (lock) {
            if (state == State.STOPPED) {
                Log.i("FlowgraphEngine", "stop() ignored: already STOPPED");
                return;
            }
            state = State.STOPPING;
        }

        try {
            Log.i("FlowgraphEngine", "stop(): calling FlowgraphNative.shutdown()");
            try { FlowgraphNative.shutdown(); } catch (Exception e) {}
            CountDownLatch gate = runningGate;
            if (gate != null) gate.countDown();

            Future<?> f;
            synchronized (lock) { f = task; }
            if (f != null) {
                Log.w("FlowgraphEngine", "stop wait timeout; cancel task");
                f.cancel(true);
            }
        } finally {
            closeConnQuietly(2500);

            synchronized (lock) {
                state = State.STOPPED;
                task  = null;
            }
            Log.i("FlowgraphEngine", "stop() complete, state=STOPPED");
        }
    }


    private void closeConnQuietly(long ms) {
        final UsbDeviceConnection c = heldConn;
        heldConn = null;
        if (c == null) return;
        exec.submit(() -> {
            int fdSnapshot = -1;
            try { fdSnapshot = c.getFileDescriptor(); } catch (Throwable ignore) {}
            try {
                Log.i("FlowgraphEngine", "close UsbDeviceConnection (delayed) fd=" + fdSnapshot);
                c.close();
            } catch (Throwable ignore) {}
        });
    }

    private static int safeFd(UsbDeviceConnection c) {
        if (c == null) return -1;
        try { return c.getFileDescriptor(); } catch (Throwable t) { return -2; }
    }
}
