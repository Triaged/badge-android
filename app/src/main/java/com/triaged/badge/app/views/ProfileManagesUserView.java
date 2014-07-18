package com.triaged.badge.app.views;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.triaged.badge.app.AbstractProfileActivity;
import com.triaged.badge.app.MyProfileActivity;
import com.triaged.badge.app.OtherProfileActivity;
import com.triaged.badge.app.R;
import com.triaged.badge.data.Contact;

/**
 * A view that holds a contact managed by another user
 *
 * Created by Will on 7/9/14.
 */
public class ProfileManagesUserView extends RelativeLayout
        implements View.OnClickListener
{

    private float densityMultiplier;
    public ImageView thumbImage;
    public TextView noPhotoThumb;
    public int profileId = -1;
    public int userId = -1;
    private Context context;

    public ProfileManagesUserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        densityMultiplier = context.getResources().getDisplayMetrics().density;
        this.context = context;
        setOnClickListener(this);
    }

    public void setupView(Contact c) {
        TextView nameTextView = (TextView) findViewById(R.id.contact_name);
        TextView titleTextView = (TextView) findViewById(R.id.contact_title);
        thumbImage = (ImageView) findViewById(R.id.contact_thumb);
        noPhotoThumb = (TextView) findViewById(R.id.no_photo_thumb);
        profileId = c.id;
        nameTextView.setText(c.name);
        titleTextView.setText(c.jobTitle);
        if (c.jobTitle == null || c.jobTitle.equals("")) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (19 * densityMultiplier),0,0);
            nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier),0,0);
            nameTextView.setLayoutParams(layoutParams);
        }
        thumbImage.setImageBitmap( null );
    }

    @Override
    public void onClick(View v) {
        Log.d(ProfileManagesUserView.class.getName(), "Hello World");
        Intent intent;
        if (userId == profileId) {
            intent = new Intent(context, MyProfileActivity.class);
        } else {
            intent = new Intent(context, OtherProfileActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("PROFILE_ID", profileId);
        context.startActivity(intent);
    }
}
