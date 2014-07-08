package com.triaged.badge.app.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Common Sticky List Header for Contacts ListViews
 *
 * Created by Will on 7/7/14.
 */
public class ContactsAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    public List<Contact> contacts = null;
    private LayoutInflater inflater;

    public ContactsAdapter(Context context, List<Contact> contacts) {
        inflater = LayoutInflater.from(context);
        this.contacts = contacts;
    }

    @Override
    public View getHeaderView(int i, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.item_contact_header, parent, false);
            holder.textView = (TextView) convertView.findViewById(R.id.section_heading);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        String headerText = "" + contacts.get(i).lastName.subSequence(0,1).charAt(0);
        holder.textView.setText(headerText);

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_contact, parent, false);
            holder.textView = (TextView) convertView.findViewById(R.id.contact_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Contact c = contacts.get(position);
        holder.textView.setText(c.name);

        return convertView;
    }

    @Override
    public long getHeaderId(int i) {
        return contacts.get(i).lastName.subSequence(0,1).charAt(0);
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Object getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class HeaderViewHolder {
        TextView textView;
    }

    class ViewHolder {
        TextView textView;
    }

}
