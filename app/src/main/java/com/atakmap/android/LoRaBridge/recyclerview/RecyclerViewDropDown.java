
package com.atakmap.android.LoRaBridge.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.atakmap.android.LoRaBridge.Contacts.ContactStore;
import com.atakmap.android.LoRaBridge.Contacts.PluginContact;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.LoRaBridge.Database.ChatViewModel;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A drop-down menu that demonstrates use of a RecyclerView to show a list of content
 */
public class RecyclerViewDropDown extends DropDownReceiver implements
        MapEventDispatcher.MapEventDispatchListener,
        View.OnClickListener, TimeListener {
    private static final String TAG = "ContactsDropDown";
    private final MapView mapView;
    private final Context pluginContext;
    private final Activity hostActivity;
    private final TimeViewUpdater timeUpdater;
    private final View rootView;
    private final RecyclerView contactsRecycler;
    private final Map<String, ChatDropDown> chatWindows = new HashMap<>();
    private final ContactAdapter contactsAdapter;
    private SettingDropDown settingDropDown;
    private ChatViewModel viewModel;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    public RecyclerViewDropDown(MapView mapView, Context context, Activity activity) {
        super(mapView);
        this.mapView = mapView;
        this.pluginContext = context;
        this.hostActivity = activity;
        viewModel = new ViewModelProvider((ViewModelStoreOwner)activity).get(ChatViewModel.class);
        rootView = LayoutInflater.from(pluginContext).inflate(R.layout.pane_contacts, mapView, false);
        contactsRecycler = rootView.findViewById(R.id.rView);
        contactsRecycler.setLayoutManager(
                new LinearLayoutManager(pluginContext, LinearLayoutManager.VERTICAL, false)
        );

        contactsAdapter = new ContactAdapter(
                pluginContext,
                this::openChat,
                contact -> {
                    viewModel.deleteMessagesForContact(contact.getId());
                        ContactStore.deleteContact(contact.getId());

                    Toast.makeText(
                            pluginContext,
                            "Deleted：" + contact.getCallsign(),
                            Toast.LENGTH_SHORT
                    ).show();
                },
                activity
        );
        contactsRecycler.setAdapter(contactsAdapter);

        rootView.findViewById(R.id.btnAddContact).setOnClickListener(v -> addNewContact());
        rootView.findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshContacts());
        View btnSetting = rootView.findViewById(R.id.btnSetting);
        if (btnSetting != null) {
            btnSetting.setOnClickListener(v -> openSettings());
        } else {
            Log.w(TAG, "btnSetting not found in pane_contacts layout");
        }

        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_ADDED, this);
        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_REMOVED, this);

        // Update the time ago for all the users each second
        timeUpdater = new TimeViewUpdater(mapView, 1000);
        timeUpdater.register(this);
    }
    private void openChat(PluginContact contact) {
        String uid = contact.getId();
        ChatDropDown win = chatWindows.get(uid);
        if (win == null) {
            win = new ChatDropDown(mapView, pluginContext, contact, hostActivity);
            chatWindows.put(uid, win);
        }
        Objects.requireNonNull(win).show();
    }
    private void refreshContacts() {
        contactsAdapter.refreshContacts();
        Log.d("ContactRefresh", "Contacts refreshed");
    }
    private void addNewContact() {
        PluginContact newContact = new PluginContact(
                null,
                "New Contact",
                "DEV-" + System.currentTimeMillis()
        );
        newContact.setLocal(true);
        contactsAdapter.addContact(newContact);
        contactsAdapter.refreshContacts();
        Log.d("ContactAdd", "Added new contact: " + newContact.getCallsign());
    }
    private void openSettings() {
        if (settingDropDown == null) {
            settingDropDown = new SettingDropDown(mapView, pluginContext, hostActivity);
        }
        settingDropDown.show();
    }


    @Override
    public void disposeImpl() {
        // Remove map listeners
        mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.ITEM_ADDED, this);
        mapView.getMapEventDispatcher().removeMapEventListener(
                MapEvent.ITEM_REMOVED, this);
        timeUpdater.unregister(this);
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
        contactsAdapter.fullSyncAndRefresh();
        showDropDown(rootView, THREE_EIGHTHS_WIDTH, FULL_HEIGHT, FULL_WIDTH, THIRD_HEIGHT);
    }
}
