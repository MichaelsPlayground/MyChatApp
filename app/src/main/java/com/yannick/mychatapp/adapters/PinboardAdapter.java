package com.yannick.mychatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yannick.mychatapp.Constants;
import com.yannick.mychatapp.GlideApp;
import com.yannick.mychatapp.StringOperations;
import com.yannick.mychatapp.data.Message;
import com.yannick.mychatapp.R;

import java.util.ArrayList;

public class PinboardAdapter extends ArrayAdapter<Message> {

    private final Context context;
    private final ArrayList<Message> pinnedList;

    static class ViewHolder {
        TextView userText;
        TextView timeText;
        TextView messageText;
        ImageView image;
    }

    public PinboardAdapter(Context context, ArrayList<Message> pinnedList) {
        super(context, -1, pinnedList);
        this.context = context;
        this.pinnedList = pinnedList;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        View rowView;
        Message.Type type = pinnedList.get(position).getType();

        if (Message.isImage(type)) {
            rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.pinned_image, parent, false);
        } else {
            rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.pinned_message, parent, false);
        }

        ViewHolder viewHolder = new ViewHolder();

        viewHolder.userText = rowView.findViewById(R.id.user);
        viewHolder.timeText = rowView.findViewById(R.id.time);

        if (Message.isImage(type)) {
            viewHolder.image = rowView.findViewById(R.id.image);
        } else {
            viewHolder.messageText = rowView.findViewById(R.id.message);
        }

        String time = pinnedList.get(position).getTime();
        String parsedTime = StringOperations.convertDateToDisplayFormat(time) + " " + time.substring(9, 11) + ":" + time.substring(11, 13);

        viewHolder.userText.setText(pinnedList.get(position).getUser().getName());
        viewHolder.timeText.setText(parsedTime);
        if (Message.isImage(type)) {
            String imageURL = pinnedList.get(position).getText();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference pathReference = storageRef.child(Constants.imagesStorageKey + imageURL);

            GlideApp.with(context)
                    .load(pathReference)
                    .centerCrop()
                    .thumbnail(0.05f)
                    .into(viewHolder.image);
        } else {
            viewHolder.messageText.setText(pinnedList.get(position).getText());
        }

        rowView.setOnClickListener(layoutView -> {
            Intent intent = new Intent("jumppinned");
            intent.putExtra("pinnedKey", pinnedList.get(position).getKey());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        });

        return rowView;
    }
}