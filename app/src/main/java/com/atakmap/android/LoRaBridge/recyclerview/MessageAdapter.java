package com.atakmap.android.LoRaBridge.recyclerview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.plugin.R;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<ChatMessageEntity> messages;

    public MessageAdapter() {
        this.messages = new ArrayList<>();
    }

    public MessageAdapter(List<ChatMessageEntity> initialMessages) {
        this.messages = new ArrayList<>(initialMessages);
    }

    public void addMessage(ChatMessageEntity msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.message_text);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageEntity msg = messages.get(position);
        holder.textView.setText(msg.message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<ChatMessageEntity> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }
}