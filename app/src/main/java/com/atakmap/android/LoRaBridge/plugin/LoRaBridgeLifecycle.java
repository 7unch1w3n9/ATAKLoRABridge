
package com.atakmap.android.LoRaBridge.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import com.atakmap.android.LoRaBridge.CoTListener.MyCotListener;
import com.atakmap.android.LoRaBridge.CoTListener.MyCotReceiver;
import com.atakmap.android.LoRaBridge.CoTListener.MySendInterceptor;
import com.atakmap.android.LoRaBridge.Database.ChatDatabase;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.chat.GeoChatService;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.LoRaBridge.LoRaBridgeMapComponent;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
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
    private MyCotReceiver cotReceiver;

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

    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);


        if (hostActivity != null) {
            chatViewModel = new ViewModelProvider((ViewModelStoreOwner) hostActivity).get(ChatViewModel.class);
            chatViewModel.getAllMessages().observe((LifecycleOwner) hostActivity, messages -> {
                if (messages == null || messages.isEmpty()) return;

                // 拿到最新一条消息（注意Room默认LiveData会回传整个列表）
                ChatMessageEntity latest = messages.get(messages.size() - 1);

                // 判断是不是别人发给我们的
                if (!latest.senderUid.equals(MapView.getDeviceUid())) {
                    Log.i(TAG, "💬 新消息来自 [" + latest.senderCallsign + "]： " + latest.message);
                    // TODO: 你也可以触发震动、通知栏提醒等
                }
            });
        }

        CotServiceRemote remote = new CotServiceRemote();
        remote.setCotEventListener(new MyCotListener(pluginContext));
        remote.connect(new CotServiceRemote.ConnectionListener() {
            @Override
            public void onCotServiceConnected(Bundle bundle) {
                Log.d(TAG, "✔ CotServiceRemote connected and listener active!");
            }
            @Override
            public void onCotServiceDisconnected() {
                Log.d(TAG, "❌ CotServiceRemote disconnected!");
            }
        });

        CommsMapComponent.getInstance().registerPreSendProcessor(new MySendInterceptor());

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
        if (cotReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(cotReceiver);
        }
    }
}
