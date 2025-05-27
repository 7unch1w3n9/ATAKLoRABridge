package com.atakmap.android.LoRaBridge.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    void insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE senderUid = :contactUid OR receiverUid = :contactUid ORDER BY sentTime ASC")
    LiveData<List<ChatMessageEntity>> getMessagesForContact(String contactUid);

    @Query("SELECT * FROM chat_messages ORDER BY sentTime ASC")
    LiveData<List<ChatMessageEntity>> getAllMessages();
}