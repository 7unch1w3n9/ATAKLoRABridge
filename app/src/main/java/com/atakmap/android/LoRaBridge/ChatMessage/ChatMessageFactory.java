package com.atakmap.android.LoRaBridge.ChatMessage;

import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotDetail;

import java.util.UUID;

public class ChatMessageFactory {

    public static ChatMessageEntity fromUserInput(
            String senderUid, String senderCallsign,
            String receiverUid, String receiverCallsign,
            String message, String messageType) {
        return new ChatMessageEntity(
                "GeoChat." + senderUid + receiverUid + UUID.randomUUID().toString(),         // 本地生成唯一主键
                senderUid,
                senderCallsign,
                receiverUid,
                receiverCallsign,
                message,
                System.currentTimeMillis(),
                messageType,
                null
        );
    }

    // 从CoT Event解析
    public static ChatMessageEntity fromCotEvent(CotEvent event, String[] toUIDs) {

        CotDetail detail = event.getDetail();

        CotDetail chatNode = detail.getFirstChildByName(0, "__chat");
        String senderCallsign = null;
        String senderUid = null;
        String message = null;

        if (chatNode != null) {
            senderCallsign = chatNode.getAttribute("senderCallsign");
            senderUid = chatNode.getAttribute("senderUid");
            message = chatNode.getAttribute("message");
        }

        if (senderUid == null) {
            CotDetail linkNode = detail.getFirstChildByName(0, "link");
            if (linkNode != null) senderUid = linkNode.getAttribute("uid");
        }

        if (message == null) {
            CotDetail remarksNode = detail.getFirstChildByName(0, "remarks");
            if (remarksNode != null) message = remarksNode.getInnerText();
        }


        String receiverUid = (toUIDs != null && toUIDs.length > 0) ? toUIDs[0] : null;
        String receiverCallsign = receiverUid;
        if (receiverUid != null) {
            com.atakmap.android.contact.Contact contact =
                    com.atakmap.android.contact.Contacts.getInstance().getContactByUuid(receiverUid);
            if (contact != null && contact.getName() != null) {
                receiverCallsign = contact.getName();
            }
        }


        String messageId = event.getUID();
        String messageType = (chatNode != null) ? chatNode.getAttribute("messageType") : "text";

        long timestamp = (event.getTime() != null) ? event.getTime().getMilliseconds() : System.currentTimeMillis();


        return new ChatMessageEntity(
                messageId,
                senderUid,
                senderCallsign != null ? senderCallsign : senderUid,
                receiverUid != null ? receiverUid : "All Chat Rooms",
                receiverCallsign != null ? receiverCallsign : "All Chat Rooms",
                message,
                timestamp,
                messageType,
                event.toString()
        );
    }
}

