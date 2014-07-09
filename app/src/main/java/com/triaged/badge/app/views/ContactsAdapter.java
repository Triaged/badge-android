package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.triaged.badge.app.BadgeApplication;
import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.Contact;

import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Common Sticky List Header for Contacts ListViews
 *
 * Created by Will on 7/7/14.
 */
public class ContactsAdapter extends CursorAdapter implements StickyListHeadersAdapter {

    LruCache<Integer, Contact> contactCache;
    private LayoutInflater inflater;
    private float densityMultiplier = 1;
    private DataProviderService.LocalBinding dataProviderServiceBinding = null;

    public ContactsAdapter(Context context, Cursor cursor, DataProviderService.LocalBinding dataProviderServiceBinding) {
        super( context, cursor, false );
        inflater = LayoutInflater.from(context);
        contactCache = new LruCache<Integer, Contact>( 100 );
        densityMultiplier = context.getResources().getDisplayMetrics().density;
        this.dataProviderServiceBinding = dataProviderServiceBinding;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.item_contact_header, parent, false);
            holder.textView = (TextView) convertView.findViewById(R.id.section_heading);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        Contact c = getCachedContact( position );
        String headerText = "" + c.lastName.subSequence(0,1).charAt(0);
        holder.textView.setText(headerText);

        return convertView;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(R.layout.item_contact, parent, false);
        holder.nameTextView = (TextView) newView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) newView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) newView.findViewById(R.id.contact_thumb );
        newView.setTag(holder);
        Contact c = getCachedContact( cursor );
        holder.nameTextView.setText(c.name);
        holder.titleTextView.setText(c.jobTitle);
        if (c.jobTitle == null) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0,0,0,0);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            holder.nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier),0,0);
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT);
            holder.nameTextView.setLayoutParams(layoutParams);
        }
        dataProviderServiceBinding.setSmallContactImage( c, holder.thumbImage );
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Contact c = getCachedContact( cursor );
        holder.nameTextView.setText(c.name);
        holder.titleTextView.setText(c.jobTitle);
        if (c.jobTitle == null) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0,0,0,0);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            holder.nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier),0,0);
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT);
            holder.nameTextView.setLayoutParams(layoutParams);
        }
        dataProviderServiceBinding.setSmallContactImage( c, holder.thumbImage );
    }

    @Override
    public long getHeaderId(int  position ) {
        return getCachedContact( position ).lastName.subSequence(0,1).charAt(0);
    }

    public Contact getCachedContact( int position ) {
        return getCachedContact( (Cursor)getItem( position ) );
    }

    public Contact getCachedContact( Cursor cursor ) {
        int id = cursor.getInt( cursor.getColumnIndex(DataProviderService.COLUMN_CONTACT_ID ) );
        Contact c = contactCache.get( id );
        if( c == null ) {
            c = new Contact();
            c.fromCursor( cursor );
            contactCache.put( c.id, c  );
        }
        return c;
    }

//    @Override
//    public int getCount() {
//        return contacts.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return contacts.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }

    class HeaderViewHolder {
        TextView textView;
    }

    class ViewHolder {
        TextView nameTextView;
        TextView titleTextView;
        ImageView thumbImage;
    }

}
