package com.atakmap.android.LoRaBridge.FlowgraphSetting;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Map;

public final class ParamsStore {
    private static final String TAG = "ParamsStore";
    private static final String PREFS_NAME = "plugin_params_store";

    public static final String K_PARAMETER_1  = "parameter_1";
    public static final String K_PARAMETER_2  = "parameter_2";
    public static final String K_PARAMETER_3  = "parameter_3";
    public static final String K_PARAMETER_4  = "parameter_4";

    // 默认值（按需修改）
    private static final Map<String, String> DEFAULTS = new HashMap<String, String>() {{
        put(K_PARAMETER_1, "868000000");   // parameter 1
        put(K_PARAMETER_2, "2000000");     // parameter 2
        put(K_PARAMETER_3, "30");          // parameter 3
        put(K_PARAMETER_4, "30");          // parameter 4
    }};

    private static SharedPreferences sPrefs;

    private ParamsStore() {}

    public static void init(MapView mapView) {
        Context app = mapView.getContext().getApplicationContext();
        sPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Prefs file = "
                + app.getApplicationInfo().dataDir
                + "/shared_prefs/" + PREFS_NAME + ".xml");
        applyDefaultsIfMissing();
    }

    private static SharedPreferences prefs() {
        if (sPrefs == null) throw new IllegalStateException("ParamsStore not initialized");
        return sPrefs;
    }

    /** 若不存在则填充默认值（不覆盖已有值） */
    public static void applyDefaultsIfMissing() {
        SharedPreferences p = prefs();
        SharedPreferences.Editor e = p.edit();
        boolean changed = false;
        for (Map.Entry<String, String> kv : DEFAULTS.entrySet()) {
            if (!p.contains(kv.getKey())) {
                e.putString(kv.getKey(), kv.getValue());
                changed = true;
            }
        }
        if (changed) e.apply();
    }


    public static String get(String key) {
        SharedPreferences p = prefs();
        String def = DEFAULTS.get(key);
        return p.getString(key, def != null ? def : "");
    }


    public static void put(String key, String val) {
        prefs().edit().putString(key, val).apply();
    }

    public static Map<String, String> getAll() {
        SharedPreferences p = prefs();
        Map<String, String> out = new HashMap<>();
        for (String k : DEFAULTS.keySet()) {
            out.put(k, p.getString(k, DEFAULTS.get(k)));
        }
        return out;
    }

    public static void putAll(Map<String, String> values) {
        SharedPreferences.Editor e = prefs().edit();
        for (Map.Entry<String, String> kv : values.entrySet()) {
            e.putString(kv.getKey(), kv.getValue());
        }
        e.apply();
    }

    public static void resetToDefaults() {
        SharedPreferences.Editor e = prefs().edit();
        e.clear();
        e.apply();
        applyDefaultsIfMissing();
    }
}
