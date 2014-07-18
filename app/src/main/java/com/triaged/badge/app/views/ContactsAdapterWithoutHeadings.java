package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Will on 7/9/14.
 */
public class ContactsAdapterWithoutHeadings extends CursorAdapter {

    LruCache<Integer, Contact> contactCache;
    protected List<Contact> filteredList = new ArrayList<Contact>(30);
    private boolean filtering = false;
    private Context context;
    private LayoutInflater inflater;
    private DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private float densityMultiplier = 1;



    public ContactsAdapterWithoutHeadings(Context context, Cursor cursor, DataProviderService.LocalBinding dataProviderServiceBinding) {
        super( context, cursor, false );
        inflater = LayoutInflater.from(context);
        contactCache = new LruCache<Integer, Contact>( 100 );
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        densityMultiplier = context.getResources().getDisplayMetrics().density;
        this.context = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return newView(context, getCachedContact(cursor), parent);
    }

    public View newView( Context context, Contact c, ViewGroup parent ) {

        ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(R.layout.item_manages_contact, parent, false);
        holder.nameTextView = (TextView) newView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) newView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) newView.findViewById(R.id.contact_thumb );
        holder.noPhotoThumb = (TextView) newView.findViewById(R.id.no_photo_thumb );
        newView.setTag(holder);
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindView( view, context, getCachedContact(cursor) );
    }

    public void bindView(View view, Context context, Contact c) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.nameTextView.setText(c.name);
        holder.titleTextView.setText(c.jobTitle);
        if (c.jobTitle == null || c.jobTitle.equals("")) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (19 * densityMultiplier),0,0);
            holder.nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier),0,0);
            holder.nameTextView.setLayoutParams(layoutParams);
        }
        holder.thumbImage.setImageBitmap( null );
        holder.noPhotoThumb.setText(c.initials);
        holder.noPhotoThumb.setVisibility(View.VISIBLE);
        if( c.avatarUrl != null ) {
            dataProviderServiceBinding.setSmallContactImage(c, holder.thumbImage, holder.noPhotoThumb );
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if( filtering ) {
            Contact c = filteredList.get( position );
            View v;
            if( convertView == null ) {
                v =  newView( context, c, parent );
            }
            else {
                v = convertView;
            }
            bindView( convertView, context, c );
            return convertView;
        }
        else {
            return super.getView(position, convertView, parent);
        }
    }

    class ViewHolder {
        TextView nameTextView;
        TextView titleTextView;
        ImageView thumbImage;
        TextView noPhotoThumb;
    }

    public Contact getCachedContact( Cursor cursor ) {
        int id = Contact.getIntSafelyFromCursor(cursor, CompanySQLiteHelper.COLUMN_CONTACT_ID);
        Contact c = contactCache.get(id);
        if( c == null ) {
            c = new Contact();
            c.fromCursor( cursor );
            contactCache.put( c.id, c  );
        }
        return c;
    }

    public Contact getCachedContact( int position ) {
        if( filtering ) {
         return (Contact)getItem( position );
        }
        else {
            return getCachedContact((Cursor) getItem(position));
        }
    }

    public void clearFilter() {
        filtering = false;
        filteredList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if( filtering ) {
            return filteredList.size();
        }
        else {
            return super.getCount();
        }
    }

    @Override
    public Object getItem(int position) {
        if( filtering ) {
            return filteredList.get( position );
        }
        else {
            return super.getItem(position);
        }
    }

    public void setFilter( String partialName ) {
        filtering = true;
        Cursor c = getCursor();
        partialName = partialName.toLowerCase();
        filteredList.clear();

        c.moveToFirst();
        do {
            String firstName = Contact.getStringSafelyFromCursor( c, CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME ).toLowerCase();
            String lastName = Contact.getStringSafelyFromCursor( c, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME ).toLowerCase();
            if( firstName.indexOf( partialName ) > -1 || lastName.indexOf( partialName ) > -1 ) {
                filteredList.add( getCachedContact( c ) );
            }
        } while( c.moveToNext() );
        notifyDataSetChanged();
    }

    public void refresh( Cursor cursor ) {
        contactCache.evictAll();
        changeCursor( cursor );
        notifyDataSetChanged();
    }

    public void destroy() {
        getCursor().close();
    }

}
