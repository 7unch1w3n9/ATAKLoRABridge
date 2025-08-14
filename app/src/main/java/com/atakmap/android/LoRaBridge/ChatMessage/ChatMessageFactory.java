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

        String origin = null;
        CotDetail chatNode = event.getDetail().getFirstChildByName(0, "__chat");

        CotDetail loraNode  = detail.getFirstChildByName(0, "__lora");
        if (loraNode  != null) {
            origin = loraNode.getAttribute("origin");
            if ("Plugin".equals(origin)) {
                return null;
            }
        }
        else{origin = "GeoChat";}

        String senderCallsign = null;
        String senderUid = null;
        String message = null;

        if (chatNode != null) {
            senderCallsign = chatNode.getAttribute("senderCallsign");
            senderUid = chatNode.getAttribute("sender")  != null ? chatNode.getAttribute("sender"):detail.getFirstChildByName(0, "link").getAttribute("uid");
            message = chatNode.getAttribute("message");
        }

        if (message == null) {
            CotDetail remarksNode = detail.getFirstChildByName(0, "remarks");
            if (remarksNode != null) message = remarksNode.getInnerText();
        }


        String receiverUid;
            if ((toUIDs != null && toUIDs.length > 0)) {
                receiverUid = toUIDs[0];
            } else {
                assert chatNode != null;
                CotDetail remarksNode= detail.getFirstChildByName(0, "remarks");
                receiverUid = remarksNode.getAttribute("to");
            }
            String receiverCallsign = receiverUid;
        if (receiverUid != null) {
            com.atakmap.android.contact.Contact contact =
                    com.atakmap.android.contact.Contacts.getInstance().getContactByUuid(receiverUid);
            if (contact != null && contact.getName() != null) {
                receiverCallsign = contact.getName();
            }
        }


        String originalId = null;
        if (loraNode  != null) {
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
                receiverUid,
                receiverCallsign,
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

