package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;

import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;


public class OutgoingGeoChatInterceptor implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "OutgoingGeoChatInterceptor";
    private final ChatRepository chatRepository;
    private final MessageSyncService syncService;

    public OutgoingGeoChatInterceptor(Context context) {
        this.chatRepository = new ChatRepository(MapView.getMapView().getContext());
        this.syncService = MessageSyncService.getInstance(context);
    }


    @Override
    public void processCotEvent(CotEvent event, String[] toUIDs) {
        if ("b-t-f".equals(event.getType())) {
            Log.e(TAG, "PreSend ------------------------------------");
            event.getDetail().getFirstChildByName(0, "__chat").setAttribute("sender",MapView.getDeviceUid());
            syncService.processIncomingCotEventFromGeoChat(event, toUIDs);
        }
    }
}
