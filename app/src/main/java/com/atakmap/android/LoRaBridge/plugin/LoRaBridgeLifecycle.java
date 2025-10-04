package com.atakmap.android.LoRaBridge.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Future;

import com.atakmap.android.LoRaBridge.ChatMessage.MessageDatenbankObserver;
import com.atakmap.android.LoRaBridge.ChatMessage.IncomingGeoChatListener;
import com.atakmap.android.LoRaBridge.ChatMessage.OutgoingGeoChatInterceptor;
import com.atakmap.android.LoRaBridge.ChatMessage.MessageSyncService;
import com.atakmap.android.LoRaBridge.Contacts.ContactStore;
import com.atakmap.android.LoRaBridge.JNI.FlowgraphEngine;
import com.atakmap.android.LoRaBridge.JNI.UsbHackrfManager;
import com.atakmap.android.LoRaBridge.phy.UdpManager;
import com.atakmap.android.LoRaBridge.JNI.PluginNativeLoader;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.LoRaBridge.LoRaBridgeMapComponent;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.usb.UsbDeviceConnection;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;

public class LoRaBridgeLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;
    private Activity hostActivity;

    private IncomingGeoChatListener incomingGeoChatListener;
    private MessageDatenbankObserver messageDatenbankObserver;
    private MessageSyncService syncService;
    private final static String TAG = "LoRaBridgeLifecycle";
    private android.hardware.usb.UsbDeviceConnection hackrfConn;

    private UsbHackrfManager usbMgr;
    public LoRaBridgeLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<>();
        this.mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0,
                         final transapps.mapi.MapView arg1) {
        Log.d(TAG, "onCreate: this=" + System.identityHashCode(this)
                + " act=" + (arg0==null?"null":arg0.getClass().getSimpleName())
                + " mv=" + (arg1==null?"null":"ok"));
        this.hostActivity = arg0;
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        Log.d(TAG, "hostActivity ist " + hostActivity);
        this.mapView = (MapView) arg1.getView();
        this.overlays.add(new LoRaBridgeMapComponent());
        Iterator<MapComponent> iter = this.overlays.iterator();
        LoRaBridgeMapComponent c;
        while (iter.hasNext()) {
            c = (LoRaBridgeMapComponent) iter.next();
            try {
                c.onCreate(this.pluginContext,
                        arg0.getIntent(),
                        this.mapView,
                        hostActivity);
            } catch (Exception e) {
                Log.w(TAG,
                        "Unhandled exception trying to create overlays MapComponent",
                        e);
                iter.remove();
            }
        }
        final Context initCtx =
                (pluginContext != null) ? this.pluginContext:
                        (hostActivity != null) ? hostActivity.getApplicationContext() :
                                this.mapView.getContext().getApplicationContext();

        PluginNativeLoader.init(initCtx);
        ContactStore.init(mapView);
    }

    @Override
    public void onDestroy() {
        try {
            FlowgraphEngine.get().stop();
        } catch (Throwable ignore) {}
        try {
            if (usbMgr != null) usbMgr.stop();
        } catch (Throwable ignore) {}

        for (MapComponent c : this.overlays)
            c.onDestroy(this.pluginContext, this.mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for (MapComponent c : this.overlays)
            c.onPause(this.pluginContext, this.mapView);
    }

    @Override
    public void onResume() {
        for (MapComponent c : this.overlays)
            c.onResume(this.pluginContext, this.mapView);
        if (usbMgr != null && !FlowgraphEngine.get().isRunning()) usbMgr.probeNow();
    }
    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);

        final Context app = (hostActivity != null)
                ? hostActivity.getApplicationContext()
                : pluginContext;
        if (usbMgr == null) {
            usbMgr = new UsbHackrfManager(app, "com.atakmap.android.LoRaBridge.USB_PERMISSION");
            usbMgr.setListener(new UsbHackrfManager.Listener() {
                @Override public void onHackrfReady(UsbDeviceConnection conn) {
                    FlowgraphEngine.get().startWithConnection(conn);
                }
                @Override public void onHackrfDetached() {
                    FlowgraphEngine.get().stop();
                }
                @Override public void onPermissionDenied() {
                    Log.w(TAG, "USB permission denied");
                }
            });
        }
        usbMgr.start();

        if (incomingGeoChatListener == null) {
            incomingGeoChatListener = new IncomingGeoChatListener(MapView.getMapView().getContext());
        }

        if (hostActivity != null) {
            messageDatenbankObserver = new MessageDatenbankObserver(
                    pluginContext,
                    hostActivity
            );
        }

        syncService = MessageSyncService.getInstance(pluginContext);
        syncService.udpManager.start();
        Log.d(TAG, "Flowgraph同步服务初始化完成");

        CommsMapComponent.getInstance().registerPreSendProcessor(
                new OutgoingGeoChatInterceptor(pluginContext)
        );

        Log.d(TAG, "onStart done");
    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);


        if (hackrfConn != null) { try { hackrfConn.close(); } catch (Throwable ignore) {} hackrfConn = null; }

        if (incomingGeoChatListener != null) { incomingGeoChatListener.shutdown(); incomingGeoChatListener = null; }

        if (syncService != null) { syncService.shutdown(); syncService = null; Log.d(TAG, "Flowgraph同步服务已关闭"); }

    }
