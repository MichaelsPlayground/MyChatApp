package com.yannick.mychatapp.adapters;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yannick.mychatapp.Constants;
import com.yannick.mychatapp.GlideApp;
import com.yannick.mychatapp.data.Message;
import com.yannick.mychatapp.PatternEditableBuilder;
import com.yannick.mychatapp.R;
import com.yannick.mychatapp.data.Theme;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import at.blogc.android.views.ExpandableTextView;
import de.hdodenhof.circleimageview.CircleImageView;
import ru.whalemare.sheetmenu.SheetMenu;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messageList;
    private final ArrayList<MessageViewHolder> holderList = new ArrayList<>();
    private Context context;
    private FirebaseStorage storage;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
    private final FirebaseAuth mAuth;

    public class MessageViewHolder extends RecyclerView.ViewHolder{
        private final TextView name, message, time, quoteName, quoteMessage;
        private final LinearLayout quoteBox, messageBox;
        private final Button messagebutton;
        private final ExpandableTextView messageExpandable;
        private final ImageView image;
        private final Theme theme;
        private int pos;
        private final GestureDetector gestureDetector;
        private final CircleImageView profileImage, quoteProfileImage;
        private String messageKey;

        @SuppressLint("ClickableViewAccessibility")
        private MessageViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.text_message_name);
            message = view.findViewById(R.id.text_message_body);
            time = view.findViewById(R.id.text_message_time);
            profileImage = view.findViewById(R.id.image_message_profile);
            image = view.findViewById(R.id.image);
            quoteMessage = view.findViewById(R.id.quote_message);
            quoteName = view.findViewById(R.id.quote_name);
            quoteProfileImage = view.findViewById(R.id.quote_profile_image);
            quoteBox = view.findViewById(R.id.quotebox);
            messageBox = view.findViewById(R.id.messagebox);
            messagebutton = view.findViewById(R.id.messagebutton);
            messageExpandable = view.findViewById(R.id.text_message_body_expandable);

            gestureDetector = new GestureDetector(context, new SingleTapConfirm());

            storage = FirebaseStorage.getInstance();

            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            theme = Theme.getCurrentTheme(context);

            view.setOnTouchListener((messageView, event) -> {
                pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    return gestureDetector.onTouchEvent(event);
                }
                return false;
            });

            if (message != null) {
                message.setOnTouchListener((messageView, event) -> {
                    pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            GradientDrawable shape;
                            if (action == MotionEvent.ACTION_DOWN) {
                                if (messageList.get(pos).isSender(mAuth.getCurrentUser().getUid())) {
                                    shape = getMessageBox(R.color.textbox_sent_clicked);
                                } else {
                                    shape = getMessageBox(R.color.textbox_received_clicked);
                                }
                            } else {
                                if (messageList.get(pos).isSender(mAuth.getCurrentUser().getUid())) {
                                    shape = getMessageBox(R.color.textbox_sent);
                                } else {
                                    shape = getMessageBox(R.color.textbox_received);
                                }
                            }

                            Message.Type type = messageList.get(pos).getType();
                            if (Message.isQuote(type)
                                    || Message.isDeletedQuote(type)
                                    || Message.isQuoteImage(type)) {
                                quoteBox.setBackground(shape);
                            } else if (Message.isForwardedMessage(type)
                                    || Message.isLinkPreview(type)
                                    || Message.isExpandable(type)
                                    || Message.isForwardedExpandable(type)) {
                                messageBox.setBackground(shape);
                            } else if (Message.isBasicMessage(type)) {
                                message.setBackground(shape);
                            }
                        }

                        return gestureDetector.onTouchEvent(event);
                    }
                    return true;
                });
            }

            if (messageExpandable != null) {
                messageExpandable.setOnTouchListener((expandableView, event) -> {
                    pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN) {
                            if (messageList.get(pos).isSender(mAuth.getCurrentUser().getUid())) {
                                messageBox.setBackground(getMessageBox(R.color.textbox_sent_clicked));
                            } else {
                                messageBox.setBackground(getMessageBox(R.color.textbox_received_clicked));
                            }
                        }
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            if (messageList.get(pos).isSender(mAuth.getCurrentUser().getUid())) {
                                messageBox.setBackground(getMessageBox(R.color.textbox_sent));
                            } else {
                                messageBox.setBackground(getMessageBox(R.color.textbox_received));
                            }
                        }
                        return gestureDetector.onTouchEvent(event);
                    }
                    return true;
                });
            }

            if (image != null) {
                image.setOnTouchListener((imageView, event) -> {
                    pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN) {
                            image.setForeground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.image_overlay, null));
                        }
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            image.setForeground(null);
                        }
                        return gestureDetector.onTouchEvent(event);
                    }
                    return true;
                });
            }

            if (profileImage != null) {
                profileImage.setOnTouchListener((profileImageView, event) -> {
                    pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN) {
                            profileImage.setForeground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.image_overlay_profile, null));
                        }
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            if (action == MotionEvent.ACTION_UP) {
                                Intent intent = new Intent("userprofile");
                                intent.putExtra("userid", messageList.get(pos).getUser().getUserID());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }

                            profileImage.setForeground(null);
                        }
                    }
                    return true;
                });
            }
        }

        public void runSelectedMenuOption(Message.Type type, MenuItem item, Message message) {
            if (item.getItemId() == R.id.copy) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", message.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, R.string.messagecopied, Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.openprofile) {
                Intent intent = new Intent("userprofile");
                intent.putExtra("userid", message.getUser().getUserID());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.forward) {
                Intent intent = new Intent("forward");
                intent.putExtra("forwardID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.pin) {
                Intent intent = new Intent("pinMessage");
                intent.putExtra("pinID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.jump) {
                Intent intent = new Intent("quotedMessage");
                intent.putExtra("quoteID", message.getQuotedMessage().getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    for (MessageViewHolder m : holderList) {
                        if (m.messageKey.equals(message.getQuotedMessage().getKey())) {
                            m.image.setForeground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.image_overlay, null));
                            handler.postDelayed(() -> m.image.setForeground(null), 1000);
                            break;
                        }
                    }
                }, 100);
            } else if (item.getItemId() == R.id.quote) {
                Intent intent = new Intent("quote");
                intent.putExtra("quoteID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.download && Message.isImage(type)) {
                Intent intent = new Intent("permission");
                intent.putExtra("imageURL", message.getText());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.download && Message.isQuoteImage(type)) {
                Intent intent = new Intent("permission");
                intent.putExtra("imageURL", message.getQuotedMessage().getText());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.delete_message) {
                Intent intent = new Intent("delete");
                intent.putExtra("messageID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }

        public void openSheetMenu() {
            Message clickedDataItem = messageList.get(pos);
            Message.Type type = clickedDataItem.getType();

            if (type != Message.Type.HEADER) {
                SheetMenu sheetMenu = new SheetMenu();
                sheetMenu.setTitle(context.getResources().getString(R.string.selectanoption));
                sheetMenu.setClick(item -> {
                    runSelectedMenuOption(type, item, clickedDataItem);
                    return false;
                });

                if (!clickedDataItem.isSender(mAuth.getCurrentUser().getUid())) {
                    if (Message.isImage(type)) {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_image);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_image_unpin);
                        }
                    } else if (Message.isQuoteImage(type)) {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_quote_image);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_quote_image_unpin);
                        }
                    } else {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_message);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_message_unpin);
                        }
                    }
                } else {
                    if (Message.isImage(type)) {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_image_sender);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_image_unpin_sender);
                        }
                    } else if (Message.isQuoteImage(type)) {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_quote_image_sender);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_quote_image_unpin_sender);
                        }
                    } else {
                        if (!clickedDataItem.isPinned()) {
                            sheetMenu.setMenu(R.menu.menu_message_sender);
                        } else {
                            sheetMenu.setMenu(R.menu.menu_message_unpin_sender);
                        }
                    }
                }

                if (theme == Theme.DARK) {
                    sheetMenu.show(new ContextThemeWrapper(context, R.style.SheetDialogDark));
                } else {
                    sheetMenu.show(context);
                }
            }
        }

        public void openFullScreenImage() {
            Message clickedDataItem = messageList.get(pos);
            if (!clickedDataItem.getSearchString().equals("")) {
                Intent intent = new Intent("quotedMessage");
                intent.putExtra("quoteID", clickedDataItem.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                Message.Type type = clickedDataItem.getType();
                if (Message.isImage(type)) {
                    Intent intent = new Intent("fullscreenimage");
                    intent.putExtra("image", clickedDataItem.getText());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else if (Message.isQuoteImage(type)) {
                    Intent intent = new Intent("fullscreenimage");
                    intent.putExtra("image", clickedDataItem.getQuotedMessage().getText());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else if (Message.isQuote(type)) {
                    String quotedMessageKey = clickedDataItem.getQuotedMessage().getKey();
                    Intent intent = new Intent("quotedMessage");
                    intent.putExtra("quoteID", quotedMessageKey);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        for (MessageViewHolder m : holderList) {
                            if (m.messageKey.equals(quotedMessageKey)) {
                                Message quotedMessage = messageList.stream().filter(message -> message.getKey().equals(quotedMessageKey)).findFirst().orElse(null);
                                if (quotedMessage != null) {
                                    if (quotedMessage.isSender(mAuth.getCurrentUser().getUid())) {
                                        m.message.setBackground(getMessageBox(R.color.textbox_sent_clicked));
                                    } else {
                                        m.message.setBackground(getMessageBox(R.color.textbox_received_clicked));
                                    }
                                    handler.postDelayed(() -> {
                                        if (quotedMessage.isSender(mAuth.getCurrentUser().getUid())) {
                                            m.message.setBackground(getMessageBox(R.color.textbox_sent));
                                        } else {
                                            m.message.setBackground(getMessageBox(R.color.textbox_received));
                                        }
                                    }, 1000);
                                }

                                break;
                            }
                        }
                    }, 100);
                }
            }
        }

        public class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                openFullScreenImage();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                openSheetMenu();
                super.onLongPress(event);
            }
        }
    }

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getType().ordinal();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        int layout = context.getResources().obtainTypedArray(R.array.messageLayouts).getResourceId(viewType, -1);
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        MessageViewHolder holder = new MessageViewHolder(itemView);
        holderList.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        Message m = messageList.get(position);
        holder.messageKey = m.getKey();
        Message.Type type = m.getType();
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
        shape.setCornerRadii(corners);
        if (!Message.isImage(type) && !Message.isLinkPreview(type)) {
            if (!m.getSearchString().isEmpty()) {
                SpannableStringBuilder sbuilder = highlightSearchedText(m.getText(), m.getSearchString());
                if (!Message.isExpandable(type) && !Message.isForwardedExpandable(type)) {
                    holder.message.setText(sbuilder);
                } else {
                    holder.messageExpandable.setText(sbuilder);
                    holder.messageExpandable.setInterpolator(new LinearInterpolator());
                    holder.messagebutton.setOnClickListener(view -> {
                        holder.messagebutton.setText(holder.messageExpandable.isExpanded() ? R.string.showmore : R.string.showless);
                        holder.messageExpandable.toggle();
                    });
                }
            } else {
                if (type != Message.Type.HEADER && !Message.isExpandable(type) && !Message.isForwardedExpandable(type)) {
                    try {
                        SpannableStringBuilder newBuilder = styleText(m.getText());
                        holder.message.setText(newBuilder);
                    } catch (IndexOutOfBoundsException ioobe) {
                        holder.message.setText(m.getText());
                    }

                    if (!m.isSender(mAuth.getCurrentUser().getUid())) {
                        new PatternEditableBuilder().
                                addPattern(Pattern.compile("\\@(\\w+)"), Color.BLUE,
                                        text -> {}).into(holder.message);
                    } else {
                        new PatternEditableBuilder().
                                addPattern(Pattern.compile("\\@(\\w+)"), Color.RED,
                                        text -> {}).into(holder.message);
                    }
                } else if (Message.isExpandable(type) || Message.isForwardedExpandable(type)) {
                    holder.messageExpandable.setText(m.getText());
                    holder.messageExpandable.setInterpolator(new LinearInterpolator());
                    holder.messagebutton.setOnClickListener(view -> {
                        holder.messagebutton.setText(holder.messageExpandable.isExpanded() ? R.string.showmore : R.string.showless);
                        holder.messageExpandable.toggle();
                    });
                } else {
                    holder.message.setText(m.getText());
                }
            }
        } else if (Message.isImage(type)) {
            String imageURL = m.getText();
            StorageReference pathReference = storage.getReference().child(Constants.imagesStorageKey + imageURL);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            if (!settings.getBoolean(Constants.settingsPreviewImagesKey, true)) {
                GlideApp.with(context)
                        .load(pathReference)
                        .onlyRetrieveFromCache(true)
                        .placeholder(R.color.lightGrey)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.image);
            } else {
                GlideApp.with(context)
                        .load(pathReference)
                        .placeholder(R.color.lightGrey)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.image);
            }
        } else  {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(m.getText(), String.valueOf(position));
        }
        if (type != Message.Type.HEADER) {
            String time = m.getTime();
            if (!time.isEmpty()) {
                holder.time.setText(time.substring(9, 11) + ":" + time.substring(11, 13));
            } else {
                holder.time.setText("");
            }
            if (!m.isSender(mAuth.getCurrentUser().getUid())) {
                if (!Message.isConMessage(type)) {
                    holder.name.setText(m.getUser().getName());

                    StorageReference refImage = storage.getReference().child(Constants.profileImagesStorageKey + m.getUser().getImage());
                    try {
                        GlideApp.with(context)
                                .load(refImage)
                                .centerCrop()
                                .into(holder.profileImage);
                    } catch (IllegalArgumentException iae) {}
                }
                shape.setColor(ContextCompat.getColor(context, R.color.textbox_received));
            } else {
                shape.setColor(ContextCompat.getColor(context, R.color.textbox_sent));
            }
        } else {
            shape.setColor(ContextCompat.getColor(context, R.color.textbox_time));
        }

        if (Message.isQuote(type) || Message.isDeletedQuote(type) || Message.isQuoteImage(type)) {
            if (!Message.isDeletedQuote(type)) {
                String tempUserId = "";
                for (Message m2 : messageList) {
                    if (m2.getKey().equals(m.getQuotedMessage().getKey())) {
                        tempUserId = m2.getUser().getUserID();
                        break;
                    }
                }
                if (mAuth.getCurrentUser().getUid().equals(tempUserId)) {
                    holder.quoteName.setText(R.string.you);
                } else {
                    holder.quoteName.setText(m.getQuotedMessage().getUser().getName());
                }

                String imageURL = m.getQuotedMessage().getUser().getImage();
                StorageReference pathReference = storage.getReference().child(Constants.profileImagesStorageKey + imageURL);

                GlideApp.with(context)
                        .load(pathReference)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.quoteProfileImage);
            }
            if (!Message.isQuoteImage(type)) {
                holder.quoteMessage.setText(m.getQuotedMessage().getText());
            } else {
                String imageURL = m.getQuotedMessage().getText();
                StorageReference pathReference = storage.getReference().child(Constants.imagesStorageKey + imageURL);

                GlideApp.with(context)
                        .load(pathReference)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.image);
            }

            holder.quoteBox.setBackground(shape);
        }

        if (type == Message.Type.HEADER || Message.isBasicMessage(type)) {
            holder.message.setBackground(shape);
        } else if (Message.isForwardedMessage(type) || Message.isLinkPreview(type) || Message.isExpandable(type) || Message.isForwardedExpandable(type)) {
            holder.messageBox.setBackground(shape);
        }

        if (messageList.size() > position + 1 && Message.isConMessage(messageList.get(position + 1).getType())) {
            holder.time.setText("");
        }
    }

    private GradientDrawable getMessageBox(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
        shape.setCornerRadii(corners);
        shape.setColor(ContextCompat.getColor(context, color));
        return shape;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private static SpannableStringBuilder styleText(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String charBold = "*";
        String charItalic = "_";
        String charStrikethrough = "~";

        String buildertext = text;
        if ((buildertext.length() - buildertext.replace(charBold, "").length())%2 == 0) {
            buildertext = buildertext.replace(charBold, "");
        }
        if ((buildertext.length() - buildertext.replace(charItalic, "").length())%2 == 0) {
            buildertext = buildertext.replace(charItalic, "");
        }

        builder.append(buildertext);

        while ((text.contains(charBold) && text.substring(text.indexOf(charBold) +1).contains(charBold)) || (text.contains(charItalic) && text.substring(text.indexOf(charItalic) +1).contains(charItalic))) {
            if ((text.contains(charBold) && text.substring(text.indexOf(charBold) +1).contains(charBold)) && (text.contains(charItalic) && text.substring(text.indexOf(charItalic) +1).contains(charItalic))) {
                if (text.indexOf(charBold) < text.indexOf(charItalic)) {
                    builder.setSpan(new StyleSpan(Typeface.BOLD), text.indexOf(charBold), text.indexOf(charBold) + text.substring(text.indexOf(charBold) + 1).indexOf(charBold), 0);
                    text = text.replaceFirst(Pattern.quote(charBold), "");
                    text = text.replaceFirst(Pattern.quote(charBold), "");
                } else {
                    builder.setSpan(new StyleSpan(Typeface.ITALIC), text.indexOf(charItalic), text.indexOf(charItalic) + text.substring(text.indexOf(charItalic) + 1).indexOf(charItalic), 0);
                    text = text.replaceFirst(Pattern.quote(charItalic), "");
                    text = text.replaceFirst(Pattern.quote(charItalic), "");
                }
            } else if (text.contains(charBold) && text.substring(text.indexOf(charBold) +1).contains(charBold)) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), text.indexOf(charBold), text.indexOf(charBold) + text.substring(text.indexOf(charBold) + 1).indexOf(charBold), 0);
                text = text.replaceFirst(Pattern.quote(charBold), "");
                text = text.replaceFirst(Pattern.quote(charBold), "");
            } else if (text.contains(charItalic) && text.substring(text.indexOf(charItalic) +1).contains(charItalic)) {
                builder.setSpan(new StyleSpan(Typeface.ITALIC), text.indexOf(charItalic), text.indexOf(charItalic) + text.substring(text.indexOf(charItalic) + 1).indexOf(charItalic), 0);
                text = text.replaceFirst(Pattern.quote(charItalic), "");
                text = text.replaceFirst(Pattern.quote(charItalic), "");
            }
        }

        return builder;
    }

    private SpannableStringBuilder highlightSearchedText(String text, String textToBold) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (textToBold.length() > 0 && !textToBold.trim().equals("")) {
            int startingIndex = text.toLowerCase().indexOf(textToBold.toLowerCase());
            int endingIndex = startingIndex + textToBold.length();
            builder.append(text);
            builder.setSpan(new StyleSpan(Typeface.BOLD), startingIndex, endingIndex, 0);
            builder.setSpan(new ForegroundColorSpan(this.context.getResources().getColor(R.color.text_highlight)), startingIndex, endingIndex, 0);
            return builder;
        } else {
            return builder.append(text);
        }
    }

    private class JsoupAsyncTask extends AsyncTask<String, String, Void> {
        Document htmlDocument;
        String htmlContentInStringFormat;
        String htmlContentInStringFormat2;
        String htmlContentInStringFormat3;
        String htmlContentInStringFormat4;
        int position;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                position = Integer.parseInt(params[1]);
                htmlDocument = Jsoup.connect(params[0]).userAgent("Googlebot/2.1 (+http://www.google.com/bot.html)").get();
                if (htmlDocument != null) {
                    String url = new URL(htmlDocument.location()).getHost();
                    if (url.contains("www.")) {
                        url = url.substring(4);
                    }
                    htmlContentInStringFormat2 = url;
                    Elements meta3e = htmlDocument.select("meta[property=og:image]");
                    Elements meta2e = htmlDocument.select("meta[property=og:description]");
                    Elements metae = htmlDocument.select("meta[property=og:title]");
                    if (!meta2e.isEmpty() && !metae.isEmpty() && !meta3e.isEmpty()) {
                        Element meta3 = meta3e.first();
                        Element meta2 = meta2e.first();
                        Element meta = metae.first();
                        htmlContentInStringFormat4 = meta3.attr("content");
                        htmlContentInStringFormat3 = meta2.attr("content");
                        htmlContentInStringFormat = meta.attr("content");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (htmlDocument != null && !htmlContentInStringFormat.isEmpty() && !htmlContentInStringFormat2.isEmpty() && !htmlContentInStringFormat3.isEmpty() && !htmlContentInStringFormat4.isEmpty()) {
                Log.d("HEYHY", "Titel: " + htmlContentInStringFormat);
                Log.d("HEYHY", "Seite: " + htmlContentInStringFormat2);
                Log.d("HEYHY", "Teaser: " + htmlContentInStringFormat3);
                Log.d("HEYHY", "Bild: " + htmlContentInStringFormat4);
                holderList.get(position).message.setText(htmlContentInStringFormat3);
            }
        }
    }
}