package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.LoRaBridge.phy.MessageConverter;
import com.atakmap.android.LoRaBridge.phy.UdpManager;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.HashSet;
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
    private static MessageSyncService instance;
    private final ChatRepository chatRepository;
    private final IncomingPluginManager incomingPluginManager;
    private final MessageTracker messageTracker = new MessageTracker();
    private final Set<String> processedMessageIds = new HashSet<>();
    private final MessageConverter messageConverter;
    public final UdpManager udpManager;

    private MessageSyncService(Context context) {
        this.chatRepository = new ChatRepository(context);
        this.incomingPluginManager = new IncomingPluginManager(context);
        this.messageConverter = new LoRaMessageConverter();
        this.udpManager = new UdpManager(this, messageConverter);
    }
    public static synchronized MessageSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new MessageSyncService(context.getApplicationContext());
        }
        return instance;
    }
    /**
     * Processes incoming CoT events (entry point).
     * Security: Filters invalid events and self-messages.
     * @param event Raw CoT event
     * @param toUIDs Recipient UID array
     */
    public void processIncomingCotEventFromGeoChat(CotEvent event, String[] toUIDs) {
        if (!isValidGeoChatEvent(event)) return;

        CotDetail lora = event.getDetail().getFirstChildByName(0, "__lora");
        if (lora != null ) {
            Log.d(TAG, "Skipping plugin processed message to avoid loop");
            return;
        }
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

    /**
     * Processes locally generated messages (DB → GeoChat).
     * Note: Only handles "Plugin" origin messages.
     * @param message Message entity to send
     */
    public void processOutgoingMessage(ChatMessageEntity message) {
        if ("Plugin".equals(message.getOrigin())) {
            if (messageTracker.isProcessed(message.getId())) {
                Log.d(TAG, "Message already processed: " + message.getId());
                return;
            }
            messageTracker.markProcessed(message.getId());
            CotEvent cotEvent = incomingPluginManager.convertChatMessageToCotEvent(message);
            if (cotEvent != null) {
                //CotDetail detail = cotEvent.getDetail();
                //CotDetail pluginDetail = detail.getFirstChildByName(0, "__Plugin");
                //pluginDetail.setAttribute("messageId", cotEvent.getUID());
                //detail.addChild(pluginDetail);
                if(cotEvent.getDetail() != null) incomingPluginManager.sendToGeoChat(cotEvent);
            }

            if (shouldSendToLoRa(message)) {
                sendToFlowgraph(message);
            }
        }
    }
    private void sendToFlowgraph(ChatMessageEntity message) {
        try {
            byte[] payload = messageConverter.encodeMessage(message);
            udpManager.sendAsync(payload);

        } catch (Exception e) {
            Log.e(TAG, "Failed to send to Flowgraph", e);
        }
    }

    public void handleFlowgraphMessage(byte[] payload) {
        try {
            Log.d(TAG, "Received Flowgraph payload (" + payload.length + " bytes)");

            // 使用转换器解码消息
            ChatMessageEntity message = messageConverter.decodeMessage(payload);

            if (message == null) {
                Log.w(TAG, "Failed to decode Flowgraph payload");
                return;
            }
            processIncomingPhyMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "Error handling Flowgraph message", e);
        }
    }

    public boolean isMessageProcessed(String messageId) {
        return messageTracker.isProcessed(messageId);
    }
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

    private boolean shouldSendToLoRa(ChatMessageEntity message) {
        //return !"All Chat Rooms".equals(message.getReceiverUid());
        Log.d(TAG, "Sending" + message.getId() + "To Flowgraph");
        return true;
    }


    /**
     * 处理从物理层接收的消息
     */
    public void processIncomingPhyMessage(ChatMessageEntity message) {
        if (message == null) return;
        Log.d(TAG, "Recevice " + message.getId() + "from Flowgraph");
        if (processedMessageIds.contains(message.getId())) {
            Log.d(TAG, "Duplicate Flowgraph  message ignored: " + message.getId());
            return;
        }
        processedMessageIds.add(message.getId());
        if (processedMessageIds.size() > 500) {
            processedMessageIds.clear(); // 防止内存占用过大
        }
        message.setOrigin("PHY");

        // 重建原始CoT
        rebuildRawCot(message);

        // 保存到数据库
        boolean inserted = chatRepository.insertIfNotExists(message);

        if (inserted) {
            Log.d(TAG, "New PHY message saved: " + message.getMessage());

            // 转换为CoT事件并发送到GeoChat
            Log.d(TAG, "TESSSSSSSSSSSSSSSSSSSSSSST2 ");
            CotEvent event = incomingPluginManager.convertChatMessageToCotEvent(message);
            if (event != null) {
                incomingPluginManager.sendToGeoChat(event);
            }
        } else {
            Log.d(TAG, "Duplicate PHY message ignored");
        }
    }


    private void rebuildRawCot(ChatMessageEntity message) {
        if (message.getCotRawXml()!= null && !message.getCotRawXml().isEmpty()) {
            return;
        }
        message.setCotRawXml(message.toString());

    }

    public void shutdown() {
        if (udpManager != null) {
            udpManager.stop();
            Log.d(TAG, "UDP管理器已停止");
        }

        processedMessageIds.clear();
        Log.d(TAG, "消息同步服务清理完成");
    }
}