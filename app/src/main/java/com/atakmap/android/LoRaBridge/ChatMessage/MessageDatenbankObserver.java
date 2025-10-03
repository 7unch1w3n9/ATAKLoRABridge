package com.atakmap.android.LoRaBridge.ChatMessage;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
import com.atakmap.coremap.log.Log;

public class MessageDatenbankObserver {
    private final String TAG = "Observer";
    private final Context context;
    private final ChatViewModel chatViewModel;
    private String lastProcessedId = "";
    private final MessageSyncService syncService;
    private boolean isFirst = true;

    public MessageDatenbankObserver(Context context, Activity hostActivity) {

        this.context = context;
        this.chatViewModel = new ViewModelProvider((ViewModelStoreOwner) hostActivity).get(ChatViewModel.class);
        this.syncService = MessageSyncService.getInstance(context);

        chatViewModel.getLatestMessage().observe((LifecycleOwner) hostActivity, latest -> {
            if (isFirst) {
                isFirst = false;
                if (latest != null) {
                    lastProcessedId = latest.getId();
                }
                return;
            }
            if (latest == null ||
                    !"Plugin".equals(latest.getOrigin()) ||
                    latest.getId().equals(lastProcessedId)) {
                return;
            }
            if (!syncService.isMessageProcessed(latest.getId())) {
                lastProcessedId = latest.getId();
                Log.d(TAG, "TESSSSSSSSSSSSSSSSSSSSSSST33333333333 ");
                syncService.processOutgoingMessage(latest);
            }
        });
    }
}