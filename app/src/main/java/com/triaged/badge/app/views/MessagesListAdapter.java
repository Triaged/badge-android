package com.triaged.badge.app.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.triaged.badge.app.ContactsActivity;
import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.ocpsoft.prettytime.PrettyTime;
import org.w3c.dom.Text;

import java.util.Date;

/**
 * Adapter for the list of threads.
 *
 * Created by Will on 7/16/14.
 */
public class MessagesListAdapter extends CursorAdapter {

    private SharedPreferences prefs;
    private LayoutInflater inflater;
    private String[] list;
    private PrettyTime prettyTime;
    private Date dateToFormat;
    private DataProviderService.LocalBinding dataProviderServiceBinding;
    private int unreadTitleColor;
    private int mainBlackColor;
    private int lightGrayColor;
    private Typeface medium;
    private Typeface regular;

    public MessagesListAdapter(DataProviderService.LocalBinding dataProviderServiceBinding, Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        inflater = LayoutInflater.from(context);
        prefs = PreferenceManager.getDefaultSharedPreferences( context );
        prettyTime = new PrettyTime();
        dateToFormat = new Date();
        mainBlackColor = context.getResources().getColor(R.color.main_text_black);
        lightGrayColor = context.getResources().getColor(R.color.light_text_gray);
        unreadTitleColor = context.getResources().getColor(R.color.main_orange);
        medium = Typeface.createFromAsset( context.getAssets(), "Roboto-Medium.ttf" );
        regular = Typeface.createFromAsset( context.getAssets(), "Roboto-Regular.ttf" );

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final ViewHolder holder = new ViewHolder();
        View v = inflater.inflate(R.layout.item_message, parent, false);
        holder.name = (TextView) v.findViewById(R.id.contact_name);
        holder.messagePreview = (TextView) v.findViewById(R.id.message_preview_text);
        holder.timestamp = (TextView) v.findViewById(R.id.timestamp);
        holder.profilePhoto = (ImageView)v.findViewById( R.id.contact_thumb );
        holder.missingProfilePhotoView = (ImageView)v.findViewById( R.id.no_photo_thumb );
        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder)view.getTag();
        holder.threadId = cursor.getString(cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_THREAD_ID ) );
        String names = cursor.getString(cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_THREAD_PARTICIPANTS ) );
        String avatarUrl = cursor.getString(cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_AVATAR_URL ) );
        String body = cursor.getString( cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_BODY ) );
        int isRead = cursor.getInt(cursor.getColumnIndex(CompanySQLiteHelper.COLUMN_MESSAGES_IS_READ));
        if (isRead == 1) {
            holder.name.setTextColor(mainBlackColor);
            holder.name.setTypeface(regular);
            holder.messagePreview.setTextColor(lightGrayColor);
        } else {
            holder.name.setTextColor(unreadTitleColor);
            holder.name.setTypeface(medium);
            holder.messagePreview.setTextColor(mainBlackColor);
        }
        holder.profilePhoto.setImageBitmap( null );
        holder.missingProfilePhotoView.setVisibility( View.VISIBLE );
        holder.name.setText( names );
        holder.messagePreview.setText(  body );
        String usersArrayString = prefs.getString(holder.threadId, "");
        try {
            JSONArray users = new JSONArray(usersArrayString);
            if (users.length() == 2) {
                dataProviderServiceBinding.setSmallContactImage( avatarUrl, holder.profilePhoto, holder.missingProfilePhotoView );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dateToFormat.setTime( cursor.getLong( cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_TIMESTAMP ))/ 1000l );
        holder.timestamp.setText( prettyTime.format(  dateToFormat) );
    }

    public class ViewHolder {
        public String threadId;
        public TextView name;
        public TextView messagePreview;
        public TextView timestamp;
        public ImageView profilePhoto;
        public ImageView missingProfilePhotoView;
    }

    /**
     * Call when no adapter no longer needed. Closes cursor.
     */
    public void destroy() {
        getCursor().close();
    }
}
