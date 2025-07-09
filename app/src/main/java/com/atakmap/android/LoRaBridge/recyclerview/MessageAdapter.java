package com.atakmap.android.LoRaBridge.recyclerview;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.maps.MapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;

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

    private long parseToMillis(String timeField) {
        try {
            if (timeField.matches("\\d+")) {
                return Long.parseLong(timeField);  // 时间戳毫秒
            }

            // 处理 "Z" 和 "+00:00" 形式的 ISO8601
            if (timeField.endsWith("Z")) {
                timeField = timeField.replace("Z", "+0000");
            } else {
                timeField = timeField.replaceAll(":(?=[0-9]{2}$)", ""); // +01:00 -> +0100
            }

            // 使用兼容模式解析（不含 X）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            Date date = sdf.parse(timeField);
            return date != null ? date.getTime() : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageEntity msg = messages.get(position);
        holder.message.setText(msg.getMessage());

        long sentMillis = parseToMillis(msg.getSentTime());
        String timeStr = "[--:--:--]";
        if (sentMillis > 0) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            timeStr = "[" + timeFormat.format(new Date(sentMillis)) + "]";
        }

        boolean isSelf = msg.getSenderUid().equals(MapView.getDeviceUid());
        String callsign = isSelf ? "Me" : msg.getSenderCallsign();
        String metaLine = timeStr + " " + callsign + ":";
        holder.meta.setText(metaLine);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        params.gravity = isSelf ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(params);
        holder.bubble.setBackgroundResource(R.drawable.chat_bubble_background);

        boolean showDate = false;
        long prevMillis = -1;
        if (position == 0) {
            showDate = true;
        } else {
            ChatMessageEntity prev = messages.get(position - 1);
            if (prev != null) {
                prevMillis = parseToMillis(prev.getSentTime());
                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();
                c1.setTimeInMillis(prevMillis);
                c2.setTimeInMillis(sentMillis);
                showDate = !(c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                        c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                        c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH));
            }
        }

        if (showDate) {
            holder.dateRow.setVisibility(View.VISIBLE);
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                holder.date.setText(dateFormat.format(new Date(sentMillis)));
            } catch (Exception e) {
                holder.date.setText("--/--/----");
            }
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
