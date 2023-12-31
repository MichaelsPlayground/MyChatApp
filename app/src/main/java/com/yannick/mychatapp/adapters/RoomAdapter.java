package com.yannick.mychatapp.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yannick.mychatapp.Constants;
import com.yannick.mychatapp.FileOperations;
import com.yannick.mychatapp.GlideApp;
import com.yannick.mychatapp.StringOperations;
import com.yannick.mychatapp.data.Message;
import com.yannick.mychatapp.R;
import com.yannick.mychatapp.data.Room;
import com.yannick.mychatapp.data.Theme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class RoomAdapter extends ArrayAdapter<Room> {

    private final Context context;
    private final ArrayList<Room> roomList;
    private final RoomListType type;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
    private final FirebaseStorage storage;
    private final FirebaseAuth mAuth;

    static class ViewHolder {
        LinearLayout background;
        TextView roomNameText;
        TextView newestMessageText;
        TextView categoryText;
        TextView lockText;
        ImageView muteIcon;
        CircleImageView roomImage;
    }

    public RoomAdapter(Context context, ArrayList<Room> roomList, RoomListType type) {
        super(context, -1, roomList);
        this.context = context;
        this.roomList = roomList;
        this.type = type;
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public enum RoomListType {
        MY_ROOMS,
        FAVORITES,
        MORE
    }

    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;

        Room r = roomList.get(position);

        if (view == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_list, parent, false);

            viewHolder.roomNameText = view.findViewById(R.id.room_name);
            viewHolder.newestMessageText = view.findViewById(R.id.room_date);
            viewHolder.categoryText = view.findViewById(R.id.room_category);
            viewHolder.lockText = view.findViewById(R.id.room_lock);
            viewHolder.background = view.findViewById(R.id.room_background);
            viewHolder.muteIcon = view.findViewById(R.id.room_mute);
            viewHolder.roomImage = view.findViewById(R.id.room_image);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        if (!r.getSearchString().isEmpty()) {
            SpannableStringBuilder sbuilder = highlightSearchedText(r.getName(), r.getSearchString());
            viewHolder.roomNameText.setText(sbuilder);
        } else {
            viewHolder.roomNameText.setText(r.getName());
        }

        if (type == RoomListType.MORE) {
            viewHolder.categoryText.setText(context.getResources().getStringArray(R.array.categories)[r.getCategory()]);
        } else {
            if (r.getNewestMessage() != null) {
                if (r.getNewestMessage().getType() == Message.Type.MESSAGE_RECEIVED) {
                    if (r.getNewestMessage().isForwarded()) {
                        viewHolder.categoryText.setText(context.getResources().getString(R.string.forwarded) + ": " + r.getNewestMessage().getText());
                    } else if (r.getNewestMessage().getUser().getUserID().equals(mAuth.getCurrentUser().getUid())) {
                        viewHolder.categoryText.setText(context.getResources().getString(R.string.you) + ": " + r.getNewestMessage().getText());
                    } else {
                        viewHolder.categoryText.setText(r.getNewestMessage().getUser().getName() + ": " + r.getNewestMessage().getText());
                    }
                } else if (r.getNewestMessage().getType() == Message.Type.IMAGE_RECEIVED) {
                    if (r.getNewestMessage().getUser().getUserID().equals(mAuth.getCurrentUser().getUid())) {
                        viewHolder.categoryText.setText(context.getResources().getString(R.string.yousharedapicture));
                    } else {
                        viewHolder.categoryText.setText(r.getNewestMessage().getUser().getName() + " " + context.getResources().getString(R.string.sharedapicture));
                    }
                }
            } else {
                if (r.getAdmin().equals(mAuth.getCurrentUser().getUid())) {
                    viewHolder.categoryText.setText(R.string.youcreatedthisroom);
                } else {
                    viewHolder.categoryText.setText(r.getUsername() + " " + context.getResources().getString(R.string.createdthisroom));
                }
            }
        }
        if (type == RoomListType.MY_ROOMS || type == RoomListType.FAVORITES) {
            if (r.getNewestMessage() != null) {
                viewHolder.newestMessageText.setText(parseTime(r.getNewestMessage().getTime()));
            } else {
                viewHolder.newestMessageText.setText(parseTime(r.getTime()));
            }
        }
        if (type == RoomListType.FAVORITES) {
            viewHolder.lockText.setText("\u2764");
        } else if (type == RoomListType.MORE) {
            viewHolder.lockText.setText("\uD83D\uDD12");
        }

        FileOperations fileOperations = new FileOperations(this.context);
        if (type == RoomListType.MY_ROOMS || type == RoomListType.FAVORITES) {
            if (r.getNewestMessage() != null) {
                if (!r.getNewestMessage().getKey().equals(fileOperations.readFromFile(String.format(FileOperations.newestMessageFilePattern, r.getKey())))) {
                    if (Theme.getCurrentTheme(context) == Theme.DARK) {
                        viewHolder.background.setBackgroundColor(context.getResources().getColor(R.color.roomhighlight_dark));
                    } else {
                        viewHolder.background.setBackgroundColor(context.getResources().getColor(R.color.roomhighlight));
                    }
                } else {
                    viewHolder.background.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                }
            }

            if (r.isMuted()) {
                viewHolder.muteIcon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.icon_muted, context.getTheme()));
            } else {
                viewHolder.muteIcon.setImageDrawable(null);
            }
        }

        StorageReference storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        final StorageReference refImage = storageRef.child(Constants.roomImagesStorageKey + r.getImage());
        GlideApp.with(context)
                .load(refImage)
                .centerCrop()
                .thumbnail(0.05f)
                .into(viewHolder.roomImage);

        return view;
    }

    private String parseTime(String time) {
        try {
            time = sdf.format(sdf.parse(time));
        } catch (ParseException e) {
            Log.e("ParseException", e.toString());
        }
        if (time.substring(0, 8).equals(sdf.format(new Date()).substring(0, 8))) {
            return time.substring(9, 11) + ":" + time.substring(11, 13);
        } else {
            return StringOperations.convertDateToDisplayFormat(time);
        }
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
}