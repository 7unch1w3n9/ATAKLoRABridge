package com.atakmap.android.LoRaBridge.recyclerview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import com.atakmap.android.LoRaBridge.FlowgraphSetting.ParamsStore;
import com.atakmap.android.LoRaBridge.JNI.FlowgraphNative;
import com.atakmap.android.LoRaBridge.JNI.UsbHackrfManager;
import com.atakmap.android.LoRaBridge.JNI.UsbHackrfManagerHolder;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SettingDropDown extends DropDownReceiver {

    private final View rootView;
    private final Context ctx;
    private final Activity activity;

    private static final Set<String> CORE_KEYS = new HashSet<>(Arrays.asList(
            ParamsStore.K_UDP_BIND,
            ParamsStore.K_UDP_SEND_TO,
            ParamsStore.K_CODE_RATE,
            ParamsStore.K_SF,
            ParamsStore.K_OVERSAMPLING,
            ParamsStore.K_SYNC_WORD,
            ParamsStore.K_BANDWIDTH,
            ParamsStore.K_SOFT_DECODING
    ));

    // UI
    private final EditText etUdpBind;
    private final EditText etUdpSendTo;
    private final Spinner  spCodeRate;
    private final Spinner  spSpreadingFactor;
    private final Spinner  spOversampling;
    private final Spinner  spSyncWord;
    private final Switch   swSoftDecoding;

    private final Button btnAddParam;
    private final Button btnCancel;
    private final Button btnSave;
    private LinearLayout extraParamsContainer;


    public SettingDropDown(MapView mapView, Context context, Activity activity) {
        super(mapView);
        this.ctx = context;
        this.activity = activity;
        this.rootView = LayoutInflater.from(context).inflate(R.layout.flowgraph_setting_layout, null);

        // 绑定固定控件
        etUdpBind         = rootView.findViewById(R.id.etUdpBind);
        etUdpSendTo       = rootView.findViewById(R.id.etUdpSendTo);
        spCodeRate        = rootView.findViewById(R.id.spCodeRate);
        spSpreadingFactor = rootView.findViewById(R.id.spSpreadingFactor);
        spOversampling    = rootView.findViewById(R.id.spOversampling);
        spSyncWord        = rootView.findViewById(R.id.spSyncWord);
        swSoftDecoding    = rootView.findViewById(R.id.swSoftDecoding);

        btnAddParam       = rootView.findViewById(R.id.btnAddParam);
        btnCancel         = rootView.findViewById(R.id.btnCancel);
        btnSave           = rootView.findViewById(R.id.btnSave);

        extraParamsContainer = rootView.findViewById(R.id.extraParamsContainer);

        setupSpinner(spCodeRate,        R.array.code_rate_values);
        setupSpinner(spSpreadingFactor, R.array.spreading_factor_values);
        setupSpinner(spOversampling,    R.array.oversampling_values);
        setupSpinner(spSyncWord,        R.array.sync_word_values);

        final String vUdpBind   = ParamsStore.tryGetRaw(ParamsStore.K_UDP_BIND);
        final String vUdpSendTo = ParamsStore.tryGetRaw(ParamsStore.K_UDP_SEND_TO);
        if (vUdpBind != null)   etUdpBind.setText(vUdpBind);
        if (vUdpSendTo != null) etUdpSendTo.setText(vUdpSendTo);

        selectSpinnerByValue(spCodeRate,        ParamsStore.tryGetRaw(ParamsStore.K_CODE_RATE));
        selectSpinnerByValue(spSpreadingFactor, ParamsStore.tryGetRaw(ParamsStore.K_SF));
        selectSpinnerByValue(spOversampling,    ParamsStore.tryGetRaw(ParamsStore.K_OVERSAMPLING));
        selectSpinnerByValue(spSyncWord,        ParamsStore.tryGetRaw(ParamsStore.K_SYNC_WORD));

        final String soft = ParamsStore.tryGetRaw(ParamsStore.K_SOFT_DECODING);
        if (soft != null) swSoftDecoding.setChecked(isTrue(soft));

        Map<String, String> all = ParamsStore.getAllExisting();
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (!CORE_KEYS.contains(e.getKey())) {
                addExtraRow(e.getKey(), e.getValue());
            }
        }

        btnCancel.setOnClickListener(v -> hideDropDown());

        btnAddParam.setOnClickListener(v -> addExtraRow(null, null));

        btnSave.setOnClickListener(v -> {
            String sUdpBind   = etUdpBind.getText().toString().trim();
            String sUdpSendTo = etUdpSendTo.getText().toString().trim();
            String sCr        = spCodeRate.getSelectedItem().toString();
            String sSf        = spSpreadingFactor.getSelectedItem().toString();
            String sOs        = spOversampling.getSelectedItem().toString();
            String sSw        = spSyncWord.getSelectedItem().toString();
            boolean bSoft     = swSoftDecoding.isChecked();

            if (!sUdpBind.isEmpty() && !isEndpoint(sUdpBind)) {
                toast("udp_bind format should be host:port, e.g. 127.0.0.1:1382");
                return;
            }
            if (!sUdpSendTo.isEmpty() && !isEndpoint(sUdpSendTo)) {
                toast("udp_send_to format should be host:port, e.g. 127.0.0.1:1383");
                return;
            }

            saveOrRemove(ParamsStore.K_UDP_BIND,    sUdpBind);
            saveOrRemove(ParamsStore.K_UDP_SEND_TO, sUdpSendTo);
            ParamsStore.put(ParamsStore.K_CODE_RATE,     sCr);
            ParamsStore.put(ParamsStore.K_SF,            sSf);
            ParamsStore.put(ParamsStore.K_OVERSAMPLING,  sOs);
            ParamsStore.put(ParamsStore.K_SYNC_WORD,     sSw);
            ParamsStore.put(ParamsStore.K_SOFT_DECODING, String.valueOf(bSoft));

            // 保存动态参数（含校验）
            try {
                saveDynamicParamsOrFail();
            } catch (IllegalArgumentException ex) {
                toast(ex.getMessage());
                return;
            }

            toast("Saved");
            hideDropDown();
            UsbHackrfManager mgr = UsbHackrfManagerHolder.get();
            if (mgr != null) {
                mgr.probeNow();
            } else {
                android.util.Log.w("SettingDropDown", "UsbHackrfManager is null; cannot re-probe");
                toast("USB manager not ready");
            }

        });
    }
    private void addExtraRow(String keyPrefill, String valPrefill) {
        View row = LayoutInflater.from(rootView.getContext())
                .inflate(R.layout.row_param_inline, extraParamsContainer, false);

        EditText etKey = row.findViewById(R.id.etParamKeyInline);
        EditText etVal = row.findViewById(R.id.etParamValueInline);
        Button btnRemove = row.findViewById(R.id.btnRemoveParamInline);

        if (keyPrefill != null) etKey.setText(keyPrefill);
        if (valPrefill != null) etVal.setText(valPrefill);

        btnRemove.setOnClickListener(v -> extraParamsContainer.removeView(row));
        extraParamsContainer.addView(row);
    }

    private void saveDynamicParamsOrFail() throws IllegalArgumentException {
        Map<String, String> newMap = new HashMap<>();
        int n = extraParamsContainer.getChildCount();
        for (int i = 0; i < n; i++) {
            View row = extraParamsContainer.getChildAt(i);
            EditText etKey = row.findViewById(R.id.etParamKeyInline);
            EditText etVal = row.findViewById(R.id.etParamValueInline);

            String key = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();

            if (key.isEmpty()) {
                throw new IllegalArgumentException("Custom parameter name cannot be empty.");
            }
            if (!key.matches("[A-Za-z0-9_\\-.]+")) {
                throw new IllegalArgumentException("Invalid name '" + key + "': only letters, digits, _ - . allowed.");
            }
            if (CORE_KEYS.contains(key)) {
                throw new IllegalArgumentException("Parameter '" + key + "' is a core parameter. Please use the control above.");
            }
            if (newMap.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate custom parameter name: '" + key + "'.");
            }
            if (!val.isEmpty()) newMap.put(key, val);
        }


        Map<String, String> existing = ParamsStore.getAllExisting();
        Set<String> existingCustom = new HashSet<>();
        for (String k : existing.keySet()) {
            if (!CORE_KEYS.contains(k)) existingCustom.add(k);
        }


        for (String oldKey : existingCustom) {
            if (!newMap.containsKey(oldKey)) {
                ParamsStore.remove(oldKey);
            }
        }


        for (Map.Entry<String, String> e : newMap.entrySet()) {
            ParamsStore.put(e.getKey(), e.getValue());
        }
    }

    private void saveOrRemove(String key, String v) {
        if (v == null || v.isEmpty()) {
            ParamsStore.remove(key);
        } else {
            ParamsStore.put(key, v);
        }
    }

    private void selectSpinnerByValue(Spinner spinner, String valueOrNull) {
        if (valueOrNull == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && valueOrNull.equals(item.toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private boolean isEndpoint(String ep) {
        int c = 0;
        for (int i = 0; i < ep.length(); i++) if (ep.charAt(i) == ':') c++;
        if (c != 1) return false;
        String[] parts = ep.split(":");
        if (parts.length != 2) return false;
        try {
            int port = Integer.parseInt(parts[1]);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private void setupSpinner(Spinner sp, int arrayRes) {
        ArrayAdapter<CharSequence> ad =
                new ArrayAdapter<>(rootView.getContext(),
                        R.layout.spinner_item,
                        ctx.getResources().getStringArray(arrayRes));
        ad.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp.setAdapter(ad);
    }

    private boolean isTrue(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private void toast(String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public void show() {
        showDropDown(rootView, THREE_EIGHTHS_WIDTH, FULL_HEIGHT, FULL_WIDTH, THIRD_HEIGHT);
    }

    @Override protected void disposeImpl() { }

    @Override public void onReceive(Context context, Intent intent) { }
}
