package com.triaged.badge.app;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Inflate custom action bar view with back button.
 * NOTE: Activity requires the "BackButton" theme in AndroidManifest
 *
 * Created by Will on 7/18/14.
 */
public class BackButtonActivity extends BadgeActivity {

    protected TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflater = LayoutInflater.from(this);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View backButtonBar = inflater.inflate(R.layout.back_button_bar, null);
        backButton = (TextView) backButtonBar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);
    }
}
