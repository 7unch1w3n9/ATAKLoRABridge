
package com.atakmap.android.LoRaBridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.LoRaBridge.plugin.databinding.MainLayoutBinding;
import com.atakmap.android.LoRaBridge.recyclerview.RecyclerViewDropDown;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;

public class LoRaBridgeDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = LoRaBridgeDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_PLUGIN";
    private final MainLayoutBinding mainLayoutBinding;
    private  Context pluginContext;
    private final RecyclerViewDropDown recyclerView;

    /**************************** CONSTRUCTOR *****************************/

    public LoRaBridgeDropDownReceiver(final MapView mapView,
                                      final Context context, Activity activity) {
        super(mapView);
        this.pluginContext = context;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        mainLayoutBinding = MainLayoutBinding.inflate(LayoutInflater.from(context));
        recyclerView = new RecyclerViewDropDown(getMapView(), context, activity);
        mainLayoutBinding.btnOpenContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRetain(true);
                recyclerView.show();
            }
        });

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            showDropDown(mainLayoutBinding.getRoot(), HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
