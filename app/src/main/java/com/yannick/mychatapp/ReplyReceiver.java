package com.yannick.mychatapp;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReplyReceiver extends BroadcastReceiver {

    private CharSequence getReplyMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("key_text_reply");
        }
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        CharSequence message = getReplyMessage(intent);

        DatabaseReference root = FirebaseDatabase.getInstance().getReference().getRoot().child("rooms").child(intent.getStringExtra("room_key"));

        Map<String, Object> map = new HashMap<String, Object>();
        String temp_key = root.push().getKey();
        root.updateChildren(map);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
        String currentDateandTime = sdf.format(new Date());

        DatabaseReference message_root = root.child(temp_key);
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("name", intent.getStringExtra("user_id"));
        map2.put("msg", message.toString());
        map2.put("img", "");
        map2.put("pin", "0");
        map2.put("quote", "");
        map2.put("time", currentDateandTime);

        message_root.updateChildren(map2);

        updateNotification(context, intent.getIntExtra("push_id", 1));

        writeToFile(temp_key, "mychatapp_raum_" + intent.getStringExtra("room_key") + "_nm.txt", context);
    }

    private void updateNotification(Context context, int notifyId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel_id")
                .setSmallIcon(R.drawable.ic_stat_ic_stat_onesignal_default)
                .setContentText(context.getResources().getString(R.string.messagesent));

        notificationManager.notify(notifyId, builder.build());
    }

    public void writeToFile(String text, String datei, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(datei, Context.MODE_PRIVATE));
            outputStreamWriter.write(text);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
