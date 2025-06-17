package com.atakmap.android.LoRaBridge.recyclerview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.maps.MapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        public final TextView meta;
        public final TextView message;
        public final TextView date;
        public final View dateRow;
        public final ViewGroup bubble;
        public final ViewGroup container;

        public ViewHolder(View view) {
            super(view);
            container = view.findViewById(R.id.message_container);
            bubble = view.findViewById(R.id.message_bubble);
            meta = view.findViewById(R.id.message_meta);
            message = view.findViewById(R.id.message_text);
            date = view.findViewById(R.id.message_date);
            dateRow = view.findViewById(R.id.message_date);
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

        // 设置内容
        holder.message.setText(msg.getMessage());

        // 时间戳格式：[HH:mm:ss]
        String timeStr = "[--:--:--]";
        if (msg.getSentTime() > 0) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            timeStr = "[" + timeFormat.format(new Date(msg.getSentTime())) + "]";
        }

        // 是否自己发的
        boolean isSelf = msg.getSenderUid().equals(MapView.getDeviceUid());
        String callsign = isSelf ? "Me" : msg.getSenderCallsign();

        // 设置 meta 行（蓝色、粗体）
        String metaLine = timeStr + " " + callsign + ":";
        holder.meta.setText(metaLine);

        // 设置对齐（靠左或靠右）
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        params.gravity = isSelf ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(params);

        // 设置背景样式（可换成你自己的 drawable）
        holder.bubble.setBackgroundResource(R.drawable.chat_bubble_background);

        // 日期分隔逻辑
        boolean showDate = false;
        if (position == 0) {
            showDate = true;
        } else {
            ChatMessageEntity prev = messages.get(position - 1);
            if (prev != null) {
                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();
                c1.setTimeInMillis(prev.getSentTime());
                c2.setTimeInMillis(msg.getSentTime());
                showDate = !(c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                        c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                        c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH));
            }
        }

        if (showDate) {
            holder.dateRow.setVisibility(View.VISIBLE);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            holder.date.setText(dateFormat.format(new Date(msg.getSentTime())));
        } else {
            holder.dateRow.setVisibility(View.GONE);
        }
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