package com.atakmap.android.LoRaBridge.Database;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey
    @NonNull
    public String messageId;
    public String senderUid;
    public String senderCallsign;
    public String receiverUid;
    public String receiverCallsign;
    public String message;
    public String sentTime;
    public String messageType;
    public String origin;
    public String cotRawXml;
    public ChatMessageEntity(@NonNull String messageId,
                             String senderUid,
                             String senderCallsign,
                             String receiverUid,
                             String receiverCallsign,
                             String message,
                             String sentTime,
                             String messageType,
                             String origin,
                             String cotRawXml) {
        this.messageId = messageId;
        this.senderUid = senderUid;
        this.senderCallsign = senderCallsign;
        this.receiverUid = receiverUid;
        this.receiverCallsign = receiverCallsign;
        this.message = message;
        this.sentTime = sentTime;
        this.messageType = messageType;
        this.origin = origin;
        this.cotRawXml = cotRawXml;
    }

    public String getId() {
        return messageId;
    }

    public void setId(String messageId) {
        this.messageId = messageId;
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

    public String getSentTime() {
        return sentTime;
    }

    public void setSentTime(String sentTime) {
        this.sentTime = sentTime;
    }

    public String getOrigin() {
        return origin;
    }
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @NonNull
    public String toString() {
        return "ChatMessageEntity{" +
                "messageId='" + messageId + '\'' +
                ", senderUid='" + senderUid + '\'' +
                ", senderCallsign='" + senderCallsign + '\'' +
                ", receiverUid='" + receiverUid + '\'' +
                ", receiverName='" + receiverCallsign + '\'' +
                ", message='" + message + '\'' +
                ", timestamp='" + sentTime  + '\'' +
                ", messageType='" + messageType + '\'' +
                ", origin='" + origin + '\'' +
                ", cotRawXml.length=" + (cotRawXml != null ? cotRawXml.length() : 0) +
                '}';
    }
}

