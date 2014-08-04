package com.triaged.badge.app.views;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.MessageNewActivity;
import com.triaged.badge.app.MessageShowActivity;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

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

    public ContactsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding, Cursor cursor, int contactResourceId) {
        super( context, cursor, contactResourceId);
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
        final ViewHolder holder = new ViewHolder();
        View newView =  inflater.inflate(contactResourceId, parent, false);
        holder.nameTextView = (TextView) newView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) newView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) newView.findViewById(R.id.contact_thumb );
        holder.noPhotoThumb = (TextView) newView.findViewById(R.id.no_photo_thumb );
        holder.messageButton = (ImageButton) newView.findViewById(R.id.message_contact);
        if (holder.messageButton !=null) {
            holder.messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Integer[] recipientIds = new Integer[] {((Contact)holder.messageButton.getTag()).id, dataProviderServiceBinding.getLoggedInUser().id};
                    Arrays.sort(recipientIds);
                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            try {
                                return dataProviderServiceBinding.createThreadSync(recipientIds);
                            }
                            catch( JSONException e ) {
                                Toast.makeText(context, "Unexpected response from server.", Toast.LENGTH_SHORT).show();
                            }
                            catch( IOException e ) {
                                Toast.makeText( context, "Network issue occurred. Try again later.", Toast.LENGTH_SHORT ).show();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute( String threadId ) {
                            if( threadId != null ) {
                                Intent intent = new Intent(context, MessageShowActivity.class);
                                intent.putExtra(MessageShowActivity.THREAD_ID_EXTRA, threadId);
                                intent.setFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                                context.startActivity(intent);
                            }
                        }
                    }.execute();

                }
            });
        }

        newView.setTag(holder);
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Contact c = getCachedContact( cursor );
        holder.contact = c;
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

            dataProviderServiceBinding.setSmallContactImage(c, holder.thumbImage, holder.noPhotoThumb);
//            holder.noPhotoThumb.setVisibility(View.GONE);
        }
        if( holder.messageButton != null ) {
            holder.messageButton.setTag( c );
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

    @Override
    public void changeCursor(Cursor cursor) {
        contactCache.evictAll();
        super.changeCursor(cursor);
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
        Contact contact;
    }

}
