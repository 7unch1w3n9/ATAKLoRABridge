package com.atakmap.android.LoRaBridge.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE senderUid = :contactUid OR receiverUid = :contactUid ORDER BY sentTime ASC")
    LiveData<List<ChatMessageEntity>> getMessagesForContact(String contactUid);

    @Query("SELECT * FROM chat_messages ORDER BY sentTime ASC")
    LiveData<List<ChatMessageEntity>> getAllMessages();

    @Query("SELECT COUNT(*) FROM chat_messages WHERE messageId = :messageId")
    int existsByMessageId(String messageId);

    @Query("SELECT * FROM chat_messages ORDER BY sentTime DESC LIMIT 1")
    LiveData<ChatMessageEntity> getLatestMessage();

    @Query("DELETE FROM chat_messages")
    void deleteAllMessages();
}