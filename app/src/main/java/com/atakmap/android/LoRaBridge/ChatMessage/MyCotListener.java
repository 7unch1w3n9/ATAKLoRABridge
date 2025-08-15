/*package com.atakmap.android.LoRaBridge.ChatMessage;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

public class MyCotListener implements CotServiceRemote.CotEventListener {
    private final ChatRepository chatRepository;

    public MyCotListener(Context context) {
        // 获取 application context
        chatRepository = new ChatRepository(context);
    }

    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        Log.d("TAG", "2222222222222222222222222222222222222222222222222222222222");
        Log.d("TAG", "cotDetail XML: " + cotEvent.getDetail().toString());
        if (cotEvent.isValid()) {
            try {
                if (!"b-t-f".equals(cotEvent.getType())) return;

                CotDetail cotDetail = cotEvent.getDetail();
                if (cotDetail == null) return;
                Log.d("TAG", "2222222222222222222222222222222222222222222222222222222222");
                Log.d("TAG", cotEvent.toString());

                int eventType = -1;
                XmlPullParserFactory factory = null;
                XmlPullParser xpp = null;
                String callsign = null;
                String deviceCallsign = null;
                String message = null;

                try {
                    factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    xpp = factory.newPullParser();
                    xpp.setInput(new StringReader(cotDetail.toString()));
                    eventType = xpp.getEventType();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            Log.d("TAG", xpp.getName());
                            if (xpp.getName().equalsIgnoreCase("remarks")) {
                                if (xpp.next() == XmlPullParser.TEXT)
                                    message = xpp.getText();
                            } else if (xpp.getName().equalsIgnoreCase("__chat")) {
                                int attributeCount = xpp.getAttributeCount();
                                Log.d("TAG", "__chat has " + +attributeCount);
                                for (int i = 0; i < attributeCount; i++) {
                                    if (xpp.getAttributeName(i).equalsIgnoreCase("senderCallsign"))
                                        callsign = xpp.getAttributeValue(i);
                                }
                            } else if (xpp.getName().equalsIgnoreCase("link")) {
                                int attributeCount = xpp.getAttributeCount();
                                Log.d("TAG", "link has " + +attributeCount);
                                for (int i = 0; i < attributeCount; i++) {
                                    if (xpp.getAttributeName(i).equalsIgnoreCase("uid"))
                                        deviceCallsign = xpp.getAttributeValue(i);
                                }
                            }
                        }
                        eventType = xpp.next();
                    }

                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }

                ChatMessageEntity entity = new ChatMessageEntity(
                        deviceCallsign,
                        callsign != null ? callsign : deviceCallsign,
                        MapView.getDeviceUid(),
                        MapView.getMapView().getDeviceCallsign(),
                        message,
                        cotEvent.getTime().getMilliseconds()
                );
                chatRepository.insert(entity);

            } catch (Exception e) {
                Log.e("MyCotListener", "解析 CotEvent 出错", e);
            }
        }
    }
}

 */


