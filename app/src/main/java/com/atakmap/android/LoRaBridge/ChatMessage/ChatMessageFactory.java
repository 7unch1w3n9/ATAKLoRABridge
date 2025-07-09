package com.atakmap.android.LoRaBridge.ChatMessage;

import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotDetail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Factory for converting between CoT events and database entities.
 * Phase 1: Handles bidirectional conversion between GeoChat and plugin messages.
 * Phase 2: Will support LoRa-specific message identifiers.
 */
public class ChatMessageFactory {

    /**
     * Creates a new message entity from user input (Plugin → DB).
     * @param senderUid Sender device UID
     * @param senderCallsign Sender callsign
     * @param receiverUid Receiver UID ("All Chat Rooms" for broadcast)
     * @param receiverCallsign Receiver display name
     * @param message Text content
     * @param messageType Message type (text/alert/etc.)
     * @return Complete message entity with auto-generated ID and timestamp
     */
    public static ChatMessageEntity fromUserInput(
            String senderUid, String senderCallsign,
            String receiverUid, String receiverCallsign,
            String message, String messageType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());
        String id = "PluginMsg." + UUID.randomUUID().toString();
        return new ChatMessageEntity(
                id,
                senderUid,
                senderCallsign,
                receiverUid,
                receiverCallsign,
                message,
                timestamp,
                messageType,
                "Plugin",
                null
        );
    }

    /**
     * Parses a CoT event into a message entity (GeoChat → Plugin).
     * Special: Handles __lora.originalId for deduplication.
     * @param event Raw CoT event
     * @param toUIDs Array of recipient UIDs
     * @return Message entity (null if parsing fails)
     */
    public static ChatMessageEntity fromCotEvent(CotEvent event, String[] toUIDs) {
        if (event == null) return null;
        try {CotDetail detail = event.getDetail();

        String origin = "External";
        CotDetail chatNode = event.getDetail().getFirstChildByName(0, "__chat");
        if (chatNode != null) {
            origin = "GeoChat";
        }
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

            // 检查是否有原始消息ID
        String originalId = null;
        CotDetail loraNode = detail.getFirstChildByName(0, "__lora");
        if (loraNode != null) {
            originalId = loraNode.getAttribute("originalId");
        }

        String messageId = (originalId != null) ? originalId : event.getUID();

        String messageType = (chatNode != null) ? chatNode.getAttribute("messageType") : "text";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(event.getTime() != null ? new Date(event.getTime().getMilliseconds()) : new Date());




        return new ChatMessageEntity(
                messageId,
                senderUid,
                senderCallsign != null ? senderCallsign : senderUid,
                receiverUid != null ? receiverUid : "All Chat Rooms",
                receiverCallsign != null ? receiverCallsign : "All Chat Rooms",
                message,
                timestamp,
                messageType,
                origin,
                event.toString()
        );} catch (Exception e) {
            Log.e("ChatMessageFactory", "Error parsing CoT event", e);
            return null;
        }
    }
}

