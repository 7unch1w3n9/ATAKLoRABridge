package com.atakmap.android.LoRaBridge.phy;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;

public interface MessageCodec {
    byte[] encode(ChatMessageEntity message);
    ChatMessageEntity decode(byte[] raw);
}
