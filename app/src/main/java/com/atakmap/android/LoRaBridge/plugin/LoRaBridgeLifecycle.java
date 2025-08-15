package com.atakmap.android.LoRaBridge.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageManager;
import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageObserver;
import com.atakmap.android.LoRaBridge.ChatMessage.GeoChatPreSendInterceptor;
import com.atakmap.android.LoRaBridge.ChatMessage.MessageSyncService;
import com.atakmap.android.LoRaBridge.ChatMessage.RemoteToMeListener;
import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
import com.atakmap.android.LoRaBridge.JNI.FlowgraphNative;
import com.atakmap.android.LoRaBridge.phy.UdpManager;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.LoRaBridge.LoRaBridgeMapComponent;

import kotlinx.coroutines.flow.Flow;
import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;

public class LoRaBridgeLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;
    private Activity hostActivity;

    private RemoteToMeListener remoteToMeListener;
    private ChatMessageObserver chatMessageObserver;
    private MessageSyncService syncService;
    private final static String TAG = "LoRaBridgeLifecycle";

    private ExecutorService flowgraphExecutor;
    private Future<?> flowgraphFuture;

    private volatile boolean flowgraphShuttingDown = false;

    private UdpManager udpManager;

    public LoRaBridgeLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<>();
        this.mapView = null;
        PluginNativeLoader.init(ctx);
        this.flowgraphExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0,
                         final transapps.mapi.MapView arg1) {
        this.hostActivity = arg0;
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
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
    }

    @Override
    public void onDestroy() {
        /*
        shutdownFlowgraph();
        if (flowgraphExecutor != null) {
            try {
                flowgraphExecutor.shutdown();
                if (!flowgraphExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    flowgraphExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                flowgraphExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            flowgraphExecutor = null;
        }
         */
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
    }
    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);

        if (remoteToMeListener == null) {
            remoteToMeListener = new RemoteToMeListener(MapView.getMapView().getContext());
        }

        if (hostActivity != null) {
            chatMessageObserver = new ChatMessageObserver(
                    pluginContext,
                    hostActivity
            );
        }

        syncService = MessageSyncService.getInstance(pluginContext);
        syncService.udpManager.start();
        Log.d(TAG, "Flowgraph同步服务初始化完成");

        CommsMapComponent.getInstance().registerPreSendProcessor(
                new GeoChatPreSendInterceptor(pluginContext)
        );

        startFlowgraphThread();
        Log.d(TAG, "Not gonna SEE mee");
    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);

        if (remoteToMeListener != null) {
            remoteToMeListener.shutdown();
            remoteToMeListener = null;
        }
        shutdownFlowgraph();
        if (syncService != null) {
            syncService.shutdown();
            syncService = null;
            Log.d(TAG, "Flowgraph同步服务已关闭");
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
        if (flowgraphFuture == null || flowgraphShuttingDown) {
            return;
        }
        // 如果任务已经结束或被取消，也直接当作已关停处理
        if (flowgraphFuture.isDone() || flowgraphFuture.isCancelled()) {
            flowgraphFuture = null;
            return;
        }
        try {
            Log.d(TAG, "请求关闭流图...");
            FlowgraphNative.shutdown();
            long deadline = System.currentTimeMillis() + 1500;
            // 等待流图线程结束（最多2秒）
            while (System.currentTimeMillis() < deadline) {
                try {
                    flowgraphFuture.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    break;
                } catch (java.util.concurrent.TimeoutException ignore) {
                    // 继续等
                }
            }
            if (!flowgraphFuture.isDone()) {
                Log.w(TAG, "流图关闭超时，尝试取消任务");
                flowgraphFuture.cancel(true);
            }
            Log.d(TAG, "流图已成功关闭");
        } catch (java.util.concurrent.CancellationException e) {
            Log.w(TAG, "流图任务已取消");
        } catch (Throwable t) {
            Log.e(TAG, "关闭流图时出错", t);
        } finally {
            flowgraphFuture = null;           // 幂等关键：清空引用
            flowgraphShuttingDown = false;
        }
    }
}