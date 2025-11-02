package com.atakmap.android.LoRaBridge.GenericMessage;

import com.atakmap.android.LoRaBridge.Database.GenericCotEntity;
import com.atakmap.android.LoRaBridge.phy.GenericCotConverter;

public class LoRaGenericCotConverter implements GenericCotConverter{
    private static final String TAG = "LoRaGenericCotConverter";
    private static final String DELIM = "|";

    @Override
    public byte[] encode(GenericCotEntity e){
        try {
            byte[] exi = ExiUtils.toExi(e.cotRawXml); // §3.1
            String head = String.join(DELIM,
                    e.id,
                    e.uid,
                    e.type,
                    e.timeIso,
                    e.origin != null ? e.origin : ""
            );
            return (head + DELIM + android.util.Base64.encodeToString(exi, android.util.Base64.NO_WRAP))
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex){
            com.atakmap.coremap.log.Log.e(TAG, "encodeCot failed", ex);
            return new byte[0];
        }
    }

    @Override
    public GenericCotEntity decode(byte[] payload){
        try {
            String s = new String(payload, java.nio.charset.StandardCharsets.UTF_8);

            final String HDR = "LORA_COTX|";
            final boolean hasHead = s.startsWith(HDR);

            String[] parts = s.split("\\|", -1);

            int off = hasHead ? 1 : 0;
            if (parts.length - off < 6) {
                android.util.Log.e(TAG, "decodeCot: not enough fields, raw=" + s);
                return null;
            }

            String id     = parts[off + 0];
            String uid    = parts[off + 1];
            String type   = parts[off + 2];
            String time   = parts[off + 3];
            String origin = parts[off + 4];
            String b64    = parts[off + 5];

            String cleaned = b64.replaceAll("\\s+", "");
            String bads = cleaned.replaceAll("[A-Za-z0-9+/=]", "");
            if (!bads.isEmpty()) {
                android.util.Log.w(TAG, "decodeCot: non-base64 chars found: [" + bads + "]");
            }
            byte[] exi = android.util.Base64.decode(cleaned, android.util.Base64.NO_WRAP);
            String xml = ExiUtils.fromExi(exi);

            return new GenericCotEntity(id, uid, type, time, origin, xml, exi);
        } catch (Exception ex){
            android.util.Log.e(TAG, "decodeCot failed", ex);
            return null;
        }
    }
}