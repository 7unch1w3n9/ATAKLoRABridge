package com.atakmap.android.LoRaBridge.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.LoRaBridge.FlowgraphSetting.ParamsStore;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import android.widget.EditText;
import com.atakmap.android.maps.MapView;

public class SettingDropDown extends DropDownReceiver {

    private final View rootView;
    private final Context ctx;
    private final EditText parameter_1;
    private final EditText parameter_2;
    private final EditText parameter_3;
    private final EditText parameter_4;

    private final Button btnCancel;
    private final Button btnSave;

    public SettingDropDown(MapView mapView, Context context, Activity activity) {
        super(mapView);
        this.ctx = context;
        rootView = LayoutInflater.from(context).inflate(R.layout.flowgraph_setting_layout, null);

        parameter_1 = rootView.findViewById(R.id.parameter_1);
        parameter_2 = rootView.findViewById(R.id.parameter_2);
        parameter_3 = rootView.findViewById(R.id.parameter_3);
        parameter_4 = rootView.findViewById(R.id.parameter_4);
        btnCancel = rootView.findViewById(R.id.btnCancel);
        btnSave   = rootView.findViewById(R.id.btnSave);

        parameter_1.setText(ParamsStore.get(ParamsStore.K_PARAMETER_1));
        parameter_2.setText(ParamsStore.get(ParamsStore.K_PARAMETER_2));
        parameter_3.setText(ParamsStore.get(ParamsStore.K_PARAMETER_3));
        parameter_4.setText(ParamsStore.get(ParamsStore.K_PARAMETER_4));

        btnCancel.setOnClickListener(v -> hideDropDown());


        btnSave.setOnClickListener(v -> {
            String v1 = parameter_1.getText().toString().trim();
            String v2 = parameter_2.getText().toString().trim();
            String v3 = parameter_3.getText().toString().trim();
            String v4 = parameter_4.getText().toString().trim();


            if (v1.isEmpty() || v2.isEmpty() || v3.isEmpty() || v4.isEmpty()) {
                Toast.makeText(ctx, "Please fill all parameters", Toast.LENGTH_SHORT).show();
                return;
            }

            ParamsStore.put(ParamsStore.K_PARAMETER_1, v1);
            ParamsStore.put(ParamsStore.K_PARAMETER_2, v2);
            ParamsStore.put(ParamsStore.K_PARAMETER_3, v3);
            ParamsStore.put(ParamsStore.K_PARAMETER_4, v4);

            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show();
            hideDropDown();
        });
    }

    public void show() {
        showDropDown(rootView, THREE_EIGHTHS_WIDTH, FULL_HEIGHT, FULL_WIDTH, THIRD_HEIGHT);
    }

    @Override
    protected void disposeImpl() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
