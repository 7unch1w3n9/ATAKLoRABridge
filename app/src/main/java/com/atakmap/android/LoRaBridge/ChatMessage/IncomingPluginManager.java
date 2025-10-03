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

/**
 * Manages message transmission and conversion.
 * Handles: Message entity ⇄ CoT event transformations.
 * Phase 2: Will implement LoRa encoding methods.
 */
public class IncomingPluginManager {
    private static final String TAG = "IncomingPluginManager";
    private final Context context;

    public IncomingPluginManager(Context context) {
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
     *   SenderUid → link.uid
     *   message → remarks.innerText
     * @param message Message entity
     * @return Complete CoT event
     */
    public  CotEvent convertChatMessageToCotEvent(ChatMessageEntity message) {
        CotEvent event = new CotEvent();
        CoordinatedTime now = new CoordinatedTime();
        GeoPoint currentLocation = MapView.getMapView().getSelfMarker().getPoint();

        // 设置基础属性
        String uid = message.getId();
        event.setUID(uid);
        event.setType("b-t-f");
        event.setHow("h-g-i-g-o");
        event.setTime(now);
        event.setStart(now);
        event.setStale(now.addMinutes(5));

        CotPoint point = new CotPoint(currentLocation);
        event.setPoint(point);

        // 构造 <detail>
        CotDetail detail = new CotDetail("detail");

        CotDetail chat = new CotDetail("__chat");
        chat.setAttribute("messageId", message.getId());
        chat.setAttribute("sender", message.getSenderUid());
        chat.setAttribute("receiver", message.getReceiverUid());
        chat.setAttribute("senderCallsign", message.getSenderCallsign());


        CotDetail loraDetail = new CotDetail("__lora");
        loraDetail.setAttribute("originalId", message.getId());
        loraDetail.setAttribute("origin", "Plugin");
        detail.addChild(loraDetail);
/*
        CotDetail chatgrp = new CotDetail("chatgrp");
        chatgrp.setAttribute("uid0", message.getSenderUid());
        chatgrp.setAttribute("uid1", "All Chat Rooms");
        chatgrp.setAttribute("id", "All Chat Rooms");
        chat.addChild(chatgrp);
        detail.addChild(chat);

 */

        CotDetail link = new CotDetail("link");
        link.setAttribute("uid", message.getSenderUid());
        link.setAttribute("type", "a-f-G-U-C");
        link.setAttribute("relation", "p-p");
        detail.addChild(link);

        CotDetail remarks = new CotDetail("remarks");
        remarks.setAttribute("source",  message.getSenderCallsign());
        remarks.setAttribute("to", message.getReceiverCallsign());
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
        com.atakmap.coremap.log.Log.d("LoRaBridge", "  Detail XML: " + detail.toString());

        return event;
    }
}
