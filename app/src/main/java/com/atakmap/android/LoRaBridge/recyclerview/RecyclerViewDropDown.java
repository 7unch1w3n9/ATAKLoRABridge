
package com.atakmap.android.LoRaBridge.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.atakmap.android.LoRaBridge.Contacts.PluginContact;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.time.TimeListener;
import com.atakmap.android.util.time.TimeViewUpdater;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A drop-down menu that demonstrates use of a RecyclerView to show a list of content
 */
public class RecyclerViewDropDown extends DropDownReceiver implements
        MapEventDispatcher.MapEventDispatchListener,
        View.OnClickListener, TimeListener {

    private final MapView _mapView;
    private final Context _plugin;
    private final TimeViewUpdater _timeUpdater;
    private final View _view;
    private final RecyclerView _rView;
    private final Map<String, ChatDropDown> activeChats = new HashMap<>();
    private final ContactAdapter adapter;

    public RecyclerViewDropDown(MapView mapView, Context plugin, Activity activity) {
        super(mapView);
        _mapView = mapView;
        _plugin = plugin;

        _view = LayoutInflater.from(_plugin).inflate(R.layout.pane_contacts,
                mapView, false);
        _rView = _view.findViewById(R.id.rView);
        _rView.setLayoutManager(new LinearLayoutManager(_plugin,
                LinearLayoutManager.VERTICAL, false));

        adapter = new ContactAdapter(_plugin, new ContactAdapter.OnContactClickListener() {
            @Override
            public void onContactClick(PluginContact contact) {
                handleContactClick(contact, activity);
            }
        });
        _rView.setAdapter(adapter);

        _view.findViewById(R.id.btnAddContact).setOnClickListener(v -> {
            addNewContact();
        });

        _view.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            refreshContacts();
        });

        // Add map listeners
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_ADDED, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REMOVED, this);

        // Update the time ago for all the users each second
        _timeUpdater = new TimeViewUpdater(_mapView, 1000);
        _timeUpdater.register(this);
    }
    private void handleContactClick(PluginContact contact, Activity activity) {
        String uid = contact.getId();
        if (activeChats.containsKey(uid)) {
            Objects.requireNonNull(activeChats.get(uid)).show();
        } else {
            ChatDropDown chatDropDown = new ChatDropDown(_mapView, _plugin, contact, activity);
            activeChats.put(uid, chatDropDown);
            chatDropDown.show();
        }
    }
    private void refreshContacts() {
        adapter.refreshContacts();
        Log.d("ContactRefresh", "Contacts refreshed");
    }

    private void addNewContact() {
        PluginContact newContact = new PluginContact(
                null,
                "New Contact",
                "DEV-" + System.currentTimeMillis()
        );
        newContact.setLocal(true);
        adapter.addContact(newContact);
        adapter.refreshContacts();
        Log.d("ContactAdd", "Added new contact: " + newContact.getCallsign());
    }



    @Override
    public void disposeImpl() {
        // Remove map listeners
        _mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.ITEM_ADDED, this);
        _mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.ITEM_REMOVED, this);
        _timeUpdater.unregister(this);
    }

    @Override
    public void onTimeChanged(CoordinatedTime ot, CoordinatedTime nt) {
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
    }

    @Override
    public void onClick(View v) {
        // Switch user list between vertical, horizontal, and grid mode
    }

    @Override
    public void onMapEvent(MapEvent event) {
    }

    public void show() {
        adapter.fullSyncAndRefresh();
        showDropDown(_view, THREE_EIGHTHS_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                THIRD_HEIGHT);
    }
}
