package com.atakmap.android.LoRaBridge.CoTListener;
import android.os.Bundle;
import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

public class MySendInterceptor implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "MySendInterceptor";
    private final ChatRepository chatRepository;

    public MySendInterceptor() {
        this.chatRepository = new ChatRepository(MapView.getMapView().getContext());
    }

    @Override
    public void processCotEvent(CotEvent event, String[] toUIDs) {
        Log.d(TAG, "将发送 CoT 消息: " + event.getUID() + ", type: " + event.getType());

        if (!"b-t-f".equals(event.getType())) return;

        CotDetail detail = event.getDetail();
        if (detail == null) return;
/*
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(detail.toString()));

            String message = null;
            String senderUid = null;
            String senderCallsign = null;

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xpp.getName()) {
                        case "__chat":
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if ("senderCallsign".equals(xpp.getAttributeName(i))) {
                                    senderCallsign = xpp.getAttributeValue(i);
                                }
                            }
                            break;
                        case "link":
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if ("uid".equals(xpp.getAttributeName(i))) {
                                    senderUid = xpp.getAttributeValue(i);
                                }
                            }
                            break;
                        case "remarks":
                            if (xpp.next() == XmlPullParser.TEXT) {
                                message = xpp.getText();
                            }
                            break;
                    }
                }
                eventType = xpp.next();
            }

            // 取接收者（第一个 UID）
            String receiverUid = (toUIDs != null && toUIDs.length > 0) ? toUIDs[0] : null;
            String receiverCallsign = receiverUid;
            Log.d(TAG, "senderUid: " + receiverUid);
            Log.d(TAG, "senderCallsign " + receiverCallsign);


            // 可选：从联系人管理器查找 Callsign
            if (receiverUid != null) {
                com.atakmap.android.contact.Contact contact =
                        com.atakmap.android.contact.Contacts.getInstance().getContactByUuid(receiverUid);
                if (contact != null && contact.getName() != null) {
                    receiverCallsign = contact.getName();
                }
            }
            Log.d(TAG, "receiverUid: " + receiverUid);
            Log.d(TAG, "receiverCallsign " + receiverCallsign);

            if (message != null && senderUid != null && receiverUid != null) {
                ChatMessageEntity entity = new ChatMessageEntity(
                        senderUid,
                        senderCallsign != null ? senderCallsign : senderUid,
                        receiverUid,
                        receiverCallsign,
                        message,
                        System.currentTimeMillis()
                );

                chatRepository.insert(entity);
                Log.d(TAG, "GeoChat 已记录: " + message);
            }

        } catch (Exception e) {
            Log.e(TAG, "解析 CoT 事件失败", e);
        }
    }
*/
        CotDetail chatNode = detail.getFirstChildByName(0,"__chat");
        CotDetail linkNode = detail.getFirstChildByName(0,"link");
        CotDetail remarksNode = detail.getFirstChildByName(0,"remarks");

        String senderCallsign = (chatNode != null) ? chatNode.getAttribute("senderCallsign") : null;
        String senderUid = (linkNode != null) ? linkNode.getAttribute("uid") : null;
        String message = (remarksNode != null) ? remarksNode.getInnerText(): null;

// 取接收者（第一个 UID）
        String receiverUid = (toUIDs != null && toUIDs.length > 0) ? toUIDs[0] : null;
        String receiverCallsign = receiverUid;

        if (receiverUid != null) {
            com.atakmap.android.contact.Contact contact =
                    com.atakmap.android.contact.Contacts.getInstance().getContactByUuid(receiverUid);
            if (contact != null && contact.getName() != null) {
                receiverCallsign = contact.getName();
            }
        }

        Log.d(TAG, "senderUid: " + senderUid);
        Log.d(TAG, "senderCallsign: " + senderCallsign);
        Log.d(TAG, "receiverUid: " + receiverUid);
        Log.d(TAG, "receiverCallsign: " + receiverCallsign);
        Log.d(TAG, "message: " + message);

        if (message != null && senderUid != null ) {
            ChatMessageEntity entity = new ChatMessageEntity(
                    senderUid,
                    senderCallsign != null ? senderCallsign : senderUid,
                    receiverUid != null ? receiverUid : "All Chat Rooms",
                    receiverCallsign!= null ? receiverCallsign : "All Chat Rooms",
                    message,
                    System.currentTimeMillis()
            );

            chatRepository.insert(entity);
            Log.d(TAG, "GeoChat 已记录: " + message);
        }
    }
}

