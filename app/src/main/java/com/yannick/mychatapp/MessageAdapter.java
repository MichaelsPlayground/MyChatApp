package com.yannick.mychatapp;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    private final List<Message> messageList;
    private final ArrayList<MyViewHolder> holderList = new ArrayList<>();
    private Context context;
    private FirebaseStorage storage;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
    private final FirebaseAuth mAuth;

    private final FileOperations fileOperations = new FileOperations(context);

    public class MyViewHolder extends RecyclerView.ViewHolder{
        private final TextView name, msg, time, quote_name, quote_message;
        private final LinearLayout quotebox, quotebox_content, messagebox;
        private final Button messagebutton;
        private final ExpandableTextView msg_exp;
        private final ImageView img;
        private final Theme theme;
        private int pos;
        private final GestureDetector gestureDetector;
        private final CircleImageView profile_image;

        @SuppressLint("ClickableViewAccessibility")
        private MyViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.text_message_name);
            msg = view.findViewById(R.id.text_message_body);
            time = view.findViewById(R.id.text_message_time);
            profile_image = view.findViewById(R.id.image_message_profile);
            img = view.findViewById(R.id.image);
            quote_message = view.findViewById(R.id.quote_message);
            quote_name = view.findViewById(R.id.quote_name);
            quotebox = view.findViewById(R.id.quotebox);
            quotebox_content = view.findViewById(R.id.quotebox_content);
            messagebox = view.findViewById(R.id.messagebox);
            messagebutton = view.findViewById(R.id.messagebutton);
            msg_exp = view.findViewById(R.id.text_message_body_expandable);

            gestureDetector = new GestureDetector(context, new SingleTapConfirm());

            storage = FirebaseStorage.getInstance();

            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            theme = Theme.getCurrentTheme(context);

            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION) {
                        return gestureDetector.onTouchEvent(event);
                    }
                    return false;
                }
            });

            try {
                msg.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            int action = event.getActionMasked();
                            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                GradientDrawable shape = new GradientDrawable();
                                shape.setShape(GradientDrawable.RECTANGLE);
                                float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
                                shape.setCornerRadii(corners);
                                if (action == MotionEvent.ACTION_DOWN) {
                                    if (messageList.get(pos).isSender()) {
                                        shape.setColor(ContextCompat.getColor(context, R.color.textbox_sent_clicked));
                                    } else {
                                        shape.setColor(ContextCompat.getColor(context, R.color.textbox_received_clicked));
                                    }
                                } else {
                                    if (messageList.get(pos).isSender()) {
                                        shape.setColor(ContextCompat.getColor(context, R.color.textbox_sent));
                                    } else {
                                        shape.setColor(ContextCompat.getColor(context, R.color.textbox_received));
                                    }
                                }

                                Message.Type type = messageList.get(pos).getType();
                                if (Message.isQuote(type)
                                        || Message.isDeletedQuote(type)
                                        || Message.isQuoteImage(type)) {
                                    quotebox.setBackground(shape);
                                } else if (Message.isForwardedMessage(type)
                                        || Message.isLinkPreview(type)
                                        || Message.isExpandable(type)
                                        || Message.isForwardedExpandable(type)) {
                                    messagebox.setBackground(shape);
                                } else if (Message.isBasicMessage(type)) {
                                    msg.setBackground(shape);
                                }
                            }

                            return gestureDetector.onTouchEvent(event);
                        }
                        return true;
                    }
                });
            } catch (NullPointerException e) {

            }

            try {
                msg_exp.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            int action = event.getActionMasked();
                            if (action == MotionEvent.ACTION_DOWN) {
                                GradientDrawable shape = new GradientDrawable();
                                shape.setShape(GradientDrawable.RECTANGLE);
                                float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
                                shape.setCornerRadii(corners);
                                if (messageList.get(pos).isSender()) {
                                    shape.setColor(ContextCompat.getColor(context, R.color.textbox_sent_clicked));
                                } else {
                                    shape.setColor(ContextCompat.getColor(context, R.color.textbox_received_clicked));
                                }
                                messagebox.setBackground(shape);
                            }
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                GradientDrawable shape = new GradientDrawable();
                                shape.setShape(GradientDrawable.RECTANGLE);
                                float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
                                shape.setCornerRadii(corners);
                                if (messageList.get(pos).isSender()) {
                                    shape.setColor(ContextCompat.getColor(context, R.color.textbox_sent));
                                } else {
                                    shape.setColor(ContextCompat.getColor(context, R.color.textbox_received));
                                }
                                messagebox.setBackground(shape);
                            }
                            return gestureDetector.onTouchEvent(event);
                        }
                        return true;
                    }
                });
            } catch (NullPointerException e) {

            }

            try {
                img.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            int action = event.getActionMasked();
                            if (action == MotionEvent.ACTION_DOWN) {
                                img.setForeground(context.getResources().getDrawable(R.drawable.image_overlay));
                            }
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                img.setForeground(null);
                            }
                            return gestureDetector.onTouchEvent(event);
                        }
                        return true;
                    }
                });
            } catch (NullPointerException e) {

            }
        }

        public void runSelectedMenuOption(Message.Type type, MenuItem item, Message message) {
            if (item.getItemId() == R.id.copy) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", message.getMsg());
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
                Intent intent = new Intent("pinnen");
                intent.putExtra("pinID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.jump) {
                Intent intent = new Intent("quotedMessage");
                intent.putExtra("quoteID", message.getQuote_key());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.quote) {
                Intent intent = new Intent("quote");
                intent.putExtra("quoteID", message.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.download && Message.isImage(type)) {
                Intent intent = new Intent("permission");
                intent.putExtra("imgurl", message.getMsg());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if (item.getItemId() == R.id.download && Message.isQuoteImage(type)) {
                Intent intent = new Intent("permission");
                intent.putExtra("imgurl", message.getQuote_message());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }

        public void openSheetMenu() {
            Message clickedDataItem = messageList.get(pos);
            String message = clickedDataItem.getMsg();
            Message.Type type = clickedDataItem.getType();

            if (type != Message.Type.HEADER) {
                SheetMenu sheetMenu = new SheetMenu();
                sheetMenu.setTitle(context.getResources().getString(R.string.selectanoption));
                sheetMenu.setClick(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        runSelectedMenuOption(type, item, clickedDataItem);
                        return false;
                    }
                });

                if (Message.isImage(type)) {
                    if (clickedDataItem.getPin().equals("0")) {
                        sheetMenu.setMenu(R.menu.menu_image);
                    } else {
                        sheetMenu.setMenu(R.menu.menu_image_unpin);
                    }
                } else if (Message.isQuoteImage(type)) {
                    if (clickedDataItem.getPin().equals("0")) {
                        sheetMenu.setMenu(R.menu.menu_quote_image);
                    } else {
                        sheetMenu.setMenu(R.menu.menu_quote_image_unpin);
                    }
                } else {
                    if (clickedDataItem.getPin().equals("0")) {
                        sheetMenu.setMenu(R.menu.menu_message);
                    } else {
                        sheetMenu.setMenu(R.menu.menu_message_unpin);
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
                    intent.putExtra("image", clickedDataItem.getMsg());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else if (Message.isQuoteImage(type)) {
                    Intent intent = new Intent("fullscreenimage");
                    intent.putExtra("image", clickedDataItem.getQuote_message());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } else if (Message.isQuote(type)) {
                    Intent intent = new Intent("quotedMessage");
                    intent.putExtra("quoteID", clickedDataItem.getQuote_key());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        int layout = context.getResources().obtainTypedArray(R.array.messageLayouts).getResourceId(viewType, -1);
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        holderList.add(holder);
        Message m = messageList.get(position);
        Message.Type type = m.getType();
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        float[] corners = new float[] { 27, 27, 27, 27, 27, 27, 27, 27 };
        shape.setCornerRadii(corners);
        if (!Message.isImage(type) && !Message.isLinkPreview(type)) {
            if (!m.getSearchString().isEmpty()) {
                SpannableStringBuilder sbuilder = highlightSearchedText(m.getMsg(), m.getSearchString());
                if (!Message.isExpandable(type) && !Message.isForwardedExpandable(type)) {
                    holder.msg.setText(sbuilder);
                } else {
                    holder.msg_exp.setText(sbuilder);
                    holder.msg_exp.setInterpolator(new LinearInterpolator());
                    holder.messagebutton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            holder.messagebutton.setText(holder.msg_exp.isExpanded() ? R.string.showmore : R.string.showless);
                            holder.msg_exp.toggle();
                        }
                    });
                }
            } else {
                if (type != Message.Type.HEADER && !Message.isExpandable(type) && !Message.isForwardedExpandable(type)) {
                    try {
                        SpannableStringBuilder newBuilder = styleText(m.getMsg());
                        holder.msg.setText(newBuilder);
                    } catch (IndexOutOfBoundsException ioobe) {
                        holder.msg.setText(m.getMsg());
                    }

                    if (!m.isSender()) {
                        new PatternEditableBuilder().
                                addPattern(Pattern.compile("\\@(\\w+)"), Color.BLUE,
                                        new PatternEditableBuilder.SpannableClickedListener() {
                                            @Override
                                            public void onSpanClicked(String text) {

                                            }
                                        }).into(holder.msg);
                    } else {
                        new PatternEditableBuilder().
                                addPattern(Pattern.compile("\\@(\\w+)"), Color.RED,
                                        new PatternEditableBuilder.SpannableClickedListener() {
                                            @Override
                                            public void onSpanClicked(String text) {

                                            }
                                        }).into(holder.msg);
                    }
                } else if (Message.isExpandable(type) || Message.isForwardedExpandable(type)) {
                    holder.msg_exp.setText(m.getMsg());
                    holder.msg_exp.setInterpolator(new LinearInterpolator());
                    holder.messagebutton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            holder.messagebutton.setText(holder.msg_exp.isExpanded() ? R.string.showmore : R.string.showless);
                            holder.msg_exp.toggle();
                        }
                    });
                } else {
                    holder.msg.setText(m.getMsg());
                }
            }
        } else if (Message.isImage(type)) {
            String imgurl = m.getMsg();
            StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
            StorageReference pathReference = storageRef.child("images/" + imgurl);

            if (fileOperations.readFromFile("mychatapp_settings_preview.txt").equals("off")) {
                GlideApp.with(context)
                        .load(pathReference)
                        .onlyRetrieveFromCache(true)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.img);
            } else {
                GlideApp.with(context)
                        .load(pathReference)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.img);
            }
        } else  {
            JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute(m.getMsg(), String.valueOf(position));
        }
        if (type != Message.Type.HEADER) {
            String time = m.getTime();
            if (!time.isEmpty()) {
                holder.time.setText(time.substring(9, 11) + ":" + time.substring(11, 13));
            } else {
                holder.time.setText("");
            }
            if (!m.isSender()) {
                if (!Message.isConMessage(type)) {
                    holder.name.setText(m.getUser().getName());

                    StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
                    final StorageReference pathReference_image = storageRef.child("profile_images/" + m.getUser().getImg());
                    try {
                        GlideApp.with(context)
                                .load(pathReference_image)
                                .centerCrop()
                                .into(holder.profile_image);
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
                String temp_userID = "";
                for (Message m2 : messageList) {
                    if (m2.getKey().equals(m.getQuote_key())) {
                        temp_userID = m2.getUser().getUserID();
                        break;
                    }
                }
                if (mAuth.getCurrentUser().getUid().equals(temp_userID)) {
                    holder.quote_name.setText(R.string.you);
                } else {
                    holder.quote_name.setText(m.getQuote_name());
                }
            }
            if (!Message.isQuoteImage(type)) {
                holder.quote_message.setText(m.getQuote_message());
            } else {
                String imgurl = m.getQuote_message();
                StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
                StorageReference pathReference = storageRef.child("images/" + imgurl);

                GlideApp.with(context)
                        //.using(new FirebaseImageLoader())
                        .load(pathReference)
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(holder.img);
            }
            GradientDrawable shape_quote = new GradientDrawable();
            shape_quote.setShape(GradientDrawable.RECTANGLE);
            shape_quote.setCornerRadii(corners);
            shape_quote.setColor(ContextCompat.getColor(context, R.color.textbox_time));
            /*if (!m.isSender()) {
                shape_quote.setColor(ContextCompat.getColor(context, R.color.textbox_received_quote));
            } else {
                shape_quote.setColor(ContextCompat.getColor(context, R.color.textbox_sent_quote));
            }*/
            holder.quotebox_content.setBackground(shape_quote);
            holder.quotebox.setBackground(shape);
        }

        if (type == Message.Type.HEADER || Message.isBasicMessage(type)) {
            holder.msg.setBackground(shape);
        } else if (Message.isForwardedMessage(type) || Message.isLinkPreview(type) || Message.isExpandable(type) || Message.isForwardedExpandable(type)) {
            holder.messagebox.setBackground(shape);
        }
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

    private static SpannableStringBuilder highlightSearchedText(String text, String textToBold) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        if(textToBold.length() > 0 && !textToBold.trim().equals("")) {
            int startingIndex = text.toLowerCase().indexOf(textToBold.toLowerCase());
            int endingIndex = startingIndex + textToBold.length();
            builder.append(text);
            builder.setSpan(new StyleSpan(Typeface.BOLD), startingIndex, endingIndex, 0);
            builder.setSpan(new ForegroundColorSpan(Color.RED), startingIndex, endingIndex, 0);
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
                holderList.get(position).msg.setText(htmlContentInStringFormat3);
            }
        }
    }
}