package com.atakmap.android.LoRaBridge.Database;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;


    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    public void insert(ChatMessageEntity message) {
        repository.insert(message);
    }

    public LiveData<List<ChatMessageEntity>> getMessagesForContact(String contactId) {
        return repository.getMessagesForContact(contactId);
    }

    public LiveData<List<ChatMessageEntity>> getAllMessages() {
        return repository.getAllMessages();
    }
}
