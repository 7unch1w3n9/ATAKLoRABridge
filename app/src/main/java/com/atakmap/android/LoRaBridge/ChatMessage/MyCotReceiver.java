/*package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.chat.GeoChatService;
import com.atakmap.android.maps.MapView;

public class MyCotReceiver extends BroadcastReceiver {
    private static final String TAG = "MyCotReceiver";
    private ChatRepository chatRepository;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyCotReceiver", "收到广播: " + intent.getAction());
        if (!"com.atakmap.android.chat.NEW_CHAT_MESSAGE".equals(intent.getAction())) return;

        long messageId = intent.getLongExtra("id", -1);
        long groupId = intent.getLongExtra("groupId", -1);
        if (messageId == -1) return;

        if (chatRepository == null) {
            chatRepository = new ChatRepository(context.getApplicationContext());
        }

        Bundle chatBundle = GeoChatService.getInstance().getMessage(messageId, groupId);
        if (chatBundle == null) {
            Log.w(TAG, "No chatBundle found for id: " + messageId);
            return;
        }

        String senderUid = chatBundle.getString("senderUid");
        String senderCallsign = chatBundle.getString("senderCallsign", senderUid);
        String message = chatBundle.getString("message");
        long sentTime = chatBundle.getLong("sentTime", System.currentTimeMillis());

        Log.d(TAG, "Received GeoChat message: " + message);

        ChatMessageEntity entity = new ChatMessageEntity(
                senderUid,
                senderCallsign,
                MapView.getDeviceUid(),
                MapView.getMapView().getDeviceCallsign(),
                message,
                sentTime
        );

        chatRepository.insert(entity);
    }
}

 */
