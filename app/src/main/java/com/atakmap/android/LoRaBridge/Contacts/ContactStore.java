package com.atakmap.android.LoRaBridge.Contacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ContactStore {
    private static final String TAG = "ContactStore";
    private static final String PREFS_NAME = "plugin_contacts_store";
    private static final String CONTACT_IDS_KEY = "contact_ids";
    private static final String CONTACT_PREFIX = "contact_";
    private static MapView mapView;
    private static SharedPreferences sPrefs;

    public static void init(MapView mapView1) {
        mapView = mapView1;
        sPrefs = mapView.getContext().getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Prefs file = "
                + mapView.getContext().getApplicationContext().getApplicationInfo().dataDir
                + "/shared_prefs/" + PREFS_NAME + ".xml");
    }


    private static SharedPreferences prefs() {
        if (sPrefs == null) throw new IllegalStateException("ContactStore not initialized");
        return sPrefs;
    }


    public static void saveContact(PluginContact contact) {
        SharedPreferences p = prefs();
        SharedPreferences.Editor editor = p.edit();

        if (contact.getId() == null || contact.getId().isEmpty()) {
            contact.setId(generateUniqueId());
        }

        String contactJson = contact.toJson();
        editor.putString(CONTACT_PREFIX + contact.getId(), contactJson);

        Set<String> contactIds = new HashSet<>(Objects.requireNonNull(p.getStringSet(CONTACT_IDS_KEY, new HashSet<>())));
        contactIds.add(contact.getId());
        editor.putStringSet(CONTACT_IDS_KEY, contactIds);

        editor.apply();
        Log.d(TAG, "Saved contact: " + contact.getCallsign());
    }


    public static List<PluginContact> getAllContacts() {
        SharedPreferences p = prefs();
        Set<String> contactIds = p.getStringSet(CONTACT_IDS_KEY, new HashSet<>());

        List<PluginContact> contacts = new ArrayList<>();
        assert contactIds != null;
        for (String id : contactIds) {
            String json = p.getString(CONTACT_PREFIX + id, null);
            if (json != null) {
                PluginContact contact = PluginContact.fromJson(json);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        }
        return contacts;
    }


    public static PluginContact getContactById(String id) {
        SharedPreferences p = prefs();
        String json = p.getString(CONTACT_PREFIX + id, null);
        return json != null ? PluginContact.fromJson(json) : null;
    }


    public static PluginContact getContactByCallsign(String callsign) {
        for (PluginContact contact : getAllContacts()) {
            if (contact.getCallsign().equals(callsign)) {
                return contact;
            }
        }
        return null;
    }


    public static void deleteContact(String id) {
        SharedPreferences p = prefs();
        SharedPreferences.Editor e = p.edit();
        Set<String> ids = new HashSet<>(Objects.requireNonNull(p.getStringSet(CONTACT_IDS_KEY, new HashSet<>())));
        ids.remove(id);
        e.putStringSet(CONTACT_IDS_KEY, ids);
        e.remove(CONTACT_PREFIX + id);
        boolean ok = e.commit();
        Log.d(TAG, "Deleted " + id + " saved=" + ok);
    }


    public static void clearAllContacts() {
        SharedPreferences p = prefs();
        p.edit().clear().apply();
        Log.d(TAG, "Cleared all contacts");
    }

    public static String generateUniqueId() {
        return "LOCAL_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private static Set<String> getContactIdSet(SharedPreferences prefs) {
        return new HashSet<>(prefs.getStringSet(CONTACT_IDS_KEY, new HashSet<>()));
    }

}