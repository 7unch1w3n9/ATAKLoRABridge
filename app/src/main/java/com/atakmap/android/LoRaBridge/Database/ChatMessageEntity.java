package com.atakmap.android.LoRaBridge.Database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String senderUid;
    public String senderCallsign;
    public String receiverUid;
    public String receiverCallsign;
    public String message;
    public long sentTime;

    public ChatMessageEntity(String senderUid, String senderCallsign,
                             String receiverUid, String receiverCallsign,
                             String message, long sentTime) {
        this.senderUid = senderUid;
        this.senderCallsign = senderCallsign;
        this.receiverUid = receiverUid;
        this.receiverCallsign = receiverCallsign;
        this.message = message;
        this.sentTime = sentTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderCallsign() {
        return senderCallsign;
    }

    public void setSenderCallsign(String senderCallsign) {
        this.senderCallsign = senderCallsign;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public void setReceiverUid(String receiverUid) {
        this.receiverUid = receiverUid;
    }

    public String getReceiverCallsign() {
        return receiverCallsign;
    }

    public void setReceiverCallsign(String receiverCallsign) {
        this.receiverCallsign = receiverCallsign;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getSentTime() {
        return sentTime;
    }

    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }
}

