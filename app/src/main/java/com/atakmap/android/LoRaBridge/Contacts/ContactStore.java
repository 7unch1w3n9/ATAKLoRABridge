package com.atakmap.android.LoRaBridge.Contacts;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactStore {
    private static final String TAG = "ContactStore";
    private static final String PREFS_NAME = "plugin_contacts_store";
    private static final String CONTACT_IDS_KEY = "contact_ids";
    private static final String CONTACT_PREFIX = "contact_";


    public static void saveContact(Context context, PluginContact contact) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (contact.getId() == null || contact.getId().isEmpty()) {
            contact.setId(generateUniqueId());
        }

        String contactJson = contact.toJson();
        editor.putString(CONTACT_PREFIX + contact.getId(), contactJson);

        Set<String> contactIds = new HashSet<>(prefs.getStringSet(CONTACT_IDS_KEY, new HashSet<>()));
        contactIds.add(contact.getId());
        editor.putStringSet(CONTACT_IDS_KEY, contactIds);

        editor.apply();
        Log.d(TAG, "Saved contact: " + contact.getCallsign());
    }


    public static List<PluginContact> getAllContacts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> contactIds = prefs.getStringSet(CONTACT_IDS_KEY, new HashSet<>());

        List<PluginContact> contacts = new ArrayList<>();
        for (String id : contactIds) {
            String json = prefs.getString(CONTACT_PREFIX + id, null);
            if (json != null) {
                PluginContact contact = PluginContact.fromJson(json);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        }
        return contacts;
    }


    public static PluginContact getContactById(Context context, String id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(CONTACT_PREFIX + id, null);
        return json != null ? PluginContact.fromJson(json) : null;
    }


    public static PluginContact getContactByCallsign(Context context, String callsign) {
        for (PluginContact contact : getAllContacts(context)) {
            if (contact.getCallsign().equals(callsign)) {
                return contact;
            }
        }
        return null;
    }


    public static void deleteContact(Context context, String id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();


        Set<String> contactIds = getContactIdSet(prefs);
        contactIds.remove(id);
        editor.putStringSet(CONTACT_IDS_KEY, contactIds);


        editor.remove(CONTACT_PREFIX + id);

        editor.apply();
        Log.d(TAG, "Deleted contact: " + id);
    }


    public static void clearAllContacts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d(TAG, "Cleared all contacts");
    }

    public static String generateUniqueId() {
        return "LOCAL_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private static Set<String> getContactIdSet(SharedPreferences prefs) {
        return new HashSet<>(prefs.getStringSet(CONTACT_IDS_KEY, new HashSet<>()));
    }

}