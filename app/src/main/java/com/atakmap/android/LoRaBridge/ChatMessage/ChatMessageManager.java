package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.UUID;
/**
 * Manages message transmission and conversion.
 * Handles: Message entity ⇄ CoT event transformations.
 * Phase 2: Will implement LoRa encoding methods.
 */
public class ChatMessageManager {
    private static final String TAG = "ChatMessageManager";
    private final Context context;

    public ChatMessageManager(Context context) {
        this.context = context;
    }

    // 外部调用：插入消息后，把它同步到GeoChat
    public void sendToGeoChat(CotEvent event) {
        CotMapComponent.getInternalDispatcher().dispatch(event);
        Log.d(TAG, "Message dispatched to GeoChat");
    }
    /**
     * Converts message entity to CoT event (Plugin → GeoChat).
     * Key field mappings:
     *   senderUid → link.uid
     *   message → remarks.innerText
     * Special: Adds __lora.originalId to preserve source ID.
     * @param message Message entity
     * @return Complete CoT event
     */
    public  CotEvent convertChatMessageToCotEvent(ChatMessageEntity message) {
        CotEvent event = new CotEvent();
        CoordinatedTime now = new CoordinatedTime();
        GeoPoint currentLocation = MapView.getMapView().getSelfMarker().getPoint();
        CotPoint point = new CotPoint(currentLocation);

        // 设置基础属性
        String uid = "GeoChat." + message.getSenderUid() + ".All Chat Rooms." + UUID.randomUUID();
        event.setUID(uid);
        event.setType("b-t-f");
        event.setHow("m-g");
        event.setTime(now);
        event.setStart(now);
        event.setStale(now.addMinutes(2));
        event.setPoint(point);

        // 构造 <detail>
        CotDetail detail = new CotDetail("detail");

        CotDetail chat = new CotDetail("__chat");
        chat.setAttribute("parent", "RootContactGroup");
        chat.setAttribute("groupOwner", "false");
        chat.setAttribute("messageId", UUID.randomUUID().toString());
        chat.setAttribute("chatroom", "All Chat Rooms");
        chat.setAttribute("id", "All Chat Rooms");
        chat.setAttribute("senderCallsign", message.getSenderCallsign());

        CotDetail loraDetail = new CotDetail("__lora");
        loraDetail.setAttribute("originalId", message.getId());
        detail.addChild(loraDetail);

        CotDetail chatgrp = new CotDetail("chatgrp");
        chatgrp.setAttribute("uid0", message.getSenderUid());
        chatgrp.setAttribute("uid1", "All Chat Rooms");
        chatgrp.setAttribute("id", "All Chat Rooms");
        chat.addChild(chatgrp);
        detail.addChild(chat);

        CotDetail link = new CotDetail("link");
        link.setAttribute("uid", message.getSenderUid());
        link.setAttribute("type", "a-f-G-U-C");
        link.setAttribute("relation", "p-p");
        detail.addChild(link);

        CotDetail serverDest = new CotDetail("__serverdestination");
        serverDest.setAttribute("destination", "0.0.0.0:4242:tcp");
        detail.addChild(serverDest);

        CotDetail remarks = new CotDetail("remarks");
        remarks.setAttribute("source", "BAO.F.ATAK." + message.getSenderUid());
        remarks.setAttribute("to", "All Chat Rooms");
        remarks.setAttribute("time", now.toString());
        remarks.setInnerText(message.getMessage());
        detail.addChild(remarks);

        event.setDetail(detail);

        // Debug 输出
        com.atakmap.coremap.log.Log.d("LoRaBridge", "📤 Convert message to CoT Event:");
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  UID: " + event.getUID());
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Type: " + event.getType());
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Sender: " + message.getSenderCallsign());
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Message: " + message.getMessage());
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Point: " + point.toString());
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Detail XML: " + detail.toString());

        return event;
    }
}
