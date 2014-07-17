package com.triaged.badge.app.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.triaged.badge.app.R;

import org.w3c.dom.Text;

/**
 * TODO: IMPLEMENT ADAPTER
 *
 * Created by Will on 7/16/14.
 */
public class MessagesListAdapter extends BaseAdapter {

    LayoutInflater inflater;
    private String[] list;

    public MessagesListAdapter(Context context, String[] list) {
        super();
        inflater = LayoutInflater.from(context);
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.length;
    }

    @Override
    public Object getItem(int position) {
        return list[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_message, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.contact_name);
            holder.messagePreview = (TextView) convertView.findViewById(R.id.message_preview_text);
            holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.name.setText("NAME");
        holder.messagePreview.setText("MESSAGE BODY : " + getItem(position));
        holder.timestamp.setText("15 min");

        return convertView;
    }

    class ViewHolder {
        TextView name;
        TextView messagePreview;
        TextView timestamp;
        ImageButton profilePhoto;
        TextView missingProfilePhotoView;
    }
}
