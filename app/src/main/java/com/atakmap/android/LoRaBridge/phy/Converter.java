package com.atakmap.android.LoRaBridge.phy;

public interface Converter<T> {
    byte[] encode(T entity);
    T decode(byte[] payload);
}