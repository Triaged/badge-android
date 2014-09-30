package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.BThreadsTable;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class HistoryAdapter extends CursorAdapter {

    private LayoutInflater inflater;
    private PrettyTime prettyTime;
    private Date dateToFormat;
    private int unreadTitleColor;
    private int mainBlackColor;
    private int lightGrayColor;
    private Typeface medium;
    private Typeface regular;

    public HistoryAdapter(Context context, Cursor c) {
        super(context, c, false);
        inflater = LayoutInflater.from(context);
        prettyTime = new PrettyTime();
        dateToFormat = new Date();
        mainBlackColor = context.getResources().getColor(R.color.main_text_black);
        lightGrayColor = context.getResources().getColor(R.color.light_text_gray);
        unreadTitleColor = context.getResources().getColor(R.color.main_orange);
        medium = Typeface.createFromAsset(context.getAssets(), "Roboto-Medium.ttf");
        regular = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View row = inflater.inflate(R.layout.item_message, parent, false);
        final ViewHolder holder = new ViewHolder(row);
        row.setTag(holder);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.threadId = cursor.getString(cursor.getColumnIndex(MessagesTable.CLM_THREAD_ID));
        String names = cursor.getString(cursor.getColumnIndex(BThreadsTable.CLM_NAME));
        if (names == null) {
            String firstName = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_LAST_NAME));
            if (firstName != null && lastName != null) {
                names = String.format("%s %s", firstName, lastName);
            } else {
                names = "Badge";
            }
        }

        String avatarUrl = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_AVATAR_URL));
        String body = cursor.getString(cursor.getColumnIndex(MessagesTable.CLM_BODY));
        int isRead = cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_IS_READ));

        if (isRead == 1) {
            holder.name.setTextColor(mainBlackColor);
            holder.name.setTypeface(regular);
            holder.messagePreview.setTextColor(lightGrayColor);
        } else {
            holder.name.setTextColor(unreadTitleColor);
            holder.name.setTypeface(medium);
            holder.messagePreview.setTextColor(mainBlackColor);
        }

        holder.profilePhoto.setImageBitmap(null);
        holder.missingProfilePhotoView.setVisibility(View.VISIBLE);
        holder.name.setText(names);
        holder.messagePreview.setText(body);
        //TODO: It's very bad idea to try to access shared preferences,
        // In bindView, Doing IO is expensive and make list laggy.
        String usersArrayString = SharedPreferencesUtil.getString(holder.threadId, "");
        try {
            JSONArray users = new JSONArray(usersArrayString);
            if (users.length() == 2) {
                ImageLoader.getInstance().displayImage(avatarUrl, holder.profilePhoto, new SimpleImageLoadingListener() {

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        holder.missingProfilePhotoView.setVisibility(View.GONE);
                    }
                });
            } else { // It's a group message
                //TODO: It's bad idea, to access share preferences,
                // in bindVeiw, but since we don't have proper database design,
                // it's okay for now.
                String groupName = SharedPreferencesUtil.getString("name_" + holder.threadId, null);
                if (!TextUtils.isEmpty(groupName)) {
                    holder.name.setText(groupName);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        long serverTime = cursor.getLong(cursor.getColumnIndex(MessagesTable.CLM_TIMESTAMP)) / 1000l;
        if (serverTime > System.currentTimeMillis()) {
            serverTime = System.currentTimeMillis() - 5000;
        }
        dateToFormat.setTime(serverTime);
        holder.timestamp.setText(prettyTime.format(dateToFormat));
    }

    public class ViewHolder {
        public String threadId;
        @InjectView(R.id.contact_name) public TextView name;
        @InjectView(R.id.message_preview_text) public TextView messagePreview;
        @InjectView(R.id.timestamp) public TextView timestamp;
        @InjectView(R.id.contact_thumb) public ImageView profilePhoto;
        @InjectView(R.id.no_photo_thumb) public ImageView missingProfilePhotoView;

        ViewHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }

}
