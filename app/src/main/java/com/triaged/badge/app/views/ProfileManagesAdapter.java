package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
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
public class ProfileManagesAdapter extends CursorAdapter {

    LruCache<Integer, Contact> contactCache;
    private LayoutInflater inflater;
    private DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private float densityMultiplier = 1;

    public ProfileManagesAdapter(Context context, Cursor cursor, DataProviderService.LocalBinding dataProviderServiceBinding) {
        super( context, cursor, false );
        inflater = LayoutInflater.from(context);
        contactCache = new LruCache<Integer, Contact>( 100 );
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        densityMultiplier = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(R.layout.item_manages_contact, parent, false);
        holder.nameTextView = (TextView) newView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) newView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) newView.findViewById(R.id.contact_thumb );
        holder.noPhotoThumb = (TextView) newView.findViewById(R.id.no_photo_thumb );
        newView.setTag(holder);
        Contact c = getCachedContact( cursor );
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
        if( c.avatarUrl != null ) {
            dataProviderServiceBinding.setSmallContactImage(c, holder.thumbImage);
        } else {
            holder.noPhotoThumb.setText(c.initials);
            holder.noPhotoThumb.setVisibility(View.VISIBLE);
        }
        return newView;


    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Contact c = getCachedContact( cursor );
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
        if( c.avatarUrl != null ) {
            dataProviderServiceBinding.setSmallContactImage(c, holder.thumbImage);
            holder.noPhotoThumb.setVisibility(View.GONE);
        } else {
            holder.noPhotoThumb.setText(c.initials);
            holder.noPhotoThumb.setVisibility(View.VISIBLE);
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
        return getCachedContact( (Cursor)getItem( position ) );
    }

    public void destroy() {
        getCursor().close();
    }

}
