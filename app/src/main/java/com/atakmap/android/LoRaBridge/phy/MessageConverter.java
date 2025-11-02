package com.atakmap.android.LoRaBridge.phy;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;

public interface MessageConverter extends Converter<ChatMessageEntity> {
    /**
     * 将消息实体转换为物理层传输格式
     */
    default byte[] encodeMessage(ChatMessageEntity message){return encode(message);}

    /**
     * 将物理层接收的数据转换为消息实体
     */
    default ChatMessageEntity decodeMessage(byte[] payload){return decode(payload);}
}