
    package com.atakmap.android.LoRaBridge.ChatMessage;

    import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
    import com.atakmap.android.LoRaBridge.phy.MessageConverter;
    import com.atakmap.coremap.log.Log;

    import java.nio.charset.StandardCharsets;
    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.Locale;
    import java.util.StringTokenizer;
    import java.util.UUID;

    public class LoRaMessageConverter implements MessageConverter {
        private static final String TAG = "LoRaMessageConverter";
        private static final String FIELD_DELIMITER = "|";
        private static final String FIELD_ESCAPE = "\\";
        private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

        @Override
        public byte[] encodeMessage(ChatMessageEntity message) {
            try {
                // 创建精简版本的消息（清除不必要字段）
                ChatMessageEntity lite = createLiteMessage(message);

                // 转义特殊字符
                String escapedMessage = escapeField(lite.getMessage());

                // 构建消息字符串
                String formatted = String.join(FIELD_DELIMITER,
                        "LORA",
                        lite.getId(),
                        lite.getSenderUid(),
                        escapeField(lite.getSenderCallsign()),
                        lite.getReceiverUid(),
                        escapeField(lite.getReceiverCallsign()),
                        escapedMessage,
                        formatTimestamp(lite.getSentTime()),
                        "text",
                        lite.getOrigin() != null ? lite.getOrigin() : ""
                );

                return formatted.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                Log.e(TAG, "Encoding failed for message: " + message.getId(), e);
                return new byte[0];
            }
        }

        @Override
        public ChatMessageEntity decodeMessage(byte[] payload) {
            try {
                String raw = new String(payload, StandardCharsets.UTF_8);
                String[] parts = splitPreservingEscapes(raw, FIELD_DELIMITER, FIELD_ESCAPE);

                if (parts.length < 9 || !"LORA".equals(parts[0])) {
                    throw new IllegalArgumentException("Invalid LoRa format. Parts: " + parts.length);
                }

                // 解析时间戳
                long timestamp = parseTimestamp(parts[6]);

                return new ChatMessageEntity(
                        unescapeField(parts[1]),  // ID
                        unescapeField(parts[2]),  // Sender UID
                        unescapeField(parts[3]),  // Sender Callsign
                        unescapeField(parts[4]),  // Receiver UID
                        unescapeField(parts[5]),  // Receiver Callsign
                        unescapeField(parts[6]),  // Message
                        String.valueOf(timestamp), // Timestamp
                        unescapeField(parts[7]),  // Message Type
                        unescapeField(parts[8]),  // Origin
                        null  // Raw CoT (将在接收后重建)
                );
            } catch (Exception e) {
                Log.e(TAG, "Decoding failed for payload: " + new String(payload), e);
                return null;
            }
        }

        private ChatMessageEntity createLiteMessage(ChatMessageEntity original) {
            return new ChatMessageEntity(
                    original.getId(),
                    original.getSenderUid(),
                    original.getSenderCallsign(),
                    original.getReceiverUid(),
                    original.getReceiverCallsign(),
                    original.getMessage(),
                    original.getSentTime(),
                    "text",
                    "LoRaOut", // 标记为发送中的LoRa消息
                    null // 清除原始CoT
            );
        }

        // ================= 辅助方法 =================

        private String escapeField(String field) {
            if (field == null) return "";
            return field.replace(FIELD_ESCAPE, FIELD_ESCAPE + FIELD_ESCAPE)
                    .replace(FIELD_DELIMITER, FIELD_ESCAPE + FIELD_DELIMITER);
        }

        private String unescapeField(String field) {
            if (field == null) return "";
            return field.replace(FIELD_ESCAPE + FIELD_DELIMITER, FIELD_DELIMITER)
                    .replace(FIELD_ESCAPE + FIELD_ESCAPE, FIELD_ESCAPE);
        }

        private String formatTimestamp(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) {
                return String.valueOf(System.currentTimeMillis());
            }
            return timestamp;
        }

        private long parseTimestamp(String timestampStr) {
            try {
                return Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                // 尝试解析ISO8601格式
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
                    Date date = sdf.parse(timestampStr);
                    return date != null ? date.getTime() : System.currentTimeMillis();
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to parse timestamp: " + timestampStr, ex);
                    return System.currentTimeMillis();
                }
            }
        }

        private String[] splitPreservingEscapes(String input, String delimiter, String escape) {
            StringTokenizer tokenizer = new StringTokenizer(input, delimiter, true);
            StringBuilder current = new StringBuilder();
            java.util.ArrayList<String> parts = new java.util.ArrayList<>();
            boolean lastWasDelimiter = false;
            boolean escaping = false;

            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();

                if (escaping) {
                    current.append(token);
                    escaping = false;
                } else if (escape.equals(token)) {
                    escaping = true;
                } else if (delimiter.equals(token)) {
                    if (lastWasDelimiter) {
                        // 连续的分隔符表示空字段
                        parts.add(current.toString());
                        current.setLength(0);
                    }
                    lastWasDelimiter = true;
                } else {
                    current.append(token);
                    lastWasDelimiter = false;
                }
            }

            // 添加最后一个字段
            parts.add(current.toString());
            return parts.toArray(new String[0]);
        }
    }
