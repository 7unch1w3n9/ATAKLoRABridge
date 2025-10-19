package com.atakmap.android.LoRaBridge.Database;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
/**
 * ViewModel for UI interaction with message database.
 * Provides LiveData for real-time UI updates.
 */
public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    public void insert(ChatMessageEntity message) {
        repository.insert(message);
    }
    /**
     * Gets message stream for specific contact.
     * @param contactId Contact UID
     * @return LiveData of message list (observable)
     */
    public LiveData<List<ChatMessageEntity>> getMessagesForContact(String contactId) {
        return repository.getMessagesForContact(contactId);
    }

    public LiveData<List<ChatMessageEntity>> getAllMessages() {
        return repository.getAllMessages();
    }
    /**
     * Gets latest message (for send triggering).
     * @return LiveData of most recent message
     */
    public LiveData<ChatMessageEntity> getLatestMessage() {
        return repository.getLatestMessage();
    }

    public void deleteAllMessages() {
        repository.deleteAllMessages();
    }

    public void insertIfNotExists(ChatMessageEntity msg) {
        repository.insertIfNotExists(msg);
    }

    public void deleteMessagesForContact(String contactId) {
        repository.deleteMessagesByContact(contactId);
    }
}
