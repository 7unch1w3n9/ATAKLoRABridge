package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Core message synchronization service.
 * Responsibilities:
 *  1. Incoming GeoChat messages → Local database
 *  2. Local messages → GeoChat broadcast
 * Phase 2: Will add LoRa message channel.
 */
public class MessageSyncService {
    private static final String TAG = "MessageSyncService";

    private final ChatRepository chatRepository;
    private final ChatMessageManager chatMessageManager;
    private final MessageTracker messageTracker = new MessageTracker();


    public MessageSyncService(Context context) {
        this.chatRepository = new ChatRepository(context);
        this.chatMessageManager = new ChatMessageManager(context);
    }

    /**
     * Processes incoming CoT events (entry point).
     * Security: Filters invalid events and self-messages.
     * @param event Raw CoT event
     * @param toUIDs Recipient UID array
     */
    public void processIncomingCotEvent(CotEvent event, String[] toUIDs) {
        if (!isValidGeoChatEvent(event)) return;

        ChatMessageEntity entity = ChatMessageFactory.fromCotEvent(event, toUIDs);
        if (entity == null) {
            Log.w(TAG, "Failed to create entity from CoT event");
            return;
        }

        if (isSelfMessage(entity)) {
            if ("GeoChat".equals(entity.getOrigin())) {
                Log.d(TAG, "Processing self-originated GeoChat message");
                saveMessageIfNew(entity);
            } else {
                Log.d(TAG, "Ignoring self-originated non-GeoChat message");
            }
        } else {
            saveMessageIfNew(entity);
        }
    }

    // 处理用户发送的消息（从UI发出）convertChatMessageToCotEvent
    /**
     * Processes locally generated messages (DB → GeoChat).
     * Note: Only handles "Plugin" origin messages.
     * @param message Message entity to send
     */
    public void processOutgoingMessage(ChatMessageEntity message) {
        if (message == null) return;


        // 只处理Plugin来源的消息
        if ("Plugin".equals(message.getOrigin())) {
            // 检查是否已处理过
            if (messageTracker.isProcessed(message.getId())) {
                Log.d(TAG, "Message already processed: " + message.getId());
                return;
            }
            messageTracker.markProcessed(message.getId());
            chatRepository.insert(message);
            CotEvent cotEvent = chatMessageManager.convertChatMessageToCotEvent(message);
            if (cotEvent != null) {
                chatMessageManager.sendToGeoChat(cotEvent);
            }
        }
    }

    // 添加消息跟踪，防止重复处理
    private boolean isValidGeoChatEvent(CotEvent event) {
        return event != null && event.isValid() &&
                "b-t-f".equals(event.getType()) &&
                event.getDetail() != null;
    }

    private boolean isSelfMessage(ChatMessageEntity entity) {
        return entity.getSenderUid() != null &&
                entity.getSenderUid().equals(MapView.getDeviceUid());
    }

    private void saveMessageIfNew(ChatMessageEntity entity) {
        boolean inserted = chatRepository.insertIfNotExists(entity);
        if (inserted) {
            Log.d(TAG, "New message saved: " + entity.getMessage());
        } else {
            Log.d(TAG, "Duplicate message ignored");
        }
    }

    // Message tracker to prevent duplicate processing
    private static class MessageTracker {
        private final Set<String> processedIds = new HashSet<>();
        private final long MAX_AGE = 5 * 60 * 1000; // 5mins

        /**
         * Checks if message was processed within 5-minute window.
         * @param id Unique message ID
         * @return True if already processed
         */
        public synchronized boolean isProcessed(String id) {
            return processedIds.contains(id);
        }
        public synchronized void markProcessed(String id) {
            if (processedIds.size() > 100) {
                processedIds.clear();
            }
            processedIds.add(id);
        }
    }
}