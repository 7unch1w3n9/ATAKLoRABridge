package com.atakmap.android.LoRaBridge.GenericMessage;

import android.content.Context;

import com.atakmap.android.LoRaBridge.Database.GenericCotEntity;
import com.atakmap.android.LoRaBridge.Database.GenericCotRepository;
import com.atakmap.android.LoRaBridge.phy.GenericCotConverter;
import com.atakmap.android.LoRaBridge.phy.UdpManager;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 通用 CoT（非 b-t-f）双向同步服务（ATAK <-> PHY）
 * - ATAK 侧收到非聊天 CoT → 编码（EXI/自定义）→ UDP 发出
 * - PHY 侧收到字节流 → 还原 CoT → 打 __lora 标签 → 分发到 ATAK
 *
 * 依赖：
 *  - GenericCotRepository（Room 持久化）
 *  - GenericCotConverter（线缆编解码接口），默认用 LoRaCotConverter 实现
 *  - UdpManager：需要支持 (chatHandler=null, cotHandler=...) 的构造，并在内部按头部路由到 cotHandler
 */
public class CotSyncService {
    private static final String TAG = "CotSyncService";
    private static CotSyncService instance;

    private final GenericCotRepository repo;
    private final GenericCotConverter cotConverter;  // EXI 编/解码器（头：LORA_COTX|...）
    private final UdpManager udp = UdpManager.getInstance();
    private final GenericTracker tracker = new GenericTracker(); // 内嵌去重器

    private CotSyncService(Context ctx) {
        this.repo = new GenericCotRepository(ctx);
        this.cotConverter = new LoRaGenericCotConverter(); // 同包内实现类，使用 EXI/自定义线缆协议
        udp.setCotHandler(this::handlePhyPayload);
    }

    public static synchronized CotSyncService getInstance(Context ctx) {
        if (instance == null) {
            instance = new CotSyncService(ctx);
        }
        return instance;
    }

    /** 入口：处理来自 ATAK 的“非聊天 CoT” */
    public void processIncomingCotFromAtak(CotEvent event) {
        if (event == null || !event.isValid()) return;

        if ("b-t-f".equals(event.getType())) return; // 聊天留给 MessageSyncService

        if (hasLoopTag(event)) {
            Log.d(TAG, "Skip looped CoT (__lora present)");
            return;
        }

        GenericCotEntity e = GenericCotFactory.fromCot(event, "Plugin");
        if (e == null) return;

        if (tracker.seen(e.id)) return;
        tracker.mark(e.id);

        // 入库（去重）
        repo.insertIfAbsent(e);

        // 打 __lora 标签避免回环
        CotEvent marked = addLoopTag(event, "Plugin", e.id);

        // (2) 发往 PHY（EXI/自定义编码）
        try {
            byte[] body = cotConverter.encodeCot(e);
            udp.sendCot(body);
        } catch (Exception ex) {
            Log.e(TAG, "sendToFlowgraph failed", ex);
        }
    }

    /** 从 PHY/Flowgraph 收到的“通用 CoT”字节流 */
    private void handlePhyPayload(byte[] payload) {
        try {
            GenericCotEntity e = cotConverter.decodeCot(payload);
            if (e == null) return;

            if ("b-t-f-d".equals(e.getType())|| "b-t-f-r".equals(e.getType())|| "a-f-G-U-C".equals(e.getType())) {
                Log.d(TAG, "Skip non a-h-g CoT: " + e.getType());
                return;
            }
            if (tracker.seen(e.id)) return;
            tracker.mark(e.id);

            // 入库（去重）
            repo.insertIfAbsent(e);

            // 解析 XML → CotEvent，并打上 __lora 标签
            CotEvent ev = parseXmlToCot(e.cotRawXml);
            if (ev == null) return;
            ev = addLoopTag(ev, "PHY", e.id);

            // 分发到 ATAK（Marker/图形/任务等会出现或更新）
            CotMapComponent.getInternalDispatcher().dispatch(ev);
        } catch (Throwable t) {
            Log.e(TAG, "handlePhyPayload decode/dispatch error", t);
        }
    }

    // ------------------ 工具方法 ------------------

    private static boolean hasLoopTag(CotEvent ev) {
        CotDetail d = ev.getDetail();
        return d != null && d.getFirstChildByName(0, "__lora") != null;
    }

    private static CotEvent addLoopTag(CotEvent ev, String origin, String id) {
        CotDetail d = ev.getDetail();
        if (d == null) {
            d = new CotDetail("detail");
            ev.setDetail(d);
        }
        CotDetail l = new CotDetail("__lora");
        l.setAttribute("origin", origin);
        l.setAttribute("originalId", id);
        d.addChild(l);
        return ev;
    }

    private static CotEvent parseXmlToCot(String xml) {
        try {
            return CotEvent.parse(xml);
        } catch (Throwable t) {
            return null;
        }
    }


    public static class GenericTracker {
        private final Set<String> processed = new HashSet<>();

        public synchronized boolean seen(String id) {
            return processed.contains(id);
        }

        public synchronized void mark(String id) {
            if (processed.size() > 2000) processed.clear();
            processed.add(id);
        }
    }
}
