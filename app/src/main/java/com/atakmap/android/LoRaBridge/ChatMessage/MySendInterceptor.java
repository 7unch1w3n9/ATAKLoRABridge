package com.atakmap.android.LoRaBridge.ChatMessage;

import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;


public class MySendInterceptor implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "MySendInterceptor";
    private final ChatRepository chatRepository;

    public MySendInterceptor() {
        this.chatRepository = new ChatRepository(MapView.getMapView().getContext());
    }

    @Override
    public void processCotEvent(CotEvent event, String[] toUIDs) {
        if (!"b-t-f".equals(event.getType())) return;
        ChatMessageEntity entity = ChatMessageFactory.fromCotEvent(event, toUIDs);
        chatRepository.insertIfNotExists(entity);
    }
}
