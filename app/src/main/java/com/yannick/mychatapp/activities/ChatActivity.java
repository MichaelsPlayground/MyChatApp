package com.yannick.mychatapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.signature.ObjectKey;
import com.chrisrenke.giv.GravityImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yannick.mychatapp.data.Background;
import com.yannick.mychatapp.BuildConfig;
import com.yannick.mychatapp.CatchViewPager;
import com.yannick.mychatapp.FileOperations;
import com.yannick.mychatapp.adapters.FullScreenImageAdapter;
import com.yannick.mychatapp.GlideApp;
import com.yannick.mychatapp.adapters.ImageAdapter;
import com.yannick.mychatapp.ImageOperations;
import com.yannick.mychatapp.adapters.MemberListAdapter;
import com.yannick.mychatapp.data.Message;
import com.yannick.mychatapp.adapters.MessageAdapter;
import com.yannick.mychatapp.MyCallback;
import com.yannick.mychatapp.adapters.PinboardAdapter;
import com.yannick.mychatapp.R;
import com.yannick.mychatapp.data.Room;
import com.yannick.mychatapp.data.Theme;
import com.yannick.mychatapp.data.User;
import com.yannick.mychatapp.ZoomOutPageTransformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

public class ChatActivity extends AppCompatActivity {

    private EditText input_msg;

    private Theme theme;

    private Room room;

    private String userID;
    private String roomKey;
    private String imgurl;
    private final String roomDataKey = "-0roomdata";
    private String app_name;
    private String roomName;
    private String lastReadMessage;
    private String key_last;
    private String lastSearch = "";

    private DatabaseReference root;
    private final DatabaseReference userRoot = FirebaseDatabase.getInstance().getReference().getRoot().child("users");
    private final DatabaseReference roomRoot = FirebaseDatabase.getInstance().getReference().getRoot().child("rooms");
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private StorageReference storageReferenceRoomImages;
    private Uri photoURI;

    private RecyclerView recyclerView;
    private MessageAdapter mAdapter;
    private CoordinatorLayout layout;
    private GestureDetector gestureDetector;
    private String quoteStatus = "";
    private int messageCount = 0;
    private User user = new User();

    private final ArrayList<Message> messageList = new ArrayList<>();
    private final ArrayList<User> userList = new ArrayList<>();
    private ArrayList<Message> searchResultList = new ArrayList<>();
    private final ArrayList<String> roomList = new ArrayList<>();
    private final ArrayList<String> roomKeysList = new ArrayList<>();
    private final ArrayList<String> imageList = new ArrayList<>();
    private final ArrayList<Message> pinnedList = new ArrayList<>();
    private final ArrayList<User> memberList = new ArrayList<>();

    private AlertDialog imageListAlert;
    private AlertDialog pinboardAlert;

    private boolean firstMessage = true;
    private boolean userListCreated = false;
    private boolean cancelFullscreenImage = false;
    private boolean imageListOpened = false;
    private boolean lastReadMessageReached = false;
    private FloatingActionButton btn_scrolldown;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
    private SimpleDateFormat sdf_local = new SimpleDateFormat("yyyyMMdd_HHmmss_z");

    private int katindex = 0;

    private GravityImageView backgroundview;
    private ImageButton roomImageButton;

    private TextView quote_text;
    private LinearLayout quote_layout;
    private ImageView quote_image;

    private SearchView searchView;

    private Dialog fullscreendialog;

    private FirebaseAuth mAuth;

    private final FileOperations fileOperations = new FileOperations(this);

    private SharedPreferences settings;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeTheme(Theme.getCurrentTheme(this));
        setContentView(R.layout.chat_room);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        recyclerView = findViewById(R.id.recycler_view);
        layout = findViewById(R.id.coordinatorlayout);
        backgroundview = findViewById(R.id.backgroundview);
        quote_text = findViewById(R.id.quote_text);
        ImageButton quote_remove = findViewById(R.id.quote_remove);
        quote_layout = findViewById(R.id.quote_layout);
        quote_image = findViewById(R.id.quote_image);

