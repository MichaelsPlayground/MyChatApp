package com.yannick.mychatapp.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.yannick.mychatapp.data.Background;
import com.yannick.mychatapp.R;
import com.yannick.mychatapp.SquareImageView;
import com.yannick.mychatapp.data.Theme;

import java.util.ArrayList;

public class BackgroundAdapter extends BaseAdapter {
    private final Context context;
    private final TypedArray imageList;
    private Background selected;
    private final Theme theme;
    private final ArrayList<SquareImageView> viewList = new ArrayList<>();

    public BackgroundAdapter(Context context, TypedArray imageList, Background selected, Theme theme) {
        this.context = context;
        this.imageList = imageList;
        this.selected = selected;
        this.theme = theme;
    }

    public int getCount() {
        return this.imageList.length();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final SquareImageView imageView = new SquareImageView(this.context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        imageView.setBorderWidth((float) 6);
        imageView.setCornerRadius((float) 10);

        if (selected == Background.getByPosition(position)) {
            if (theme == Theme.DARK) {
                imageView.setBorderColor(context.getResources().getColor(R.color.dark_button));
            } else {
                imageView.setBorderColor(context.getResources().getColor(R.color.red));
            }
        } else {
            imageView.setBorderColor(context.getResources().getColor(R.color.grey));
        }

        imageView.setOnClickListener(view -> {
            selected = Background.getByPosition(position);
            for (SquareImageView v : viewList) {
                v.setBorderColor(context.getResources().getColor(R.color.grey));
            }
            if (theme == Theme.DARK) {
                imageView.setBorderColor(context.getResources().getColor(R.color.dark_button));
            } else {
                imageView.setBorderColor(context.getResources().getColor(R.color.red));
            }
        });

        if (position == 0) {
            if (theme == Theme.DARK) {
                imageView.setImageDrawable(new ColorDrawable(context.getResources().getColor(R.color.dark_background)));
            } else {
                imageView.setImageDrawable(new ColorDrawable(context.getResources().getColor(R.color.white)));
            }
        } else {
            int image = imageList.getResourceId(position, -1);
            imageView.setImageResource(image);
        }

        viewList.add(imageView);

        return imageView;
    }

    public Background getSelected() {
        return selected;
    }
}
