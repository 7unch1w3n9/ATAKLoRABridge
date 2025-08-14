package com.atakmap.android.LoRaBridge.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.atakmap.android.LoRaBridge.ChatMessage.ChatMessageFactory;
import com.atakmap.android.LoRaBridge.Contacts.PluginContact;
import com.atakmap.android.LoRaBridge.Database.ChatMessageEntity;
import com.atakmap.android.LoRaBridge.plugin.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.EditText;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;


import com.atakmap.android.LoRaBridge.Database.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class ChatDropDown extends DropDownReceiver {

        private final View rootView;
        private final RecyclerView messageList;
        private final EditText messageInput;
        private final Button sendButton;
        private final MessageAdapter messageAdapter;
        private Observer<List<ChatMessageEntity>> chatObserver;
        private ChatViewModel viewModel;
        private final PluginContact contact;

        public ChatDropDown(MapView mapView, Context context, PluginContact contact, Activity activity) {
            super(mapView);
            rootView = LayoutInflater.from(context).inflate(R.layout.pane_chat, null);

            TextView title = rootView.findViewById(R.id.chat_title);
            title.setText("Chat with " + contact.getCallsign() );
            messageList = rootView.findViewById(R.id.chat_message_list);


            messageAdapter = new MessageAdapter(new ArrayList<>());
            messageList.setAdapter(messageAdapter);
            messageList.setLayoutManager(new LinearLayoutManager(context));

            this.contact = contact;
//            Activity activity1 = (Activity)(mapView.getContext());
            viewModel = new ViewModelProvider((ViewModelStoreOwner)activity).get(ChatViewModel.class);
            viewModel.deleteAllMessages();

            chatObserver = messageAdapter::setMessages;
            LiveData<List<ChatMessageEntity>> liveData = viewModel.getMessagesForContact(contact.getId());
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
                            contact.getId(),
                            contact.getCallsign(),
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
                viewModel.getMessagesForContact(contact.getId()).removeObserver(chatObserver);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }

}



