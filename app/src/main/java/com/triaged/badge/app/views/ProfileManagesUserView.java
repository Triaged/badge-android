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
public class ProfileManagesUserView extends ProfileReportsToView {

    public ProfileManagesUserView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