/*
    private void startFlowgraphWithFd(int fd) {
        synchronized (flowgraphLock) {
            if (flowgraphFuture != null && !flowgraphFuture.isDone()) {
                Log.d(TAG, "Flowgraph 已经在运行");
                return;
            }
            flowgraphFuture = flowgraphExecutor.submit(() -> {
                try {
                    Log.i(TAG, "启动 flowgraph (FD=" + fd + ") …");
                    int rc = FlowgraphNative.run_flowgraph_with_fd(fd);
                    Log.i(TAG, "flowgraph 退出, rc=" + rc);
                } catch (Throwable t) {
                    Log.e(TAG, "flowgraph 线程异常终止", t);
                }
            });
        }
    }
    private void startFlowgraphThread() {
        if (flowgraphFuture != null && !flowgraphFuture.isDone()) {
            Log.d(TAG, "Flowgraph 已经在运行");
            return;
        }

        flowgraphFuture = flowgraphExecutor.submit(() -> {
            try {
                Log.d(TAG, "启动流图线程...");
                FlowgraphNative.run_flowgraph();
                Log.d(TAG, "流图线程正常结束");
            } catch (Throwable t) {
                Log.e(TAG, "流图线程异常终止", t);
            }
        });
    }
    private void shutdownFlowgraph() {
        synchronized (flowgraphLock) {
            if (flowgraphFuture == null) return;

            try {
                Log.d(TAG, "请求关闭流图...");
                FlowgraphNative.shutdown(); // 幂等
                // 等待结束
                long deadline = System.currentTimeMillis() + 1500;
                while (System.currentTimeMillis() < deadline) {
                    try {
                        flowgraphFuture.get(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
                        break;
                    } catch (java.util.concurrent.TimeoutException ignore) {}
                }
                if (!flowgraphFuture.isDone()) {
                    Log.w(TAG, "流图关闭超时，尝试取消任务");
                    flowgraphFuture.cancel(true);
                }
            } catch (Throwable t) {
                Log.e(TAG, "关闭流图时出错", t);
            } finally {
                flowgraphFuture = null;
            }
            Log.d(TAG, "流图已成功关闭");
        }
    }

 */

