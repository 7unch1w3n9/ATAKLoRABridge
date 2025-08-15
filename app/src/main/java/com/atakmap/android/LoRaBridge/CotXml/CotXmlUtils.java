package com.atakmap.android.LoRaBridge.CotXml;

import android.os.Build;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for parsing CoT protocol XML.
 * Encapsulates all CoT field extraction logic.
 * Note: All methods are thread-safe statics.
 */
public class CotXmlUtils {
    /**
     * Extracts message text from <remarks> node.
     * @param event Complete CoT event
     * @return Message text content (may be null)
     */
    public static String getMessageText(CotEvent event) {
        if (event == null || event.getDetail() == null) return null;
        XmlPullParser xpp = initParser(event.getDetail());
        if (xpp == null) return null;

        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.getName().equalsIgnoreCase("remarks")) {
                    if (xpp.next() == XmlPullParser.TEXT) {
                        return xpp.getText();
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRecipientUid(CotEvent event) {
        if (event == null || event.getDetail() == null) return null;

        // First try __chat.id
        CotDetail chatNode = event.findDetail("__chat");
        if (chatNode != null) {
            String id = chatNode.getAttribute("id");
            if (id != null && !id.isEmpty()) return id;
        }

        // Then try chatgrp.uid1
        CotDetail chatGrp = event.findDetail("chatgrp");
        if (chatGrp != null) {
            String uid1 = chatGrp.getAttribute("uid1");
            if (uid1 != null && !uid1.isEmpty()) return uid1;
        }

        // Fallback: remarks.to
        CotDetail remarks = event.findDetail("remarks");
        if (remarks != null) {
            String to = remarks.getAttribute("to");
            if (to != null && !to.isEmpty()) return to;
        }

        return null;
    }

    public static String getSenderCallsign(CotEvent event) {
        if (event == null || event.getDetail() == null) return null;
        CotDetail chatNode = event.findDetail("__chat");
        if (chatNode != null) {
            return chatNode.getAttribute("senderCallsign");
        }
        CotDetail contactNode = event.findDetail("contact");
        if (contactNode != null) {
            return contactNode.getAttribute("callsign");
        }
        return null;
    }
    /**
     * Determines if event is a group chat message.
     * @param event CoT event
     * @return True when receiver is "All Chat Rooms"
     */
    public static boolean isGroupChat(CotEvent event) {
        String recipient = getRecipientUid(event);
        return "All Chat Rooms".equalsIgnoreCase(recipient);
    }

    public static boolean isDirectMessage(CotEvent event) {
        String recipient = getRecipientUid(event);
        return recipient != null && !"All Chat Rooms".equalsIgnoreCase(recipient);
    }

    public static String getSenderUid(CotEvent event) {
        CotDetail link = event.findDetail("link");
        if (link != null) {
            return link.getAttribute("uid");
        }
        return null;
    }

    public static String getChatRoomName(CotEvent event) {
        CotDetail chatNode = event.findDetail("__chat");
        if (chatNode != null) {
            return chatNode.getAttribute("chatroom");
        }
        return null;
    }


    public static String getMessageTime(CotEvent event) {
        CotDetail remarks = event.findDetail("remarks");
        if (remarks != null) {
            String timeStr = remarks.getAttribute("time");
            if (timeStr != null) {
                return timeStr;
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private static XmlPullParser initParser(CotDetail detail) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(detail.toString()));
            return xpp;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
    }

}
