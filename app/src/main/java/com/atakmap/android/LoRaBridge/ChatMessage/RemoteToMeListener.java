package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;


/**
 * Listener for remote messages targeting this device (CotService callback).
 * Note: Deprecated direct parsing in favor of MessageSyncService.
 */
public class RemoteToMeListener implements CotServiceRemote.CotEventListener {

    private final CotServiceRemote cotServiceRemote;
    private String TAG = "RemoteToMeListener";
    private final ChatRepository chatRepository;

    private final MessageSyncService syncService;

    public RemoteToMeListener(Context context) {
        this.chatRepository = new ChatRepository(context);;
        this.syncService = new MessageSyncService(context);
        cotServiceRemote = new CotServiceRemote();
        cotServiceRemote.setCotEventListener(this);
        cotServiceRemote.connect(new CotServiceRemote.ConnectionListener() {
            @Override
            public void onCotServiceConnected(Bundle bundle) {
                Log.d("ChatCotBridge", "CotService connected");
            }

            @Override
            public void onCotServiceDisconnected() {
                Log.d("ChatCotBridge", "CotService disconnected");
            }
        });
    }

    /**
     * CotService event callback entry point.
     * @param cotEvent Received CoT event
     * @param bundle Additional metadata
     */
    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        // Delegate directly to sync service
        syncService.processIncomingCotEvent(cotEvent, null);
    }

    public void shutdown() {
        if (cotServiceRemote != null) {
            cotServiceRemote.disconnect();
            Log.d(TAG, "CotServiceRemote disconnected");
        }
    }
}
