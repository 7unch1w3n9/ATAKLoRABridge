package com.atakmap.android.LoRaBridge.JNI;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.phy.MessageConverter;
import com.atakmap.coremap.log.Log;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class JniMessageConverter implements MessageConverter {
    private static final String TAG = "JniMessageConverter";
    private static final String FIELD_DELIMITER = "|";
    private static final String FIELD_ESCAPE = "\\";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Override
    public byte[] encodeMessage(ChatMessageEntity message) {
        try {

            ChatMessageEntity lite = createLiteMessage(message);

            String escapedMessage = escapeField(lite.getMessage());

            String formatted = String.join(FIELD_DELIMITER,
                    "JNI",
                    lite.getId(),
                    escapeField(lite.getSenderUid()),
                    escapeField(lite.getSenderCallsign()),
                    escapeField(lite.getReceiverUid()),
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

            // 添加详细日志
            Log.d(TAG, "Split into " + parts.length + " parts: " + Arrays.toString(parts));

            if (parts.length < 10 || !"JNI".equals(parts[0])) {
                throw new IllegalArgumentException("Expected 10 parts, got: " + parts.length);
            }

            // 修正字段索引
            long timestamp = parseTimestamp(parts[7]);  // 索引7是时间戳

            return new ChatMessageEntity(
                    unescapeField(parts[1]),  // ID
                    unescapeField(parts[2]),  // Sender UID
                    unescapeField(parts[3]),  // Sender Callsign
                    unescapeField(parts[4]),  // Receiver UID
                    unescapeField(parts[5]),  // Receiver Callsign
                    unescapeField(parts[6]),  // Message
                    String.valueOf(timestamp),
                    unescapeField(parts[8]),  // Message Type
                    unescapeField(parts[9]),  // Origin
                    null
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
                "JNI",
                null
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

    private String[] splitPreservingEscapes(String input, String delimiter, String escape) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char delimChar = delimiter.charAt(0);
        char escapeChar = escape.charAt(0);
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

    private long parseTimestamp(String timestampStr) {
        // 先尝试直接解析为毫秒数
        if (timestampStr.matches("\\d+")) {
            try {
                return Long.parseLong(timestampStr);
            } catch (NumberFormatException ignore) {}
        }

        // 尝试ISO 8601格式
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(timestampStr).getTime();
        } catch (ParseException e) {
            Log.w(TAG, "Using current time for invalid timestamp: " + timestampStr);
            return System.currentTimeMillis();
        }
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return String.valueOf(System.currentTimeMillis());
        }
        return timestamp;
    }

}
