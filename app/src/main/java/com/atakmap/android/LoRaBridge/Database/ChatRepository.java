package com.atakmap.android.LoRaBridge.Database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * Repository for database access operations.
 * Encapsulates DB interactions using single-thread executor.
 */
public class ChatRepository {
    private final ChatMessageDao chatDao;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, LiveData<List<ChatMessageEntity>>> contactMessageCache = new HashMap<>();
    private boolean a;

    public ChatRepository(Context context) {
        ChatDatabase db = ChatDatabase.getDatabase(context);
        chatDao = db.chatMessageDao();
    }

    public void insert(ChatMessageEntity message) {
        executor.execute(() -> chatDao.insert(message));
    }

    public LiveData<List<ChatMessageEntity>> getMessagesForContact(String contactUid) {
        if (!contactMessageCache.containsKey(contactUid)) {
            contactMessageCache.put(contactUid, chatDao.getMessagesForContact(contactUid));
        }
        return contactMessageCache.get(contactUid);
    }

    public LiveData<List<ChatMessageEntity>> getAllMessages() {
        return chatDao.getAllMessages();
    }
    /**
     * Inserts message if not exists (deduplication).
     * Uniqueness: Based on messageId constraint.
     * @param msg Message to insert
     * @return True if new record was inserted
     */
    public boolean insertIfNotExists(ChatMessageEntity msg) {
        final boolean[] result = {false};
        executor.execute(() -> {
            if (msg != null && chatDao.existsByMessageId(msg.messageId) == 0) {
                chatDao.insert(msg);
                result[0] = true;
            }
        });

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }

    public LiveData<ChatMessageEntity> getLatestMessage() {
        return chatDao.getLatestMessage();
    }

    public void deleteAllMessages() {
        executor.execute(chatDao::deleteAllMessages);
    }

}