        mAdapter = new MessageAdapter(messageList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        setBackgroundImage();

        mAuth = FirebaseAuth.getInstance();

        btn_scrolldown = findViewById(R.id.scrolldown);
        btn_scrolldown.setOnClickListener(view -> recyclerView.scrollToPosition(messageList.size() - 1));

        app_name = getResources().getString(R.string.app_name);

        btn_scrolldown.hide();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx,int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!recyclerView.canScrollVertically(1)) {
                    btn_scrolldown.hide();
                } else if (dy <0 && !btn_scrolldown.isShown()) {
                    btn_scrolldown.show();
                }
            }
        });

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        LocalBroadcastManager.getInstance(this).registerReceiver(quoteReceiver, new IntentFilter("quote"));
        LocalBroadcastManager.getInstance(this).registerReceiver(quotedReceiver, new IntentFilter("quotedMessage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(permissionReceiver, new IntentFilter("permission"));
        LocalBroadcastManager.getInstance(this).registerReceiver(userReceiver, new IntentFilter("userprofile"));
        LocalBroadcastManager.getInstance(this).registerReceiver(forwardReceiver, new IntentFilter("forward"));
        LocalBroadcastManager.getInstance(this).registerReceiver(fullscreenReceiver, new IntentFilter("fullscreenimage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pinReceiver, new IntentFilter("pinMessage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(jumppinnedReceiver, new IntentFilter("jumppinned"));
        LocalBroadcastManager.getInstance(this).registerReceiver(closeFullscreenReceiver, new IntentFilter("closefullscreen"));

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        Button sendMessageButton = findViewById(R.id.btn_send);
        input_msg = findViewById(R.id.msg_input);
        ImageButton btn_camera = findViewById(R.id.btn_camera);
        ImageButton btn_image = findViewById(R.id.btn_image);

        userID = mAuth.getCurrentUser().getUid();
        roomName = getIntent().getExtras().get("room_name").toString();
        roomKey = getIntent().getExtras().get("room_key").toString();
        String nmid = getIntent().getExtras().get("nmid").toString();
        lastReadMessage = getIntent().getExtras().get("last_read_message").toString();
        lastReadMessageReached = (nmid.equals(lastReadMessage));
        setTitle(roomName);
        fileOperations.writeToFile(roomKey, FileOperations.currentInputFilePattern);

        int pushID = 0;
        for (int i = 0; i < roomKey.length(); ++i) {
            pushID += (int) roomKey.charAt(i);
        }
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(pushID);

        if (settings.getBoolean(MainActivity.settingsSaveEnteredTextKey, true)) {
            input_msg.setText(fileOperations.readFromFile(String.format(FileOperations.currentInputFilePattern, roomKey)).replaceAll("<br />", "\n"));
        }

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        input_msg.setOnClickListener(view -> recyclerView.scrollToPosition(messageList.size() - 1));

        recyclerView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> {
            if (i3 < i7 && !cancelFullscreenImage) {
                recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(
                        recyclerView.getAdapter().getItemCount() - 1), 0);
            } else {
                cancelFullscreenImage = false;
            }
        });

        root = FirebaseDatabase.getInstance().getReference().child("rooms").child(roomKey);

        sendMessageButton.setOnClickListener(view -> {
            if (!input_msg.getText().toString().trim().isEmpty()) {
                if (!searchView.isIconified()) {
                    searchView.setIconified(true);
                    searchView.setIconified(true);
                }

                String newMessageKey = root.push().getKey();

                String currentDateAndTime = sdf.format(new Date());

                DatabaseReference message_root = root.child(newMessageKey);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", userID);
                map.put("msg", input_msg.getText().toString().trim());
                map.put("img", "");
                map.put("pinned", false);
                map.put("quote", quoteStatus);
                map.put("time", currentDateAndTime);

                message_root.updateChildren(map);
                input_msg.getText().clear();
                quoteStatus = "";
                quote_text.setText("");
                quote_image.setImageDrawable(null);
                quote_image.setVisibility(View.GONE);
                quote_layout.setVisibility(View.GONE);
                fileOperations.writeToFile("", String.format(FileOperations.currentInputFilePattern, roomKey));
                fileOperations.writeToFile(newMessageKey, String.format(FileOperations.newestMessageFilePattern, roomKey));
            }
        });

        btn_image.setOnTouchListener((view, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                chooseImage();
                btn_image.setBackgroundResource(R.drawable.ic_image);
                return true;
            } else {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_image.setBackgroundResource(R.drawable.ic_image_light);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    btn_image.setBackgroundResource(R.drawable.ic_image);
                    return true;
                }
            }
            return false;
        });

        btn_camera.setOnTouchListener((view, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                takePicture();
                btn_camera.setBackgroundResource(R.drawable.ic_camera);
                return true;
            } else {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_camera.setBackgroundResource(R.drawable.ic_camera_light);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    btn_camera.setBackgroundResource(R.drawable.ic_camera);
                    return true;
                }
            }
            return false;
        });

        quote_remove.setOnTouchListener((view, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                quoteStatus = "";
                quote_text.setText("");
                quote_image.setImageDrawable(null);
                quote_image.setVisibility(View.GONE);
                quote_layout.setVisibility(View.GONE);
                quote_remove.setBackgroundResource(R.drawable.ic_clear);
                return true;
            } else {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    quote_remove.setBackgroundResource(R.drawable.ic_clear_light);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    quote_remove.setBackgroundResource(R.drawable.ic_clear);
                    return true;
                }
            }
            return false;
        });

        userRoot.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                addUser(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, R.string.nodatabaseconnection, Toast.LENGTH_SHORT).show();
            }
        });
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!userListCreated) {
                    handler.postDelayed(this, 1000);
                } else {
                    root.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            addMessage(dataSnapshot, -1);
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            changeMessage(dataSnapshot);
                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {
                            removeMessage(dataSnapshot);
                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(ChatActivity.this, R.string.nodatabaseconnection, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }, 10);

        roomRoot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
                    String roomKey = uniqueKeySnapshot.getKey();
                    if (roomKey.equals(ChatActivity.this.roomKey)) {
                        messageCount = (int)uniqueKeySnapshot.getChildrenCount() - 1;
                    }
                    for (DataSnapshot roomSnapshot : uniqueKeySnapshot.getChildren()) {
                        Room room = roomSnapshot.getValue(Room.class);
                        room.setKey(roomKey);
                        if (room.getPasswd().equals(fileOperations.readFromFile(String.format(FileOperations.passwordFilePattern, roomKey))) && !roomKey.equals(ChatActivity.this.roomKey)) {
                            roomList.add(room.getName());
                            roomKeysList.add(room.getKey());
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, R.string.nodatabaseconnection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMessage(DataSnapshot dataSnapshot, int index) {
        if (firstMessage) {
            firstMessage = false;

            room = dataSnapshot.getValue(Room.class);
            room.setKey(dataSnapshot.getRef().getParent().getKey());
            user = getUser(room.getAdmin());

            String creationTime = room.getTime();

            try {
                creationTime = sdf_local.format(sdf_local.parse(creationTime));
            } catch (ParseException e) {
                Log.e("ParseException", e.toString());
            }
            String creationTimeCon = creationTime.substring(6, 8) + "." + creationTime.substring(4,6) + "." + creationTime.substring(0, 4);
            String text = getResources().getString(R.string.roomintro, creationTimeCon, user.getName());
            Message m = new Message(user, text, creationTime, false, room.getKey(), Message.Type.HEADER, "", "", "", false);

            messageList.add(m);
            if (!memberList.contains(m.getUser())) {
                memberList.add(m.getUser());
            }
        } else {
            String key = dataSnapshot.getKey();
            String chat_msg = dataSnapshot.child("msg").getValue().toString();
            String img = dataSnapshot.child("img").getValue().toString();
            String chat_user_id = dataSnapshot.child("name").getValue().toString();
            boolean pinned = (boolean) dataSnapshot.child("pinned").getValue();
            String quote = dataSnapshot.child("quote").getValue().toString();
            String time = dataSnapshot.child("time").getValue().toString();

            user = getUser(chat_user_id);

            try {
                time = sdf_local.format(sdf_local.parse(time));
            } catch (ParseException e) {
                Log.e("ParseException", e.toString());
            }
            if (lastReadMessage.equals(key_last) && !lastReadMessageReached) {
                Message m = new Message(user, getResources().getString(R.string.unreadmessages), time, false, "-", Message.Type.HEADER, "", "", "", false);
                messageList.add(m);
            }
            key_last = key;

            if (index == -1 && !messageList.get(messageList.size() - 1).getTime().substring(0, 8).equals(time.substring(0, 8))) {
                String text = time.substring(6, 8) + "." + time.substring(4, 6) + "." + time.substring(0, 4);
                Message m = new Message(user, text, time, false, "-", Message.Type.HEADER, "", "", "", false);
                messageList.add(m);
            }

            boolean sender = userID.equals(user.getUserID());
            boolean con = false;
            int ind = (index == -1) ? messageList.size() : index;
            if (messageList.size() - 1 > 0 && messageList.get(ind - 1).getType() != Message.Type.HEADER && messageList.get(ind - 1).getUser().getUserID().equals(chat_user_id) && messageList.get(ind - 1).getTime().substring(0, 13).equals(time.substring(0, 13))) {
                con = true;
                messageList.get(ind - 1).setTime("");
                mAdapter.notifyDataSetChanged();
            }

            Message m;
            if (quote.equals("")) {
                if (!chat_msg.equals("")) {
                    if (chat_msg.length() > 11 && chat_msg.substring(0, 12).equals("(Forwarded) ")) {
                        if (chat_msg.length() > 2000 + 12) {
                            m = new Message(user, chat_msg.substring(12), time, sender, key, Message.getFittingForwardedExpandableMessageType(sender, con), "", "", "", pinned);
                        } else {
                            m = new Message(user, chat_msg.substring(12), time, sender, key, Message.getFittingForwardedMessageType(sender, con), "", "", "", pinned);
                        }
                    } else {
                        if (chat_msg.length() > 2000) {
                            m = new Message(user, chat_msg, time, sender, key, Message.getFittingExpandableMessageType(sender, con), "", "", "", pinned);
                        } else {
                            m = new Message(user, chat_msg, time, sender, key, Message.getFittingBasicMessageType(sender, con), "", "", "", pinned);
                        }
                    }
                } else {
                    if (!imageList.contains(img)) {
                        imageList.add(img);
                    }
                    m = new Message(user, img, time, sender, key, Message.getFittingImageMessageType(sender, con), "", "", "", pinned);
                }
            } else {
                Message.Type quoteType = Message.Type.HEADER;
                String quoteMessage = getResources().getString(R.string.quotedmessagenolongeravailable);
                String quoteName = "";
                String quoteKey = "";
                for (Message quoteMsg : messageList) {
                    if (quoteMsg.getKey().equals(quote)) {
                        quoteMessage = quoteMsg.getMsg();
                        quoteName = quoteMsg.getUser().getName();
                        quoteKey = quoteMsg.getKey();
                        quoteType = quoteMsg.getType();
                        break;
                    }
                }
                if (!Message.isImage(quoteType)) {
                    if (!quoteMessage.equals(getResources().getString(R.string.quotedmessagenolongeravailable))) {
                        m = new Message(user, chat_msg, time, sender, key, Message.getFittingQuoteMessageType(sender, con), quoteName, quoteMessage, quoteKey, pinned);
                    } else {
                        m = new Message(user, chat_msg, time, sender, key, Message.getFittingQuoteDeletedMessageType(sender, con), quoteName, quoteMessage, quoteKey, pinned);
                    }
                } else {
                    m = new Message(user, chat_msg, time, sender, key, Message.getFittingQuoteImageMessageType(sender, con), quoteName, quoteMessage, quoteKey, pinned);
                }
            }
            if (index != -1 && messageList.get(ind).getTime().equals("")) {
                m.setTime("");
            }
            if (index == -1) {
                messageList.add(m);
                if (!memberList.contains(m.getUser())) {
                    memberList.add(m.getUser());
                }
                if (pinned) {
                    pinnedList.add(m);
                }
            } else {
                ArrayList<Message> templist = new ArrayList<>(messageList);
                messageList.clear();
                templist.add(index+1, m);
                messageList.addAll(templist);
                mAdapter.notifyDataSetChanged();
            }
        }

        recyclerView.scrollToPosition(messageList.size() - 1);
        btn_scrolldown.hide();
    }

    private void removeMessage(DataSnapshot dataSnapshot) {
        String key = dataSnapshot.getKey();
        String img = dataSnapshot.child("img").getValue().toString();

        if (!img.equals("")) {
            StorageReference storageRef = storage.getReferenceFromUrl(storageReference.toString());
            StorageReference pathReference = storageRef.child("images/" + img);
            imageList.remove(img);
            pathReference.delete();
        }

        List<Message> tempMessageList = new ArrayList<>();
        for (Message m : messageList) {
            if (!(m.getKey().equals(key))) {
                tempMessageList.add(m);
            } else if (m.getType() != Message.Type.HEADER && Message.isConMessage(m.getType()) && !m.getTime().equals("")) {
                tempMessageList.get(tempMessageList.size()-1).setTime(m.getTime());
            }
        }

        if (tempMessageList.get(tempMessageList.size()-1).getType() == Message.Type.HEADER && tempMessageList.size()!=1) {
            tempMessageList.remove(tempMessageList.size()-1);
        }
        for (int j = 1; j < tempMessageList.size()-1; j++) {
            if (tempMessageList.get(j).getType() == Message.Type.HEADER && tempMessageList.get(j+1).getType() == Message.Type.HEADER) {
                tempMessageList.remove(j);
            }
        }

        messageList.clear();
        for (Message m : tempMessageList) {
            if (m.getQuote_key().equals(key)) {
                if (img.equals("")) {
                    m.setQuote_message(getResources().getString(R.string.quotedmessagenolongeravailable));
                } else {
                    m.setQuote_message(img);
                }
                m.setQuote_name("");
                m.setType(Message.getQuoteDeletedTypeForQuoteType(m.getType()));
            }
            messageList.add(m);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void changeMessage(DataSnapshot dataSnapshot) {
        String key = dataSnapshot.getKey();
        String img = dataSnapshot.child("img").getValue().toString();
        String chatMsg = dataSnapshot.child("msg").getValue().toString();
        String chatUserId = dataSnapshot.child("name").getValue().toString();

        for (Message m : messageList) {
            if (m.getType() != Message.Type.HEADER) {
                if (m.getKey().equals(key)) {
                    int index = messageList.indexOf(m);
                    addMessage(dataSnapshot, index);
                    ArrayList<Message> templist = new ArrayList<>(messageList);
                    messageList.clear();
                    templist.remove(index);
                    messageList.addAll(templist);
                    break;
                }
            }
        }
        for (Message m : messageList) {
            if (m.getType() != Message.Type.HEADER) {
                if (m.getQuote_key().equals(key)) {
                    if (img.equals("")) {
                        m.setQuote_message(chatMsg);
                    } else {
                        m.setQuote_message(img);
                    }
                    m.setQuote_name(getUser(chatUserId).getName());
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void setBackgroundImage() {
        Background background = Background.getCurrentBackground(getApplicationContext());
        if (getResources().getConfiguration().orientation != 2) {
            switch (background) {
                case BREATH_OF_THE_WILD:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_botw, null));
                    break;
                case SPLATOON_2:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.CENTER);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_splatoon2, null));
                    break;
                case PERSONA_5:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.TOP);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_persona, null));
                    break;
                case KIMI_NO_NA_WA:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_kiminonawa, null));
                    break;
                case SUPER_MARIO_BROS:
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_smb, null));
                    break;
                case SUPER_MARIO_MAKER:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_smm, null));
                    break;
                case XENOBLADE_CHRONICLES_2:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_xc2, null));
                    break;
                case FIRE_EMBLEM_FATES:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_fef, null));
                    break;
                case SUPER_SMASH_BROS_ULTIMATE:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.CENTER);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_ssbu, null));
                    break;
                case DETECTIVE_PIKACHU:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.TOP);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_dp, null));
                    break;
                default:
                    break;
            }
        } else {
            switch (background) {
                case BREATH_OF_THE_WILD:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.TOP);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_botw_horizontal, null));
                    break;
                case SPLATOON_2:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.CENTER);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_splatoon2_horizontal, null));
                    break;
                case PERSONA_5:
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_persona_horizontal, null));
                    break;
                case KIMI_NO_NA_WA:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.TOP);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_kiminonawa_horizontal, null));
                    break;
                case SUPER_MARIO_BROS:
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_smb_horizontal, null));
                    break;
                case SUPER_MARIO_MAKER:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_smm_horizontal, null));
                    break;
                case XENOBLADE_CHRONICLES_2:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.BOTTOM);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_xc2_horizontal, null));
                    break;
                case FIRE_EMBLEM_FATES:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.CENTER);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_fef_horizontal, null));
                    break;
                case SUPER_SMASH_BROS_ULTIMATE:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.CENTER);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_ssbu_horizontal, null));
                    break;
                case DETECTIVE_PIKACHU:
                    backgroundview.setImageGravity(GravityImageView.CENTER_HORIZONTAL|GravityImageView.TOP);
                    backgroundview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.background_dp_horizontal, null));
                    break;
                default:
                    break;
            }
        }
    }

    private void changeTheme(Theme theme) {
        this.theme = theme;
        if (theme == Theme.DARK) {
            setTheme(R.style.DarkChat);
        } else {
            setTheme(R.style.AppThemeChat);
        }
    }

    private void uploadImage(Uri filePath, final int type) {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.setIconified(true);
        }
        final ProgressDialog progressDialog;
        if (theme == Theme.DARK) {
            progressDialog = new ProgressDialog(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle(R.string.upload);
        progressDialog.show();

        String imgName = UUID.randomUUID().toString();
        StorageReference ref;
        if (type == ImageOperations.PICK_ROOM_IMAGE_REQUEST) {
            ref = storageReference.child("room_images/" + imgName);
        } else {
            ref = storageReference.child("images/" + imgName);
        }

        ImageOperations imageOperations = new ImageOperations(getContentResolver());
        byte[] byteArray = imageOperations.getImageAsBytes(this, filePath, type);

        UploadTask uploadTask = ref.putBytes(byteArray);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            progressDialog.dismiss();
            Toast.makeText(ChatActivity.this, R.string.imageuploaded, Toast.LENGTH_SHORT).show();

            if (type != ImageOperations.PICK_ROOM_IMAGE_REQUEST) {
                String newMessageKey = root.push().getKey();

                String currentDateAndTime = sdf.format(new Date());

                DatabaseReference message_root = root.child(newMessageKey);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", userID);
                map.put("msg", "");
                map.put("img", imgName);
                map.put("pinned", false);
                map.put("quote", "");
                map.put("time", currentDateAndTime);

                message_root.updateChildren(map);

                quoteStatus = "";

                if (type == ImageOperations.CAPTURE_IMAGE_REQUEST && settings.getBoolean(MainActivity.settingsStoreCameraPicturesKey, true)) {
                    downloadImage(imgName, type);
                }
            } else {
                DatabaseReference message_root = FirebaseDatabase.getInstance().getReference().child("rooms").child(roomKey).child(roomDataKey);
                Map<String, Object> map = new HashMap<>();
                map.put("img", imgName);
                message_root.updateChildren(map);

                room.setImg(imgName);

                storageReferenceRoomImages = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
                final StorageReference pathReference = storageReferenceRoomImages.child("room_images/" + imgName);
                GlideApp.with(getApplicationContext())
                        .load(pathReference)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(roomImageButton);
            }
        }).addOnFailureListener(e -> {
            Log.e("Upload failed", e.toString());
            progressDialog.dismiss();
            Toast.makeText(ChatActivity.this, R.string.imagetoolarge, Toast.LENGTH_SHORT).show();
        }).addOnProgressListener(taskSnapshot -> {
            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                    .getTotalByteCount());
            progressDialog.setMessage((int)progress+"% " + getResources().getString(R.string.uploaded));
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), ImageOperations.PICK_IMAGE_REQUEST);
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e("error creating file", e.toString());
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID+".fileprovider", photoFile);
                if (isStoragePermissionGranted(1)) {
                    takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, ImageOperations.CAPTURE_IMAGE_REQUEST);
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = sdf.format(new Date()).substring(0, 15);
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpeg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImageOperations.CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (photoURI != null) {
                uploadImage(photoURI, requestCode);
            }
        } else if (resultCode == RESULT_OK && data != null) {
            Uri filePath = data.getData();
            if (filePath != null) {
                uploadImage(filePath, requestCode);
            }
        }
    }

    public BroadcastReceiver quoteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            quoteStatus = intent.getStringExtra("quoteID");
            for (Message m : messageList) {
                if (m.getKey().equals(quoteStatus)) {
                    String user;
                    if (m.getUser().getUserID().equals(userID)) {
                        user = getResources().getString(R.string.you);
                    } else {
                        user = m.getUser().getName();
                    }
                    if (!Message.isImage(m.getType())) {
                        String text = user + " " + m.getMsg();
                        SpannableStringBuilder str = new SpannableStringBuilder(text);
                        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), user.length()+1, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        quote_image.setVisibility(View.GONE);
                        quote_text.setText(str);
                    } else {
                        SpannableStringBuilder str = new SpannableStringBuilder(user);
                        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, user.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        String imgurl = m.getMsg();
                        StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
                        StorageReference pathReference = storageRef.child("images/" + imgurl);
                        GlideApp.with(context)
                                .load(pathReference)
                                .placeholder(R.color.grey)
                                .centerCrop()
                                .thumbnail(0.05f)
                                .into(quote_image);
                        quote_image.setVisibility(View.VISIBLE);
                        quote_text.setText(str);
                    }
                    quote_layout.setVisibility(View.VISIBLE);
                    break;
                }
            }
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    };

    public BroadcastReceiver quotedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
                searchView.setIconified(true);
            }
            
            String quoted = intent.getStringExtra("quoteID");
            int pos = 0;
            for (Message m : messageList) {
                if (m.getKey().equals(quoted)) {
                    break;
                } else {
                    pos++;
                }
            }
            recyclerView.scrollToPosition(pos);
        }
    };

    public BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            imgurl = intent.getStringExtra("imgurl");
            if (isStoragePermissionGranted(0)) {
                downloadImage(imgurl, ImageOperations.PICK_IMAGE_REQUEST);
            }
        }
    };

    public BroadcastReceiver userReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_id = intent.getStringExtra("userid");
            showProfile(user_id);
        }
    };

    public BroadcastReceiver fullscreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String image = intent.getStringExtra("image");
            showFullscreenImage(image, 2);
        }
    };

    public BroadcastReceiver forwardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String message_id = intent.getStringExtra("forwardID");
            if (!roomList.isEmpty()) {
                forwardMessage(message_id);
            } else {
                Toast.makeText(getApplicationContext(), R.string.noroomfound, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public BroadcastReceiver pinReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pin = intent.getStringExtra("pinID");
            pinMessage(pin);
        }
    };

    public BroadcastReceiver jumppinnedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pinnedKey = intent.getStringExtra("pinnedKey");
            int pos = 0;
            for (Message m : messageList) {
                if (m.getKey().equals(pinnedKey)) {
                    break;
                } else {
                    pos++;
                }
            }
            pinboardAlert.dismiss();
            recyclerView.scrollToPosition(pos);
        }
    };

    public BroadcastReceiver closeFullscreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cancelFullscreenImage = true;
            View decorView = fullscreendialog.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(uiOptions);
            fullscreendialog.dismiss();
        }
    };

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    public boolean isStoragePermissionGranted(int requestCode) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("StoragePermission", "Permission is granted");
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("StoragePermission", "Permission is granted");
            if (requestCode == 0) {
                downloadImage(imgurl, ImageOperations.PICK_IMAGE_REQUEST);
            } else if (requestCode == 1) {
                takePicture();
            } else if (requestCode == 2) {
                writeBackup(createBackup());
            }
        } else {
            Log.v("StoragePermission", "Permission is rejected");
        }
    }

    private void downloadImage(String imgurl, final int type) {
        StorageReference storageRef = storage.getReferenceFromUrl(storageReference.toString());
        final StorageReference pathReference = storageRef.child("images/" + imgurl);

        final Context context = getApplicationContext();
        final File rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()+ "/" + app_name);
        if (!rootPath.exists()) {
            if (!rootPath.mkdirs()) {
                Log.e("firebase ", "Creating dir failed");
            }
        }

        pathReference.getMetadata().addOnSuccessListener(storageMetadata -> {
            String mimeType = storageMetadata.getContentType();
            String currentDateAndTime = sdf.format(new Date());
            final String filename = app_name + "_" + currentDateAndTime.substring(0,15) + "." + mimeType.substring(6);
            final File localFile = new File(rootPath,filename);

            pathReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                notifyGallery(localFile.getAbsolutePath());
                if (type == ImageOperations.PICK_IMAGE_REQUEST) {
                    createSnackbar(localFile, mimeType, getResources().getString(R.string.imagesaved));
                }
            }).addOnFailureListener(exception -> {
                Toast.makeText(context, R.string.savingimagefailed, Toast.LENGTH_SHORT).show();
                Log.e("firebase", "Saving image failed: " + exception);
            });
        });
    }

    private void notifyGallery(String path) {
        MediaScannerConnection.scanFile(getApplicationContext(), new String[] { path }, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (!fileOperations.readFromFile(String.format(FileOperations.favFilePattern, roomKey)).equals("1")) {
            if (!fileOperations.readFromFile(String.format(FileOperations.muteFilePattern, roomKey)).equals("1")) {
                inflater.inflate(R.menu.menu_chatroom, menu);
            } else {
                inflater.inflate(R.menu.menu_chatroom_unmute, menu);
            }
        } else {
            if (!fileOperations.readFromFile(String.format(FileOperations.muteFilePattern, roomKey)).equals("1")) {
                inflater.inflate(R.menu.menu_chatroom_unfav, menu);
            } else {
                inflater.inflate(R.menu.menu_chatroom_unmute_unfav, menu);
            }
        }

        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        MenuItem search_item = menu.findItem(R.id.roomsearch);
        searchView = (SearchView) search_item.getActionView();
        searchView.setFocusable(false);
        searchView.setQueryHint(getResources().getString(R.string.searchmessage));
        ImageView searchClose = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchClose.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                TextView noMessageFound = findViewById(R.id.no_message_found);
                if (!s.trim().isEmpty()) {
                    searchResultList = searchMessage(s);

                    if (!searchResultList.isEmpty()) {
                        mAdapter = new MessageAdapter(searchResultList);
                        recyclerView.setAdapter(mAdapter);
                        recyclerView.setVisibility(View.VISIBLE);
                        noMessageFound.setVisibility(View.GONE);
                        mAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(searchResultList.size() - 1);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        noMessageFound.setVisibility(View.VISIBLE);
                    }
                } else {
                    mAdapter = new MessageAdapter(messageList);
                    recyclerView.setVisibility(View.VISIBLE);
                    noMessageFound.setVisibility(View.GONE);
                    recyclerView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.roominfo) {
            openInfo();
            return true;
        } else if (item.getItemId() == R.id.roomfav) {
            markAsFav();
            return true;
        } else if (item.getItemId() == R.id.roomsearch) {
            return super.onOptionsItemSelected(item);
        } else if (item.getItemId() == R.id.roomphotos) {
            openImageList();
            return true;
        } else if (item.getItemId() == R.id.roompinboard) {
            openPinboard();
            return true;
        } else if (item.getItemId() == R.id.roommute) {
            muteRoom();
            return true;
        } else if (item.getItemId() == R.id.roombackup) {
            if (messageList.size() > 1) {
                writeBackup(createBackup());
            } else {
                Toast.makeText(this, R.string.nomessagesfound, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == R.id.roomleave) {
            leaveRoom();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(userReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(forwardReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(fullscreenReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(jumppinnedReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(closeFullscreenReceiver);
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            if (settings.getBoolean(MainActivity.settingsSaveEnteredTextKey, true)) {
                fileOperations.writeToFile(input_msg.getText().toString().trim().replaceAll("\\n", "<br />"), String.format(FileOperations.currentInputFilePattern, roomKey));
                if (!input_msg.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, R.string.messagesaved, Toast.LENGTH_SHORT).show();
                }
            }
            if (!messageList.isEmpty()) {
                fileOperations.writeToFile(messageList.get(messageList.size() - 1).getKey(), String.format(FileOperations.newestMessageFilePattern, roomKey));
            }
            fileOperations.writeToFile("0", FileOperations.currentRoomFile);
            startActivity(new Intent(ChatActivity.this, MainActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void openInfo() {
        LayoutInflater inflater= LayoutInflater.from(this);
        View view=inflater.inflate(R.layout.room_info, null);

        final TextView room_desc = view.findViewById(R.id.room_description);
        final TextView room_cat = view.findViewById(R.id.room_cat);
        final TextView room_creation = view.findViewById(R.id.room_creation);
        final TextView room_amount_messages = view.findViewById(R.id.room_amount_messages);

        final ListView memberListView = view.findViewById(R.id.memberList);
        memberListView.setAdapter(new MemberListAdapter(getApplicationContext(), memberList, room.getAdmin()));

        String time = room.getTime().substring(6, 8) + "." + room.getTime().substring(4, 6) + "." + room.getTime().substring(0, 4);
        room_desc.setText(room.getDesc());
        room_cat.setText(getResources().getStringArray(R.array.categories)[Integer.parseInt(room.getCategory())]);
        room_creation.setText(time);
        room_amount_messages.setText(String.valueOf(messageCount));

        AlertDialog.Builder builder;
        if (theme == Theme.DARK) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setCustomTitle(setupHeader(getResources().getString(R.string.roominfo)));
        builder.setView(view);
        builder.setPositiveButton(R.string.close, null);
        if (room.getAdmin().equals(userID)) {
            builder.setNegativeButton(R.string.editroom, (dialogInterface, i) -> editRoom());
        }
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(forwardReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fullscreenReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(jumppinnedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeFullscreenReceiver);
        if (settings.getBoolean(MainActivity.settingsSaveEnteredTextKey, true)) {
            fileOperations.writeToFile(input_msg.getText().toString().trim().replaceAll("\\n", "<br />"), String.format(FileOperations.currentInputFilePattern, roomKey));
        }
        fileOperations.writeToFile("0", FileOperations.currentRoomFile);
        if ((messageList.size() - 1) >= 0) {
            fileOperations.writeToFile(messageList.get(messageList.size() - 1).getKey(), String.format(FileOperations.newestMessageFilePattern, roomKey));
        }
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        if (settings.getBoolean(MainActivity.settingsSaveEnteredTextKey, true)) {
            fileOperations.writeToFile(input_msg.getText().toString().trim().replaceAll("\\n", "<br />"), String.format(FileOperations.currentInputFilePattern, roomKey));
        }
        if (!messageList.isEmpty()) {
            fileOperations.writeToFile(messageList.get(messageList.size() - 1).getKey(), String.format(FileOperations.newestMessageFilePattern, roomKey));
        }
        fileOperations.writeToFile("0", FileOperations.currentRoomFile);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        fileOperations.writeToFile("0", FileOperations.currentRoomFile);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        fileOperations.writeToFile(roomKey, FileOperations.currentRoomFile);
        super.onResume();
    }

    private ArrayList<Message> searchMessage(String text) {
        ArrayList<Message> searchedMessageList = new ArrayList<>();
        if (!lastSearch.isEmpty() && text.contains(lastSearch)) {
            for (Message m : searchResultList) {
                searchStringInMessage(searchedMessageList, m, text);
            }
        } else {
            for (Message m : messageList) {
                searchStringInMessage(searchedMessageList, m, text);
            }
        }

        lastSearch = text;
        return searchedMessageList;
    }

    private ArrayList<Message> searchStringInMessage(ArrayList<Message> searchedMessageList, Message m, String text) {
        if (m.getMsg().toLowerCase().contains(text.toLowerCase()) && m.getType() != Message.Type.HEADER && !Message.isImage(m.getType())) {
            if (searchedMessageList.isEmpty() || !searchedMessageList.get(searchedMessageList.size() - 1).getTime().substring(0, 8).equals(m.getTime().substring(0, 8))) {
                String time = m.getTime().substring(6, 8) + "." + m.getTime().substring(4, 6) + "." + m.getTime().substring(0, 4);
                Message m2 = new Message(user, time, m.getTime(), false, "-", Message.Type.HEADER, "", "", "", m.isPinned());
                searchedMessageList.add(m2);
            }
            Message m2;
            if (Message.isConMessage(m.getType())) {
                m2 = new Message(m.getUser(), m.getMsg(), m.getTime(), m.isSender(), m.getKey(), Message.getNonConTypeForConType(m.getType()), m.getQuote_name(), m.getQuote_message(), m.getQuote_key(), m.isPinned());
            } else {
                m2 = new Message(m.getUser(), m.getMsg(), m.getTime(), m.isSender(), m.getKey(), m.getType(), m.getQuote_name(), m.getQuote_message(), m.getQuote_key(), m.isPinned());
            }
            m2.setSearchString(text);
            searchedMessageList.add(m2);
        }
        return searchedMessageList;
    }

    private String createBackup() {
        String currentDateAndTime = sdf_local.format(new Date());
        String fcdat = currentDateAndTime.substring(0, 4) + "." + currentDateAndTime.substring(4, 6) + "." + currentDateAndTime.substring(6, 8) + " " + currentDateAndTime.substring(9, 11) + ":" + currentDateAndTime.substring(11, 13) + ":" + currentDateAndTime.substring(13, 15);
        String ftime = room.getTime().substring(0, 4) + "." + room.getTime().substring(4, 6) + "." + room.getTime().substring(6, 8) + " " + room.getTime().substring(9, 11) + ":" + room.getTime().substring(11, 13) + ":" + room.getTime().substring(13, 15);
        String backup = getResources().getString(R.string.backupof) + " " + roomName + "\n" + getResources().getString(R.string.createdon) + ": " + fcdat + "\n\n" +
                getResources().getString(R.string.category) + ": " + getResources().getStringArray(R.array.categories)[Integer.parseInt(room.getCategory())] + "\n" + getResources().getString(R.string.admin) + ": " + getUser(room.getAdmin()).getName() + "\n" + getResources().getString(R.string.foundation) + ": " + ftime + "\n" + getResources().getString(R.string.sentmessages) + ": " + messageCount + "\n----------------------------------------\n";

        String newDay = "";
        for (Message m : messageList) {
            String btimeDay = "";
            if (m.getType() != Message.Type.HEADER) {
                btimeDay = m.getTime().substring(0, 4) + "." + m.getTime().substring(4, 6) + "." + m.getTime().substring(6, 8);
                String btime = m.getTime().substring(9, 11) + ":" + m.getTime().substring(11, 13) + ":" + m.getTime().substring(13, 15);
                if (!newDay.equals(btimeDay)) {
                    backup += "\n" + btimeDay + "\n";
                }
                if (Message.isQuote(m.getType()) || m.getType() == Message.Type.QUOTE_IMAGE_RECEIVED_CON || m.getType() == Message.Type.QUOTE_IMAGE_SENT_CON) {
                    String quote = m.getQuote_message();
                    if (quote.length() > 40) {
                        quote = quote.substring(0, 40) + "...";
                    }
                    backup += btime + " - [" + m.getQuote_name() + ": " + quote + "] - "  + m.getUser().getName() + ": " + m.getMsg() + "\n";
                } else {
                    backup += btime + " - " + m.getUser().getName() + ": " + m.getMsg() + "\n";
                }
            }
            newDay = btimeDay;
        }

        return backup;
    }

    private void writeBackup(String text) {
        if (isStoragePermissionGranted(2)) {
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/" + getResources().getString(R.string.app_name) + "/");

            if (!path.exists()) {
                path.mkdirs();
            }

            final File file = new File(path, getResources().getString(R.string.app_name) + "_" + roomName.replace(" ", "") + "_backup.txt");

            try {
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(text);

                myOutWriter.close();

                fOut.flush();
                fOut.close();

                createSnackbar(file, "text/plain", getResources().getString(R.string.backupcreated));
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

    private void createSnackbar(final File file, final String mime, final String text) {
        Snackbar snack = Snackbar.make(layout, text, Snackbar.LENGTH_LONG).setActionTextColor(getResources().getColor(R.color.white))
                .setAction(R.string.open, view -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri fileURI = FileProvider.getUriForFile(ChatActivity.this,
                            BuildConfig.APPLICATION_ID + ".fileprovider",
                            file);
                    intent.setDataAndType(fileURI, mime);
                    intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_GRANT_WRITE_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
        View sbView = snack.getView();
        if (theme == Theme.DARK) {
            sbView.setBackgroundColor(getResources().getColor(R.color.dark_actionbar));
        } else {
            sbView.setBackgroundColor(getResources().getColor(R.color.red));
        }
        snack.show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mAdapter = new MessageAdapter(messageList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.scrollToPosition(messageList.size() - 1);

        if (imageListOpened) {
            imageListAlert.dismiss();
            openImageList();
        }

        setBackgroundImage();
    }

    private void markAsFav() {
        Intent intent = new Intent("favroom");
        intent.putExtra("roomKey", roomKey);
        intent.putExtra("roomName", roomName);
        intent.putExtra("admin", room.getAdmin());
        intent.putExtra("category", room.getCategory());
        String creationTime = "";
        try {
            creationTime = sdf.format(sdf.parse(room.getTime()));
        } catch (ParseException e) {
            Log.e("ParseException", e.toString());
        }
        intent.putExtra("newestMessage", creationTime);
        intent.putExtra("passwd", room.getPasswd());
        Message newest = messageList.get(messageList.size() - 1);
        if (messageList.size()!=1) {
            intent.putExtra("nmMessage", newest.getMsg());
            String parsedTime = "";
            try {
                parsedTime = sdf.format(sdf.parse(newest.getTime()));
            } catch (ParseException e) {
                Log.e("ParseException", e.toString());
            }
            intent.putExtra("nmTime", parsedTime);
            intent.putExtra("nmKey", newest.getKey());
            intent.putExtra("nmType", newest.getType().toString());
        } else {
            intent.putExtra("nmMessage", "");
            intent.putExtra("nmTime", "");
            intent.putExtra("nmKey", "");
            intent.putExtra("nmType", Message.Type.HEADER.toString());
        }
        if (!fileOperations.readFromFile(String.format(FileOperations.favFilePattern, roomKey)).equals("1")) {
            fileOperations.writeToFile("1", String.format(FileOperations.favFilePattern, roomKey));
            Toast.makeText(this, R.string.addedtofavorites, Toast.LENGTH_SHORT).show();
        } else {
            fileOperations.writeToFile("0", String.format(FileOperations.favFilePattern, roomKey));
            Toast.makeText(this, R.string.removedfromfavorites, Toast.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void addUser(DataSnapshot dataSnapshot) {
        User user = dataSnapshot.getValue(User.class);
        user.setUserID(dataSnapshot.getKey());
        userList.add(user);

        userListCreated = true;
    }

    public void addUser(final String key, final String user_id, final String chat_msg, final String img, final String pin, final String quote, final String time, final MyCallback myCallback) {
        userRoot.child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                user.setUserID(user_id);
                userList.add(user);
                myCallback.onCallback(key, user, time, chat_msg, img, pin, quote);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showProfile(final String user_ID) {
        User user = getUser(user_ID);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.profile, null);

        CircleImageView profileIcon = view.findViewById(R.id.icon_profile);
        TextView profileName = view.findViewById(R.id.name_profile);
        TextView profileDescription = view.findViewById(R.id.profile_bio);
        TextView birthday = view.findViewById(R.id.profile_birthday);
        TextView location = view.findViewById(R.id.profile_location);
        ImageView banner = view.findViewById(R.id.background_profile);
        AlertDialog.Builder builder;

        if (theme == Theme.DARK) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
            banner.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.side_nav_bar_dark, null));
        } else {
            builder = new AlertDialog.Builder(this);
            banner.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.side_nav_bar, null));
        }

        StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        final StorageReference pathReference_banner = storageRef.child("profile_banners/" + user.getBanner());
        GlideApp.with(getApplicationContext())
                .load(pathReference_banner)
                .centerCrop()
                .thumbnail(0.05f)
                .into(banner);

        final StorageReference pathReference_image = storageRef.child("profile_images/" + user.getImg());
        GlideApp.with(getApplicationContext())
                .load(pathReference_image)
                .centerCrop()
                .into(profileIcon);

        profileIcon.setOnClickListener(v -> showFullscreenImage(user_ID, 0));
        banner.setOnClickListener(v -> showFullscreenImage(user_ID, 1));

        profileName.setText(user.getName());
        profileDescription.setText(user.getProfileDescription());
        birthday.setText(user.getBirthday().substring(6, 8) + "." + user.getBirthday().substring(4, 6) + "." + user.getBirthday().substring(0, 4));
        location.setText(user.getLocation());

        builder.setCustomTitle(setupHeader(getResources().getString(R.string.profile, user.getName())));
        builder.setView(view);
        builder.setPositiveButton(R.string.close, null);

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void forwardMessage(final String messageID) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.forward_message, null);

        final ListView listView = view.findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roomList));
        AlertDialog.Builder builder;
        if (theme == Theme.DARK) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setCustomTitle(setupHeader(getResources().getString(R.string.forwardmessage)));
        builder.setView(view);
        builder.setPositiveButton(R.string.cancel, null);
        final AlertDialog alert = builder.create();
        alert.show();
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            int position = listView.getPositionForView(view1);
            String roomKey = roomKeysList.get(position);
            Message fMessage = new Message();
            for (Message m : messageList) {
                if (m.getKey().equals(messageID)) {
                    fMessage = m;
                    break;
                }
            }
            String newMessageKey = roomRoot.child(roomKey).push().getKey();

            String currentDateAndTime = sdf.format(new Date());

            DatabaseReference message_root = roomRoot.child(roomKey).child(newMessageKey);
            Map<String, Object> map = new HashMap<>();
            map.put("name", userID);
            if (Message.isImage(fMessage.getType())) {
                map.put("msg", "");
                map.put("img", fMessage.getMsg());
            } else {
                map.put("msg", "(Forwarded) " + fMessage.getMsg());
                map.put("img", "");
            }
            map.put("pinned", false);
            map.put("quote", quoteStatus);
            map.put("time", currentDateAndTime);

            message_root.updateChildren(map);
            fileOperations.writeToFile(newMessageKey, String.format(FileOperations.newestMessageFilePattern, roomKey));
            alert.cancel();
            if (Message.isImage(fMessage.getType())) {
                Toast.makeText(ChatActivity.this, R.string.imageforwarded, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatActivity.this, R.string.messageforwarded, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private User getUser(final String user_id) {
        for (User u : userList) {
            if (u.getUserID().equals(user_id)) {
                return u;
            }
        }
        return null;
    }

    private void showFullscreenImage(String image, int type) {
        final View dialogView = getLayoutInflater().inflate(R.layout.fullscreen_image, null);
        if (theme == Theme.DARK) {
            fullscreendialog = new Dialog(this,R.style.FullScreenImageDark);
        } else {
            fullscreendialog = new Dialog(this,R.style.FullScreenImage);
        }
        fullscreendialog.setContentView(dialogView);

        final View decorView = fullscreendialog.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);

        decorView.setOnSystemUiVisibilityChangeListener(i -> {
            if (fullscreendialog.isShowing()) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    int uiOptions1 = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE;
                    decorView.setSystemUiVisibility(uiOptions1);
                }, 2000);
            }
        });

        CatchViewPager mViewPager = dialogView.findViewById(R.id.pager);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        if (type == 0) {
            ArrayList<String> images = new ArrayList<>();
            images.add(image);
            mViewPager.setAdapter(new FullScreenImageAdapter(this, images, 0));
        } else if (type == 1) {
            ArrayList<String> images = new ArrayList<>();
            images.add(image);
            mViewPager.setAdapter(new FullScreenImageAdapter(this, images, 1));
        } else {
            mViewPager.setAdapter(new FullScreenImageAdapter(this, imageList, 2));
            mViewPager.setCurrentItem(imageList.indexOf(image));
        }

        fullscreendialog.show();
    }

    private void openImageList() {
        if (!imageList.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.image_list, null);
            GridView imageGrid = view.findViewById(R.id.gridview);
            imageGrid.setAdapter(new ImageAdapter(this, imageList));
            imageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    showFullscreenImage(imageList.get(i), 2);
                }
            });
            AlertDialog.Builder builder;
            if (theme == Theme.DARK) {
                builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
            } else {
                builder = new AlertDialog.Builder(this);
            }
            builder.setCustomTitle(setupHeader(getResources().getString(R.string.images)));
            builder.setView(view);
            builder.setPositiveButton(R.string.close, (dialogInterface, i) -> imageListOpened = false);
            imageListAlert = builder.create();
            imageListAlert.show();
            imageListOpened = true;
        } else {
            Toast.makeText(this, R.string.noimagesfound, Toast.LENGTH_SHORT).show();
        }
    }

    private void openPinboard() {
        if (!pinnedList.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.pinboard, null);

            ListView pinListView = view.findViewById(R.id.pinboardList);
            pinListView.setAdapter(new PinboardAdapter(this, pinnedList));
            AlertDialog.Builder builder;
            if (theme == Theme.DARK) {
                builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
            } else {
                builder = new AlertDialog.Builder(this);
            }
            builder.setCustomTitle(setupHeader(getResources().getString(R.string.pinboard)));
            builder.setView(view);
            builder.setPositiveButton(R.string.close, null);
            pinboardAlert = builder.create();
            pinboardAlert.show();
        } else {
            Toast.makeText(this, R.string.nopinnedmessagesfound, Toast.LENGTH_SHORT).show();
        }

    }

    private void pinMessage(String messageToPin) {
        for (Message m : messageList) {
            if (m.getKey().equals(messageToPin)) {
                boolean removed = false;
                for (Message m2 : pinnedList) {
                    if (m2.getKey().equals(m.getKey())) {
                        m.setPinned(false);
                        pinnedList.remove(m2);

                        Map<String, Object> map = new HashMap<String, Object>();
                        DatabaseReference message_root = root.child(m.getKey());
                        map.put("pinned", false);
                        message_root.updateChildren(map);

                        Toast.makeText(getApplicationContext(), R.string.messageunpinned, Toast.LENGTH_SHORT).show();

                        removed = true;
                        break;
                    }
                }
                if (!removed) {
                    m.setPinned(true);
                    if (!pinnedList.isEmpty()) {
                        int index = 0;
                        for (Message pm : pinnedList) {
                            if (Long.parseLong(pm.getTime().substring(0, 8) + pm.getTime().substring(9, 15)) > Long.parseLong(m.getTime().substring(0, 8) + m.getTime().substring(9, 15))) {
                                break;
                            } else {
                                index++;
                            }
                        }
                        pinnedList.add(index, m);
                    } else {
                        pinnedList.add(m);
                    }

                    Map<String, Object> map = new HashMap<String, Object>();
                    DatabaseReference message_root = root.child(m.getKey());
                    map.put("pinned", true);
                    message_root.updateChildren(map);

                    Toast.makeText(getApplicationContext(), R.string.messagepinned, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void muteRoom() {
        if (!fileOperations.readFromFile(String.format(FileOperations.muteFilePattern, roomKey)).equals("1")) {
            fileOperations.writeToFile("1", String.format(FileOperations.muteFilePattern, roomKey));
            FirebaseMessaging.getInstance().unsubscribeFromTopic(roomKey);
            Toast.makeText(this, R.string.roommuted, Toast.LENGTH_SHORT).show();
        } else {
            fileOperations.writeToFile("0", String.format(FileOperations.muteFilePattern, roomKey));
            FirebaseMessaging.getInstance().subscribeToTopic(roomKey);
            Toast.makeText(this, R.string.roomunmuted, Toast.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();
    }

    private void leaveRoom() {
        AlertDialog.Builder builder;
        if (theme == Theme.DARK) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(R.string.reallyleaveroom);
        builder.setPositiveButton(R.string.yes, (dialogInterface, which) -> {
            fileOperations.writeToFile("", String.format(FileOperations.passwordFilePattern, roomKey));
            startActivity(new Intent(ChatActivity.this, MainActivity.class));
            if (fileOperations.readFromFile(String.format(FileOperations.favFilePattern, roomKey)).equals("1")) {
                markAsFav();
            }
            Intent intent = new Intent("leaveroom");
            intent.putExtra("roomkey", roomKey);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            Toast.makeText(getApplicationContext(), R.string.roomleft, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.no, null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void editRoom() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.add_room, null);

        final EditText edit_room_name = view.findViewById(R.id.room_name);
        final EditText edit_room_desc = view.findViewById(R.id.room_description);
        final EditText edit_room_password = view.findViewById(R.id.room_password);
        final EditText edit_room_password_repeat = view.findViewById(R.id.room_password_repeat);

        final TextInputLayout room_name_layout = view.findViewById(R.id.room_name_layout);
        final TextInputLayout room_desc_layout = view.findViewById(R.id.room_description_layout);
        final TextInputLayout room_password_layout = view.findViewById(R.id.room_password_layout);
        final TextInputLayout room_password_repeat_layout = view.findViewById(R.id.room_password_repeat_layout);

        edit_room_name.setText(room.getName());
        edit_room_desc.setText(room.getDesc());
        edit_room_password.setText(room.getPasswd());
        edit_room_password_repeat.setText(room.getPasswd());

        final Spinner spinner = view.findViewById(R.id.spinner);
        roomImageButton = view.findViewById(R.id.room_image);

        spinner.setSelection(Integer.parseInt(room.getCategory()));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,int position, long id) {
                String category = adapter.getItemAtPosition(position).toString();
                String[] categories = getResources().getStringArray(R.array.categories);
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(category)) {
                        katindex = i;
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        edit_room_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    room_name_layout.setError(null);
                }
            }
        });
        edit_room_desc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    room_desc_layout.setError(null);
                }
            }
        });
        edit_room_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    room_password_layout.setError(null);
                }
            }
        });
        edit_room_password_repeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    room_password_repeat_layout.setError(null);
                }
            }
        });

        StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        final StorageReference pathReference_image = storageRef.child("room_images/" + room.getImg());

        if (theme == Theme.DARK) {
            GlideApp.with(getApplicationContext())
                    .load(pathReference_image)
                    .placeholder(R.drawable.side_nav_bar_dark)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(roomImageButton);
        } else {
            GlideApp.with(getApplicationContext())
                    .load(pathReference_image)
                    .placeholder(R.drawable.side_nav_bar)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(roomImageButton);
        }

        roomImageButton.setOnClickListener(view13 -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), ImageOperations.PICK_ROOM_IMAGE_REQUEST);
        });

        AlertDialog.Builder builder;
        if (theme == Theme.DARK) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setCustomTitle(setupHeader(getResources().getString(R.string.editroom)));
        builder.setCancelable(false);
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm, (dialogInterface, i) -> {});
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            View view1 = ((AlertDialog) dialogInterface).getCurrentFocus();
            if (view1 != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
            }
            dialogInterface.cancel();
            openInfo();
        });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(dialogInterface -> {

            Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view12 -> {
                final String roomName = edit_room_name.getText().toString().trim();
                final String roomPassword = edit_room_password.getText().toString().trim();
                final String roomPasswordRepeat = edit_room_password_repeat.getText().toString().trim();
                final String roomDescription = edit_room_desc.getText().toString().trim();
                if (!roomName.isEmpty()) {
                    if (!roomDescription.isEmpty()) {
                        if (katindex!=0) {
                            if (!roomPassword.isEmpty()) {
                                if (!roomPasswordRepeat.isEmpty()) {
                                    if (roomPassword.equals(roomPasswordRepeat)) {
                                        if (view12 != null) {
                                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                            imm.hideSoftInputFromWindow(view12.getWindowToken(), 0);
                                        }
                                        DatabaseReference message_root = FirebaseDatabase.getInstance().getReference().child("rooms").child(roomKey).child(roomDataKey);
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("name", roomName);
                                        map.put("passwd", roomPassword);
                                        map.put("desc", roomDescription);
                                        map.put("category", String.valueOf(katindex));
                                        message_root.updateChildren(map);
                                        fileOperations.writeToFile(roomPassword, String.format(FileOperations.passwordFilePattern, roomKey));
                                        setTitle(roomName);
                                        Toast.makeText(getApplicationContext(), R.string.roomedited, Toast.LENGTH_SHORT).show();
                                        room.setDesc(roomDescription);
                                        room.setCategory(String.valueOf(katindex));
                                        room.setPasswd(roomPassword);
                                        alert.cancel();
                                        openInfo();
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.passwordsdontmatch, Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    room_password_repeat_layout.setError(getResources().getString(R.string.repeatpassword));
                                }
                            } else {
                                room_password_layout.setError(getResources().getString(R.string.enterpassword));
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.selectcategory, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        room_desc_layout.setError(getResources().getString(R.string.enterroomdesc));
                    }
                } else {
                    room_name_layout.setError(getResources().getString(R.string.enterroomname));
                }
            });
        });
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alert.show();
    }

    private TextView setupHeader(String title) {
        TextView header = new TextView(this);

        if (theme == Theme.DARK) {
            header.setBackgroundColor(getResources().getColor(R.color.dark_button));
        } else {
            header.setBackgroundColor(getResources().getColor(R.color.red));
        }

        header.setText(title);
        header.setPadding(30, 30, 30, 30);
        header.setTextSize(20F);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Color.WHITE);

        return header;
    }
}