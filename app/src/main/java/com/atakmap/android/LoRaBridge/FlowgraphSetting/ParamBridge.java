package com.atakmap.android.LoRaBridge.FlowgraphSetting;

import androidx.annotation.Nullable;

public final class ParamBridge {
    private ParamBridge() {}

    @Nullable
    public static String tryGet(String key) {
        String v = ParamsStore.tryGetRaw(key);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
