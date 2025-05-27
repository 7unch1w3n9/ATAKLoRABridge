package com.atakmap.android.LoRaBridge.recyclerview;// 1. 自定义联系人列表的 Adapter
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    private final List<Contact> contactList;
    private final OnContactClickListener clickListener;

    public ContactAdapter(OnContactClickListener listener) {
        this.clickListener = listener;
        this.contactList = new ArrayList<>();

        // 提取所有单独联系人
        List<Contact> allContacts = Contacts.getInstance().getAllContacts() ;
        for (Contact c : allContacts) {
//            if (c instanceof IndividualContact
//                && !c.getExtras().getBoolean("fakeGroup", false)
//                && !c.getUID().equals("UserGroups")
//                && !c.getUID().equals("TeamGroups"))
//            {
                contactList.add(c);
//            }
        }
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
        Contact contact = contactList.get(position);
        holder.getTextView().setText(contact.getName());
        holder.itemView.setOnClickListener(v -> clickListener.onContactClick(contact));
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public List<Contact> getContactList() {
        return contactList;
    }
}
