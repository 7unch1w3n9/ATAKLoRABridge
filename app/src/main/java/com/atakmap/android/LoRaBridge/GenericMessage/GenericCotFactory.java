package com.atakmap.android.LoRaBridge.GenericMessage;

import com.atakmap.android.LoRaBridge.Database.GenericCotEntity;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.security.MessageDigest;

/**
 * GenericFactory
 * 把 ATAK 的 CotEvent 对象转换成可存储/发送的 GenericCotEntity。
 */
public final class GenericCotFactory {

    private GenericCotFactory() {
    }

    /**
     * 从 CotEvent 生成 GenericCotEntity。
     * @param event   原始 CoT 事件（可能是 Marker、任务、图层等）
     * @param origin  来源标识（Plugin / PHY / Geo）
     * @return        可持久化/编码的 GenericCotEntity
     */
    public static GenericCotEntity fromCot(CotEvent event, String origin) {
        try {
            String uid = event.getUID();
            String type = event.getType();
            String timeIso = event.getTime() != null
                    ? event.getTime().toString()
                    : new CoordinatedTime().toString();

            // XML 原文，去掉多余空白
            String xml = minify(event.toString());

            // 生成唯一 ID（SHA1(uid + type + time + xml.length)）
            String id = sha1(uid + "|" + type + "|" + timeIso + "|" + xml.length());

            return new GenericCotEntity(
                    id,
                    uid,
                    type,
                    timeIso,
                    origin,
                    xml,
                    null // exiBytes 暂时为空，发送时再由 EXI 工具生成
            );
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    // ---- 内部辅助方法 ----

    private static String sha1(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] b = md.digest(s.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static String minify(String xml) {
        if (xml == null) return "";
        // 压掉多余空格
        return xml.replaceAll(">\\s+<", "><").trim();
    }
}
