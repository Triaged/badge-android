package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.ContactsTable;

/**
 * Created by Will on 7/9/14.
 * Revised by Sadegh on 9/26/14.
 */
public class ContactsWithoutHeadingsAdapter extends CursorAdapter {

    private LayoutInflater inflater;
    private float densityMultiplier = 1;

    public ContactsWithoutHeadingsAdapter(Context context, Cursor cursor) {
        super(context, cursor, true);
        inflater = LayoutInflater.from(context);
        densityMultiplier = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        View rowView = inflater.inflate(R.layout.item_contact_without_heading, parent, false);
        holder.nameTextView = (TextView) rowView.findViewById(R.id.contact_name);
        holder.titleTextView = (TextView) rowView.findViewById(R.id.contact_title);
        holder.thumbImage = (ImageView) rowView.findViewById(R.id.contact_thumb);
        holder.noPhotoThumb = (TextView) rowView.findViewById(R.id.no_photo_thumb);
        rowView.setTag(holder);
        return rowView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        String firstName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.CLM_FIRST_NAME));
        String lastName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.CLM_LAST_NAME));
        holder.name = firstName + " " + lastName;
        holder.id = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_ID)) ;

        holder.nameTextView.setText(holder.name);
        String jobTitle = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.CLM_JOB_TITLE));
        holder.titleTextView.setText(jobTitle);
        if (TextUtils.isEmpty(jobTitle)) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (19 * densityMultiplier), 0, 0);
            holder.nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier), 0, 0);
            holder.nameTextView.setLayoutParams(layoutParams);
        }
        holder.thumbImage.setImageBitmap(null);
        holder.noPhotoThumb.setText(String.valueOf(firstName.charAt(0) + lastName.charAt(0)).toUpperCase());
        holder.noPhotoThumb.setVisibility(View.VISIBLE);
        String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.CLM_AVATAR_URL));
        if (avatarUrl != null) {
//            dataProviderServiceBinding.setSmallContactImage(c, holder.thumbImage, holder.noPhotoThumb );
            ImageLoader.getInstance().displayImage(avatarUrl, holder.thumbImage, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    holder.noPhotoThumb.setVisibility(View.GONE);
                }
            });
        }
    }

    public class ViewHolder {
        public int id;
        public String name;
        TextView nameTextView;
        TextView titleTextView;
        ImageView thumbImage;
        TextView noPhotoThumb;
    }

}
