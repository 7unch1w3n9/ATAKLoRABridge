package com.atakmap.android.LoRaBridge.recyclerview;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.LoRaBridge.Contacts.ContactStore;
import com.atakmap.android.LoRaBridge.Contacts.PluginContact;
import com.atakmap.android.contact.Connector;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final Context context;

    public interface OnContactClickListener {
        void onContactClick(PluginContact contact);
    }

    public interface OnContactDeleteListener {
        void onDeleteContact(PluginContact contact);
    }

    private final List<PluginContact> contactList = new ArrayList<>();
    private final OnContactClickListener clickListener;
    private final OnContactDeleteListener deleteListener;
    private final Activity activity;

    public ContactAdapter(Context context, OnContactClickListener listener, OnContactDeleteListener deleteListener, Activity activity) {
        this.context = context;
        this.clickListener = listener;
        this.deleteListener = deleteListener;
        this.activity = activity;
        refreshContacts();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.contact_name);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PluginContact contact = contactList.get(position);
        holder.getTextView().setText(contact.getCallsign());
        holder.itemView.setOnClickListener(v -> clickListener.onContactClick(contact));

        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, v);
            if (contact.isLocal()) {
                menu.getMenu().add("Delete Contact");
            } else {
                menu.getMenu().add("(ATAK contact — cannot delete)");
            }

            menu.setOnMenuItemClickListener(mi -> {
                String title = mi.getTitle().toString();
                if ("Delete Contact".equals(title)) {
                    Log.w("How", context.toString());
                    new AlertDialog.Builder(activity)
                            .setTitle("Confirm Deletion")
                            .setMessage("Delete contact \"" + safe(contact.getCallsign()) + "\"? This only affects local plugin contacts.")
                            .setPositiveButton("Delete", (d, w) -> {
                                int pos  = holder.getAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    contactList.remove(pos);
                                    notifyItemRemoved(pos);
                                } else {
                                    refreshContacts();
                                }
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                                if (deleteListener != null) {
                                    deleteListener.onDeleteContact(contact);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(context, "ATAK contacts cannot be deleted in the plugin.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            menu.show();
            return true;
        });
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public List<PluginContact> getContactList() {
        return contactList;
    }

    public boolean syncContactsFromATAK() {
        List<Contact> atakContacts = Contacts.getInstance().getAllContacts();
        List<PluginContact> storedContacts = ContactStore.getAllContacts();

        Set<String> storedIds = new HashSet<>();
        for (PluginContact contact : storedContacts) {
            storedIds.add(contact.getId());
            Log.d("ContactSync", "saved ATAK contact: " + contact.getId());
        }

        boolean hasChanges = false;

        for (Contact c : atakContacts) {
            if (c instanceof IndividualContact && !c.getExtras().getBoolean("fakeGroup", false)) {
                String uid = c.getUID();
                Log.d("ContactSync", "atakContacts: " + uid);
                Log.d("ContactSync", "have or not:" + storedIds.contains(uid));

                if (!storedIds.contains(uid)) {
                    PluginContact pluginContact = convertToLoRaContact((IndividualContact) c);
                    ContactStore.saveContact(pluginContact);
                    Log.d("ContactSync", "Added ATAK contact: " + pluginContact.getCallsign());
                    hasChanges = true;
                }
            }
        }

        return hasChanges;
    }

    public void addContact(PluginContact contact) {
        ContactStore.saveContact(contact);
        contactList.add(contact);
        notifyItemInserted(contactList.size() - 1);
    }

    public void refreshContacts() {
        try {
            contactList.clear();
            contactList.addAll(ContactStore.getAllContacts());

            Set<String> atakUids = getATAKContactUids();
            for (PluginContact contact : contactList) {
                contact.setLocal(!atakUids.contains(contact.getId()));
            }
        } catch (Exception e) {
            Log.e("ContactAdapter", "刷新失败: " + e.getMessage());
        } finally {
            notifyDataSetChanged();
        }
    }
    private Set<String> getATAKContactUids() {
        Set<String> uids = new HashSet<>();
        List<Contact> atakContacts = Contacts.getInstance().getAllContacts();

        for (Contact c : atakContacts) {
            if (c instanceof IndividualContact && !c.getExtras().getBoolean("fakeGroup", false)) {
                uids.add(c.getUID());
            }
        }
        return uids;
    }

    public void fullSyncAndRefresh() {
        boolean changesMade = syncContactsFromATAK();
        refreshContacts();

        if (changesMade) {
            Log.d("ContactSync", "Completed full sync with changes");
        } else {
            Log.d("ContactSync", "Completed full sync (no changes)");
        }
    }

    private PluginContact convertToLoRaContact(IndividualContact contact) {
        PluginContact loraContact = new PluginContact(
                contact.getUID(),
                contact.getName(),
                contact.getName()
        );

        Connector ipConnector = contact.getConnector("connector.ip");
        if (ipConnector != null) {
            String[] parts = ipConnector.getConnectionString().split(":");
            if (parts.length >= 2) {
                loraContact.setIpAddress(parts[0]);
                try {
                    loraContact.setPort(Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    android.util.Log.e("ContactConvert", "Invalid port: " + parts[1]);
                }
            }
        }
        return loraContact;
    }

}
