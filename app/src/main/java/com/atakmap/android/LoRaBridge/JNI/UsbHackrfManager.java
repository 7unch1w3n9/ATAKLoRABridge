package com.atakmap.android.LoRaBridge.JNI;

import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.*;

import com.atakmap.coremap.log.Log;


public final class UsbHackrfManager {
    public interface Listener {
        void onHackrfReady(UsbDeviceConnection conn);
        void onHackrfDetached();
        void onPermissionDenied();
    }

    private final Context appCtx;
    private final UsbManager usb;
    private final String actionUsbPermission;
    private PendingIntent permissionPI;
    private BroadcastReceiver receiver;
    private Listener listener;

    // 记录当前 Engine 正在使用的 HackRF 设备名（/dev/bus/usb/xxx/yyy）
    private volatile String activeHackrfName;

    public UsbHackrfManager(Context appCtx, String actionUsbPermission) {
        this.appCtx = appCtx.getApplicationContext();
        this.usb = (UsbManager) appCtx.getSystemService(Context.USB_SERVICE);
        this.actionUsbPermission = actionUsbPermission;
    }

    public void setListener(Listener l) { this.listener = l; }

    public void start() {
        IntentFilter f = new IntentFilter();
        f.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        f.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        f.addAction(actionUsbPermission);

        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String a = i.getAction();
                android.util.Log.i("UsbHackrfManager", "onReceive action=" + a + " thread=" + Thread.currentThread().getName());

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(a)) {
                    UsbDevice d = i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (d != null) android.util.Log.i("UsbHackrfManager", "ATTACHED: " + devStr(d));
                    probeNow();
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(a)) {
                    UsbDevice d = i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (d != null) android.util.Log.w("UsbHackrfManager", "DETACHED: " + devStr(d));
                    if (isHackrf(d) && devNameEqActive(d)) {
                        if (listener != null) listener.onHackrfDetached();
                        activeHackrfName = null;
                    } else {
                        android.util.Log.i("UsbHackrfManager", "DETACHED ignored (not active HackRF)");
                    }
                } else if (actionUsbPermission.equals(a)) {
                    handlePermissionResult(i);
                }
            }
        };
        appCtx.registerReceiver(receiver, f);

        Intent intent = new Intent(actionUsbPermission).setPackage(appCtx.getPackageName());
        permissionPI = PendingIntent.getBroadcast(appCtx, 0,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        probeNow();
    }

    public void stop() {
        try { if (receiver != null) appCtx.unregisterReceiver(receiver); } catch (Throwable ignore) {}
        receiver = null;
        permissionPI = null;
        activeHackrfName = null;
    }

    public void probeNow() {
        Log.i("UsbHackrfManager", "probeNow()");

        if (FlowgraphEngine.get().isBusy()) {
            Log.i("UsbHackrfManager", "Engine busy; skip probe");
            return;
        }

        for (UsbDevice d : usb.getDeviceList().values()) {
            Log.i("UsbHackrfManager", "  check " + devDump(d));
            if (!isHackrf(d)) continue;

            Log.i("UsbHackrfManager", "  isHackrf=" + true + " hasPermission=" + usb.hasPermission(d));
            if (usb.hasPermission(d)) openAndNotify(d);
            else usb.requestPermission(d, permissionPI);
            break;
        }
    }

    private void handlePermissionResult(Intent i) {
        UsbDevice d = i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        boolean granted = i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        if (d == null) return;
        if (!granted) { if (listener!=null) listener.onPermissionDenied(); return; }
        openAndNotify(d);
    }

    private void openAndNotify(UsbDevice d) {
        if (FlowgraphEngine.get().isBusy()) {
            android.util.Log.w("UsbHackrfManager", "Engine busy; skip open " + d.getDeviceName());
            return;
        }
        android.util.Log.i("UsbHackrfManager", "openAndNotify OPEN " + devStr(d));
        UsbDeviceConnection c = usb.openDevice(d);
        if (c == null) { android.util.Log.w("UsbHackrfManager", "openDevice returned null"); return; }

        android.util.Log.i("UsbHackrfManager", "openDevice OK, fd=" + c.getFileDescriptor() + " conn=" + c);
        activeHackrfName = d.getDeviceName();

        if (listener != null) listener.onHackrfReady(c);
    }

    private boolean devNameEqActive(UsbDevice d) {
        String name = d.getDeviceName();
        return activeHackrfName != null && activeHackrfName.equals(name);
    }

    private static boolean isHackrf(UsbDevice d) {
        return d != null && d.getVendorId()==0x1D50 &&
                (d.getProductId()==0x6089 || d.getProductId()==0x604B || d.getProductId()==0xCC15);
    }

    private static String devStr(UsbDevice d) {
        if (d == null) return "null";
        return "name=" + d.getDeviceName() + " vid=0x" + toHex(d.getVendorId()) + " pid=0x" + toHex(d.getProductId());
    }

    private static String devDump(UsbDevice d) {
        if (d == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("dev=").append(devStr(d))
                .append(" class=").append(d.getDeviceClass())
                .append(" sub=").append(d.getDeviceSubclass())
                .append(" proto=").append(d.getDeviceProtocol())
                .append(" ifaceCount=").append(d.getInterfaceCount());
        for (int i=0;i<d.getInterfaceCount();i++) {
            android.hardware.usb.UsbInterface inf = d.getInterface(i);
            sb.append(" [if#").append(inf.getId())
                    .append(" cls=").append(inf.getInterfaceClass())
                    .append(" sub=").append(inf.getInterfaceSubclass())
                    .append(" ep=").append(inf.getEndpointCount()).append("]");
        }
        return sb.toString();
    }

    private static String toHex(int v) {
        return String.format(java.util.Locale.US, "%04X", v & 0xFFFF);
    }
}
