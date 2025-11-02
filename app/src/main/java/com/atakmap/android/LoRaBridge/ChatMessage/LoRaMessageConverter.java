
    package com.atakmap.android.LoRaBridge.ChatMessage;

    import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
    import com.atakmap.android.LoRaBridge.phy.MessageConverter;
    import com.atakmap.coremap.log.Log;

    import java.nio.charset.StandardCharsets;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;
    import java.util.TimeZone;

    public class LoRaMessageConverter implements MessageConverter {
        private static final String TAG = "LoRaMessageConverter";
        private static final String FIELD_DELIMITER = "|";
        private static final String FIELD_ESCAPE = "\\";
        private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

        @Override
        public byte[] encode(ChatMessageEntity message) {
            try {
                // 创建精简版本的消息（清除不必要字段）
                ChatMessageEntity lite = createLiteMessage(message);

                // 转义特殊字符
                String escapedMessage = escapeField(lite.getMessage());

                // 构建消息字符串
                String formatted = String.join(FIELD_DELIMITER,
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
        public ChatMessageEntity decode(byte[] payload) {
            try {
                String raw = new String(payload, StandardCharsets.UTF_8);
                String[] parts = splitPreservingEscapes(raw);

                if (parts.length < 9 ) {
                    throw new IllegalArgumentException("Invalid LoRa format. Parts: " + parts.length);
                }

                String id               = unescapeField(parts[0]);
                String senderUid        = unescapeField(parts[1]);
                String senderCallsign   = unescapeField(parts[2]);
                String receiverUid      = unescapeField(parts[3]);
                String receiverCallsign = unescapeField(parts[4]);
                String messageBody      = unescapeField(parts[5]);
                String tsStr            = unescapeField(parts[6]);
                String msgType          = unescapeField(parts[7]);
                String origin           = unescapeField(parts[8]);

                String tsIso = parseTimestamp(tsStr);

                return new ChatMessageEntity(
                        id,
                        senderUid,
                        senderCallsign,
                        receiverUid,
                        receiverCallsign,
                        messageBody,
                        tsIso,
                        msgType,
                        origin,
                        null
                );
            } catch (Exception e) {
                Log.e(TAG, "Decoding failed for payload: " + new String(payload, StandardCharsets.UTF_8), e);
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

        private String parseTimestamp(String timestampStr) {
            try {
                // 输出格式：UTC ISO 带 'Z'
                SimpleDateFormat out = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
                out.setTimeZone(TimeZone.getTimeZone("UTC"));

                if (timestampStr == null || timestampStr.isEmpty()) {
                    return out.format(new Date(System.currentTimeMillis()));
                }

                // 1) 纯数字：按毫秒解析 -> 输出 ISO
                if (timestampStr.matches("\\d+")) {
                    long ms = Long.parseLong(timestampStr);
                    return out.format(new Date(ms));
                }

                // 2) 归一化 ISO8601：支持末尾 Z 或 +hh:mm
                String s = timestampStr;
                if (s.endsWith("Z")) {
                    s = s.replace("Z", "+0000");                  // ...SSS+0000
                } else {
                    s = s.replaceAll(":(?=[0-9]{2}$)", "");       // +01:00 -> +0100
                }
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
                in.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = in.parse(s);
                return d != null ? out.format(d) : out.format(new Date(System.currentTimeMillis()));
            } catch (Exception e) {
                Log.w(TAG, "parseTimestamp fallback, input=" + timestampStr);
                SimpleDateFormat out = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
                out.setTimeZone(TimeZone.getTimeZone("UTC"));
                return out.format(new Date(System.currentTimeMillis()));
            }
        }

        private String[] splitPreservingEscapes(String input) {

            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            char delimChar = LoRaMessageConverter.FIELD_DELIMITER.charAt(0);
            char escapeChar = LoRaMessageConverter.FIELD_ESCAPE.charAt(0);
            boolean nextEscaped = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (nextEscaped) {
                    // 转义模式下追加任何字符
                    current.append(c);
                    nextEscaped = false;
                } else if (c == escapeChar) {
                    // 标记下一个字符需要转义
                    nextEscaped = true;
                } else if (c == delimChar) {
                    // 遇到未转义的分隔符
                    parts.add(current.toString());
                    current.setLength(0);
                } else {
                    // 普通字符
                    current.append(c);
                }
            }

            // 添加最后一个字段
            parts.add(current.toString());
            return parts.toArray(new String[0]);
        }
    }
