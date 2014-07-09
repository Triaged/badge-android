package com.triaged.badge.app.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.triaged.badge.app.R;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileManagesAdapter extends ArrayAdapter<Contact> {

    private List<Contact> contacts;
    private LayoutInflater inflater;

    public ProfileManagesAdapter(Context context, int resource, ArrayList<Contact> contacts) {
        super(context, resource, contacts);
        this.contacts = contacts;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Contact getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_manages_contact, parent, false);
            holder = new ViewHolder();
            holder.profileManagesUserView = (ProfileManagesUserView) convertView.findViewById(R.id.managed_user);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Contact c = contacts.get(position);
        holder.profileManagesUserView.primaryValue = c.name;
        holder.profileManagesUserView.secondaryValue = c.jobTitle;
        holder.profileManagesUserView.invalidate();

        return convertView;
    }

    class ViewHolder {
        ProfileManagesUserView profileManagesUserView;
    }
}
