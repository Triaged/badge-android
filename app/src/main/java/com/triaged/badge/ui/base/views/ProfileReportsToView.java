package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.models.Contact;

/**
 * Created by Will on 7/21/14.
 */
public class ProfileReportsToView extends RelativeLayout {

    private float densityMultiplier;
    public ImageView thumbImage;
    public TextView noPhotoThumb;
    public int profileId = -1;
    public int userId = -1;

    public ProfileReportsToView(Context context, AttributeSet attrs) {
        super(context, attrs);
        densityMultiplier = context.getResources().getDisplayMetrics().density;
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
            layoutParams.setMargins(0, (int) (19 * densityMultiplier), 0, 0);
            nameTextView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) nameTextView.getLayoutParams();
            layoutParams.setMargins(0, (int) (10 * densityMultiplier), 0, 0);
            nameTextView.setLayoutParams(layoutParams);
        }
        thumbImage.setImageBitmap(null);
    }
}
