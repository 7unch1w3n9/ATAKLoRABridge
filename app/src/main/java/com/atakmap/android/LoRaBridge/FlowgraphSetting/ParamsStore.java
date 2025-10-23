package com.atakmap.android.LoRaBridge.FlowgraphSetting;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Map;

public final class ParamsStore {
    private static final String TAG = "ParamsStore";
    private static final String PREFS_NAME = "plugin_params_store";

    // 与 Flowgraph 对齐的 key
    public static final String K_UDP_BIND        = "udp_bind";
    public static final String K_UDP_SEND_TO     = "udp_send_to";
    public static final String K_CODE_RATE       = "code_rate";
    public static final String K_SF              = "spreading_factor";
    public static final String K_OVERSAMPLING    = "oversampling";
    public static final String K_SYNC_WORD       = "sync_word";
    public static final String K_BANDWIDTH       = "bandwidth";
    public static final String K_SOFT_DECODING   = "soft_decoding";

    private static SharedPreferences sPrefs;

    private ParamsStore() {}

    /** 推荐用 MapView 初始化（与项目其余部分一致） */
    public static void init(MapView mapView) {
        Context app = mapView.getContext().getApplicationContext();
        sPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Prefs file = "
                + app.getApplicationInfo().dataDir
                + "/shared_prefs/" + PREFS_NAME + ".xml");
    }

    /** 也可直接用 Context 初始化 */
    public static void init(Context context) {
        Context app = context.getApplicationContext();
        sPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Prefs file = "
                + app.getApplicationInfo().dataDir
                + "/shared_prefs/" + PREFS_NAME + ".xml");
    }

    private static SharedPreferences prefs() {
        if (sPrefs == null) throw new IllegalStateException("ParamsStore not initialized");
        return sPrefs;
    }

    /** 是否已显式设置（区分“未设置”和“设置为某值”） */
    public static boolean has(String key) {
        return prefs().contains(key);
    }

    /** 读取原始值；若未设置返回 null（不给任何默认） */
    @Nullable
    public static String tryGetRaw(String key) {
        SharedPreferences p = prefs();
        if (!p.contains(key)) return null;
        return p.getString(key, null);
    }

    /** 便捷读取：未设置返回空字符串（仅用于 UI 展示时避免 NPE；JNI 不建议用这个） */
    public static String getOrEmpty(String key) {
        SharedPreferences p = prefs();
        return p.getString(key, "");
    }

    /** 写入字符串值（UI 保存时用） */
    public static void put(String key, String val) {
        prefs().edit().putString(key, val).apply();
    }

    /** 删除某个参数 */
    public static void remove(String key) {
        prefs().edit().remove(key).apply();
    }

    /** 清空所有已设置的参数（不回填默认） */
    public static void clearAll() {
        prefs().edit().clear().apply();
    }

    /** 返回当前“已设置”的键值（仅包含存在于 SharedPreferences 的项） */
    public static Map<String, String> getAllExisting() {
        SharedPreferences p = prefs();
        Map<String, String> out = new HashMap<>();
        Map<String, ?> all = p.getAll();
        if (all != null) {
            for (Map.Entry<String, ?> e : all.entrySet()) {
                Object v = e.getValue();
                if (v instanceof String) {
                    out.put(e.getKey(), (String) v);
                }
            }
        }
        return out;
    }
}
