package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;


public class GeoChatPreSendInterceptor implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "GeoChatPreSendInterceptor";
    private final ChatRepository chatRepository;
    private final MessageSyncService syncService;

    public GeoChatPreSendInterceptor(Context context) {
        this.chatRepository = new ChatRepository(MapView.getMapView().getContext());
        this.syncService = new MessageSyncService(context);
    }


    @Override
    public void processCotEvent(CotEvent event, String[] toUIDs) {
        syncService.processIncomingCotEvent(event, toUIDs);
    }
}
