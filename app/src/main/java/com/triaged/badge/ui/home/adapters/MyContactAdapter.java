package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.ContactsTable;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */

public class MyContactAdapter extends CursorAdapter implements StickyListHeadersAdapter {


    private float densityMultiplier = 1;
    private LayoutInflater inflater;

    public MyContactAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
        inflater = LayoutInflater.from(context);
        densityMultiplier = context.getResources().getDisplayMetrics().density;
    }


    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        View row = inflater.inflate(R.layout.item_contact_with_msg, parent, false);
        final ViewHolder holder = new ViewHolder(row);
        row.setTag(holder);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String firstName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_FIRST_NAME));
        String lastName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_LAST_NAME));
        String jobTitle = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_JOB_TITLE));
        String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_AVATAR_URL));

        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.nameTextView.setText(firstName + " " + lastName);
        holder.titleTextView.setText(jobTitle);
        if (jobTitle == null || jobTitle.equals("")) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (19 * densityMultiplier), 0, 0);
            holder.nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier), 0, 0);
            holder.nameTextView.setLayoutParams(layoutParams);
        }
        holder.thumbImage.setImageBitmap(null);
//        holder.noPhotoThumb.setText(c.initials);
        holder.noPhotoThumb.setVisibility(View.VISIBLE);
        if (avatarUrl != null) {

            ImageLoader.getInstance().displayImage(avatarUrl, holder.thumbImage, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    holder.noPhotoThumb.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    holder.noPhotoThumb.setVisibility(View.GONE);
                }
            });
        }

    }

    @Override
    public View getHeaderView(int position, View headerView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (headerView == null) {
            headerView = inflater.inflate(R.layout.item_contact_header, parent, false);
            holder = new HeaderViewHolder(headerView);
            headerView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) headerView.getTag();
        }

        Cursor cursor = (Cursor) getItem(position);
        String lastName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_LAST_NAME));
        String headerText = "" + lastName.subSequence(0, 1).charAt(0);
        holder.textView.setText(headerText);

        return headerView;
    }

    @Override
    public long getHeaderId(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_LAST_NAME)).charAt(0);
    }

    class HeaderViewHolder {
        @InjectView(R.id.section_heading) TextView textView;
        HeaderViewHolder(View header) {
            ButterKnife.inject(this, header);
        }
    }

    class ViewHolder {
        @InjectView(R.id.contact_name) TextView nameTextView;
        @InjectView(R.id.contact_title) TextView titleTextView;
        @InjectView(R.id.contact_thumb) ImageView thumbImage;
        @InjectView(R.id.message_contact) ImageButton messageButton;
        @InjectView(R.id.no_photo_thumb) TextView noPhotoThumb;

        ViewHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }

}
