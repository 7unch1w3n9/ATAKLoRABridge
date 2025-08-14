package com.atakmap.android.LoRaBridge.phy;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;

public interface MessageConverter {
    /**
     * 将消息实体转换为物理层传输格式
     */
    byte[] encodeMessage(ChatMessageEntity message);

    /**
     * 将物理层接收的数据转换为消息实体
     */
    ChatMessageEntity decodeMessage(byte[] payload);
}