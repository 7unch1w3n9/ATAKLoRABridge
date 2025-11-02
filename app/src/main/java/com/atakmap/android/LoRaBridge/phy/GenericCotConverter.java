package com.atakmap.android.LoRaBridge.phy;

import com.atakmap.android.LoRaBridge.Database.GenericCotEntity;

public interface GenericCotConverter extends Converter<GenericCotEntity> {
    default byte[] encodeCot(GenericCotEntity e) { return encode(e); }
    default GenericCotEntity decodeCot(byte[] p) { return decode(p); }
}