/*
    private void requestHackRfPermission() {
        final String HOST_PKG = "com.atakmap.app.civ";
        Log.d("LoRaBridge", "requestHackRfPermission called. Current pluginContext: " + this.pluginContext);
        if (this.pluginContext == null) {
            Log.e("LoRaBridge", "FATAL: Context is null in requestHackRfPermission!");
            // This is where your NPE is likely, but the check above should catch it earlier.

            Context hostCtx;
            try {
                hostCtx = pluginContext.createPackageContext(HOST_PKG, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "createPackageContext failed, fallback to app ctx", e);
                hostCtx = (hostActivity != null) ? hostActivity.getApplicationContext() : pluginContext;
            }

            UsbManager usbManager = (UsbManager) hostCtx.getSystemService(Context.USB_SERVICE);

            Intent intent = new Intent(c).setPackage(HOST_PKG);
            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getBroadcast(hostCtx, 0, intent, flags);

            // 用该 ctx 记住，onStop 时用它来反注册
            usbReceiverCtx = hostCtx;
            hostCtx.registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            for (UsbDevice device : deviceList.values()) {
                final int vid = device.getVendorId();
                final int pid = device.getProductId();
                // 支持 HackRF One + Jawbreaker + rad1o
                boolean isHackrf = (vid == 0x1D50) && (pid == 0x6089 || pid == 0x604B || pid == 0xCC15);
                if (!isHackrf) continue;

                if (usbManager.hasPermission(device)) {
                    // 已授权，直接打开并启动
                    Log.i(TAG, "已有 USB 权限，直接打开设备 " + device.getDeviceName());
                    hackrfConn = usbManager.openDevice(device);
                    if (hackrfConn == null) {
                        Log.e(TAG, "openDevice returned null");
                        continue;
                    }
                    int fd = hackrfConn.getFileDescriptor();
                    startFlowgraphWithFd(fd);
                } else {
                    try {
                        Log.d(TAG, "请求 USB 权限, dev=" + device.getDeviceName());
                        usbManager.requestPermission(device, pi);
                    } catch (IllegalArgumentException iae) {
                        Log.e(TAG, "Usb permission request failed", iae);
                    }
                }
            }
        }
    }

private void requestHackRfPermission() {
    // 只用插件自己的 ApplicationContext（和测试版一致）
    final Context ctx = hostActivity != null ? hostActivity.getApplicationContext() : pluginContext;
    Log.d(TAG,"CONTEXXXXXXXX = "+ ctx);
    UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);

    // PendingIntent 也发给插件自己（和测试一致）
    Intent intent = new Intent(ACTION_USB_PERMISSION).setPackage(ctx.getPackageName());
    int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
    PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent, flags);

    // 用同一个 app ctx 注册 Receiver（和测试一致）
    try { if (usbReceiverCtx != null) usbReceiverCtx.unregisterReceiver(usbReceiver); } catch (Throwable ignore) {}
    ctx.registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    usbReceiverCtx = ctx;

    // 枚举 HackRF，已授权直接 open，未授权就 requestPermission（完全照测试逻辑）
    for (UsbDevice d : usbManager.getDeviceList().values()) {
        if (d.getVendorId() == 0x1D50 && d.getProductId() == 0x6089) {
            if (usbManager.hasPermission(d)) {
                hackrfConn = usbManager.openDevice(d);
                if (hackrfConn == null) {
                    Log.e(TAG, "openDevice returned null");
                    return;
                }
                int fd = hackrfConn.getFileDescriptor();
                Log.i(TAG, "HackRF FD = " + fd);
                startFlowgraphWithFd(fd);
            } else {
                Log.d(TAG, "请求 USB 权限, dev=" + d.getDeviceName());
                usbManager.requestPermission(d, pi);
            }
            break; // 找到第一台就够了
        }
    }
}
*/

/*

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) return;

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device == null) {
                Log.w(TAG, "USB permission intent without device");
                return;
            }
            if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.w(TAG, "USB permission denied: " + device);
                return;
            }

            Log.i(TAG, "Permission granted for " + device);
            UsbManager usb = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            hackrfConn = usb.openDevice(device);
            if (hackrfConn == null) {
                Log.e(TAG, "openDevice returned null");
                return;
            }
            int fd = hackrfConn.getFileDescriptor();
            Log.i(TAG, "HackRF FD = " + fd);

            // ✅ 用 executor 启动（不会阻塞广播线程）
            startFlowgraphWithFd(fd);
        }
    };

private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
        if (!ACTION_USB_PERMISSION.equals(intent.getAction())) return;

        // 和测试一样的写法（targetSdk<33 可直接这样拿）
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

        if (!granted || device == null) {
            Log.w(TAG, "USB permission denied or device null: " + device);
            return;
        }

        Log.i(TAG, "Permission granted for " + device);
        UsbManager usb = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        hackrfConn = usb.openDevice(device);
        if (hackrfConn == null) {
            Log.e(TAG, "openDevice returned null");
            return;
        }
        int fd = hackrfConn.getFileDescriptor();
        Log.i(TAG, "HackRF FD = " + fd);
        startFlowgraphWithFd(fd); // 和测试一样 -> 直接把 fd 丢给 native
    }
};
 */

}

