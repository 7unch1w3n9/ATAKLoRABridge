package com.atakmap.android.LoRaBridge.Database;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {
    private final ChatMessageDao chatDao;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, LiveData<List<ChatMessageEntity>>> contactMessageCache = new HashMap<>();

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

    public void insertIfNotExists(ChatMessageEntity msg) {
        executor.execute(() -> {
            if (chatDao.existsByMessageId(msg.messageId) == 0) {
                chatDao.insert(msg);
            }
        });
    }

}
