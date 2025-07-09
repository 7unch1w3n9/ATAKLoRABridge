package com.atakmap.android.LoRaBridge.ChatMessage;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

public class ChatMessageObserver {

    private final Context context;
    private final ChatViewModel chatViewModel;
    private String lastProcessedId = "";
    private final MessageSyncService syncService;

    public ChatMessageObserver(Context context, Activity hostActivity) {

        this.context = context;
        this.chatViewModel = new ViewModelProvider((ViewModelStoreOwner) hostActivity).get(ChatViewModel.class);
        this.syncService = new MessageSyncService(context);

        chatViewModel.getLatestMessage().observe((LifecycleOwner) hostActivity, latest -> {
            if (latest == null || !"Plugin".equals(latest.getOrigin())) return;
            if (latest.getId().equals(lastProcessedId)) {
                return;
            }
            lastProcessedId = latest.getId();
            syncService.processOutgoingMessage(latest);
        });
    }
}