package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileManagesAdapter extends CursorAdapter {

    LruCache<Integer, Contact> contactCache;
    private LayoutInflater inflater;
    private float densityMultiplier = 1;
    private DataProviderService.LocalBinding dataProviderServiceBinding = null;

    public ProfileManagesAdapter(Context context, Cursor cursor, DataProviderService.LocalBinding dataProviderServiceBinding) {
        super( context, cursor, false );
        inflater = LayoutInflater.from(context);
        contactCache = new LruCache<Integer, Contact>( 100 );
        densityMultiplier = context.getResources().getDisplayMetrics().density;
        this.dataProviderServiceBinding = dataProviderServiceBinding;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(R.layout.item_manages_contact, parent, false);
        holder.profileManagesUserView = (ProfileManagesUserView) newView.findViewById(R.id.managed_user);
        newView.setTag(holder);
        Contact c = getCachedContact( cursor );

        holder.profileManagesUserView.primaryValue = c.name;
        holder.profileManagesUserView.secondaryValue = c.jobTitle;
        dataProviderServiceBinding.setSmallContactImage( c, holder.profileManagesUserView );
        //holder.profileManagesUserView.invalidate();

        return newView;

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Contact c = getCachedContact( cursor );
        holder.profileManagesUserView.primaryValue = c.name;
        holder.profileManagesUserView.secondaryValue = c.jobTitle;
        holder.profileManagesUserView.clearBitmap();
        dataProviderServiceBinding.setSmallContactImage( c, holder.profileManagesUserView );
        //holder.profileManagesUserView.invalidate();
    }

    class ViewHolder {
        ProfileManagesUserView profileManagesUserView;
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

}
