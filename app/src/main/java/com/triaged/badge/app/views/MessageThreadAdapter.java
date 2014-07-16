package com.triaged.badge.app.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.triaged.badge.app.R;

import java.util.ArrayList;

/**
 * Created by Will on 7/16/14.
 */
public class MessageThreadAdapter extends BaseAdapter {

    LayoutInflater inflater;
    private String[] list;

    public MessageThreadAdapter(Context context, String[] list) {
        super();
        inflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.length;
    }

    @Override
    public String getItem(int position) {
        return list[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessageHolder holder;
        int resourceId;
        String message;

        if (getItemViewType(position) == 0) {
            resourceId = R.layout.item_my_message;
            message = "MY MESSAGE: ";
        } else {
            resourceId = R.layout.item_other_message;
            message = "OTHER MESSAGE: ";
        }
        if (convertView == null) {
            convertView = inflater.inflate(resourceId, parent, false);
            holder = new MessageHolder();
            holder.message = (TextView) convertView.findViewById(R.id.message_text);
            holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
            convertView.setTag(holder);
        } else {
            holder = (MessageHolder) convertView.getTag();
        }
        message += list[position];
        holder.message.setText(message);
        holder.timestamp.setText("2:45 PM");

        return convertView;
    }

    /** This adapter uses 2 different types of views (my message and other message) */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /** Determine which view to use */
    @Override
    public int getItemViewType(int position) {
        if (isMyMessage(position)) {
            return 0;
        } else {
            return 1;
        }
    }

    /** Lookup message to determine which view to use */
    public boolean isMyMessage(int position) {
        // TODO: REPLACE PLACEHOLDER
        return position%2 == 0;
    }

    class MessageHolder {
        TextView message;
        TextView timestamp;
        ImageButton userPhoto;
    }



}
