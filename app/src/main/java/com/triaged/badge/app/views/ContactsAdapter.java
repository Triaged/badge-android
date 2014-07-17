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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.BadgeApplication;
import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import org.w3c.dom.Text;

import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Common Sticky List Header for Contacts ListViews
 *
 * Created by Will on 7/7/14.
 */
public class ContactsAdapter extends CursorAdapter implements StickyListHeadersAdapter {

    protected static final LruCache<Integer, Contact> contactCache = new LruCache<Integer, Contact>( 100 );
    private LayoutInflater inflater;
    private float densityMultiplier = 1;
    private DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private int contactResourceId;

    public ContactsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding, int contactResourceId) {
        this(context, dataProviderServiceBinding, contactResourceId, true);
    }

    public ContactsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding, int contactResourceId, boolean includeMe) {
        super( context, includeMe ? dataProviderServiceBinding.getContactsCursor() : dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser(), false);
        inflater = LayoutInflater.from(context);
        densityMultiplier = context.getResources().getDisplayMetrics().density;
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.contactResourceId= contactResourceId;
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
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(contactResourceId, parent, false);
        holder.nameTextView = (TextView) newView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) newView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) newView.findViewById(R.id.contact_thumb );
        holder.noPhotoThumb = (TextView) newView.findViewById(R.id.no_photo_thumb );
        newView.setTag(holder);
        Contact c = getCachedContact( cursor );
        holder.nameTextView.setText(c.name);
        holder.titleTextView.setText(c.jobTitle);
        holder.messageButton = (ImageButton) newView.findViewById(R.id.message_contact);
        if (holder.messageButton !=null) {
            holder.messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "HELLO", Toast.LENGTH_SHORT).show();
                }
            });
        }
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

    @Override
    public long getHeaderId(int  position ) {
        return getCachedContact( position ).lastName.subSequence(0,1).charAt(0);
    }

    public Contact getCachedContact( int position ) {
        return getCachedContact( (Cursor)getItem( position ) );
    }

    public static Contact getCachedContact( Cursor cursor ) {
        int id = Contact.getIntSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_CONTACT_ID );
        Contact c = contactCache.get( id );
        if( c == null ) {
            c = new Contact();
            c.fromCursor( cursor );
            contactCache.put( c.id, c  );
        }
        return c;
    }

    /**
     * Notifies the adapter that new data is available so it should
     * re-query and refresh the data shown in the list.
     */
    public void refresh() {
        contactCache.evictAll();
        changeCursor( dataProviderServiceBinding.getContactsCursor() );
        notifyDataSetChanged();
    }

    /**
     * Call when adapter is going away for good (listview or containing activity being destroyed)
     */
    public void destroy() {
        getCursor().close();
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
        ImageButton messageButton;
        TextView noPhotoThumb;
    }

}
