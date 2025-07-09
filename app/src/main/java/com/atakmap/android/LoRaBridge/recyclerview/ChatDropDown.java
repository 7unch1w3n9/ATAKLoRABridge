package com.atakmap.android.LoRaBridge.recyclerview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageFactory;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.Database.ChatRepository;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.chat.ChatDatabase;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.EditText;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotService;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;


import com.atakmap.android.LoRaBridge.Database.ChatViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatDropDown extends DropDownReceiver {

        private final View rootView;
        private final RecyclerView messageList;
        private final EditText messageInput;
        private final Button sendButton;
        private final MessageAdapter messageAdapter;
        private Observer<List<ChatMessageEntity>> chatObserver;
        private ChatViewModel viewModel;
        private final Contact contact;

        public ChatDropDown(MapView mapView, Context context, Contact contact, Activity activity) {
            super(mapView);
            rootView = LayoutInflater.from(context).inflate(R.layout.pane_chat, null);

            TextView title = rootView.findViewById(R.id.chat_title);
            title.setText("Chat with " + contact.getName() );
            messageList = rootView.findViewById(R.id.chat_message_list);


            messageAdapter = new MessageAdapter(new ArrayList<>());
            messageList.setAdapter(messageAdapter);
            messageList.setLayoutManager(new LinearLayoutManager(context));

            this.contact = contact;
//            Activity activity1 = (Activity)(mapView.getContext());
            viewModel = new ViewModelProvider((ViewModelStoreOwner)activity).get(ChatViewModel.class);
            viewModel.deleteAllMessages();

            chatObserver = messageAdapter::setMessages;
            LiveData<List<ChatMessageEntity>> liveData = viewModel.getMessagesForContact(contact.getUID());
            liveData.observeForever(chatObserver);
            List<ChatMessageEntity> initialMessages = liveData.getValue();
            if (initialMessages != null) {
                messageAdapter.setMessages(initialMessages);
            }


            messageInput = rootView.findViewById(R.id.chat_input);
            sendButton = rootView.findViewById(R.id.chat_send_button);
            sendButton.setOnClickListener(v -> {
                String text = messageInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    ChatMessageEntity message = ChatMessageFactory.fromUserInput(
                            MapView.getDeviceUid(),
                            MapView._mapView.getDeviceCallsign(),
                            contact.getUID(),
                            contact.getName(),
                            text,
                            "text"
                    );
                    viewModel.insertIfNotExists(message);
                    messageInput.setText("");
                } else {
                    Log.e("Chat", "Invalid message: empty content");
                }
            });
        }

        public void show() {
            showDropDown(rootView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT);
        }

        @Override
        protected void disposeImpl() {
            if (chatObserver != null) {
                viewModel.getMessagesForContact(contact.getUID()).removeObserver(chatObserver);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }

}



