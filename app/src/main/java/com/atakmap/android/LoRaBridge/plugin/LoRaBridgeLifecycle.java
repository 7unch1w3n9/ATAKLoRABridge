
package com.atakmap.android.LoRaBridge.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;


import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageManager;
import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageObserver;
import com.atakmap.android.LoRaBridge.ChatMessage.GeoChatPreSendInterceptor;
import com.atakmap.android.LoRaBridge.ChatMessage.MessageSyncService;
import com.atakmap.android.LoRaBridge.ChatMessage.RemoteToMeListener;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.LoRaBridge.LoRaBridgeMapComponent;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.log.Log;

public class LoRaBridgeLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;
    private ChatViewModel chatViewModel;
    private Activity hostActivity;

    private RemoteToMeListener remoteToMeListener;
    private ChatMessageObserver chatMessageObserver;
    private MessageSyncService syncService;

    private final static String TAG = "LoRaBridgeLifecycle";

    public LoRaBridgeLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<>();
        this.mapView = null;
        PluginNativeLoader.init(ctx);
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
        LoRaBridgeLifecycle.this.overlays
                .add(new LoRaBridgeMapComponent());

        // create components
        Iterator<MapComponent> iter = LoRaBridgeLifecycle.this.overlays
                .iterator();
        LoRaBridgeMapComponent c;
        while (iter.hasNext()) {
            c = (LoRaBridgeMapComponent) iter.next();
            try {
                c.onCreate(LoRaBridgeLifecycle.this.pluginContext,
                        arg0.getIntent(),
                        LoRaBridgeLifecycle.this.mapView,
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
    /**
     * Plugin startup entry (system callback).
     * Initialization sequence:
     *  1. Core map components
     *  2. Message listeners
     *  3. Sync services
     *  4. GeoChat interceptor
     */
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

        syncService = new MessageSyncService(pluginContext);
        CommsMapComponent.getInstance().registerPreSendProcessor(new GeoChatPreSendInterceptor(pluginContext));

        /*
        AtakBroadcast.DocumentedIntentFilter filter = new AtakBroadcast.DocumentedIntentFilter("com.atakmap.android.chat.NEW_CHAT_MESSAGE");
        cotReceiver = new MyCotReceiver();
        AtakBroadcast.getInstance().registerReceiver(cotReceiver,  filter);

        //AtakBroadcast.getInstance().sendBroadcast(new Intent("com.atakmap.android.chat.NEW_CHAT_MESSAGE"));
        Intent testIntent = new Intent("com.atakmap.android.chat.NEW_CHAT_MESSAGE");
        pluginContext.sendBroadcast(testIntent);
         */

    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);

        if (remoteToMeListener != null) {
            remoteToMeListener.shutdown();
            remoteToMeListener = null;
        }
    }
}
