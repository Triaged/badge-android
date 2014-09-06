package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

/**
 * Profile activity for the logged-in-user.
 *
 * Created by Will on 7/10/14.
 */
public class MyProfileActivity extends AbstractProfileActivity implements ActionBar.TabListener {

    private ImageButton settingsButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_unselected).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_unselected).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_selected).setTabListener(this), true);

        settingsButton = (ImageButton) findViewById(R.id.settings_button);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

    }

    @Override
    protected void setContentViewLayout() {
        setContentView(R.layout.activity_my_profile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getActionBar();
        actionBar.getTabAt(0).setIcon(R.drawable.messages_unselected);
        actionBar.getTabAt(1).setIcon(R.drawable.contacts_unselected);
        actionBar.getTabAt(2).setIcon(R.drawable.profile_selected).select();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 0) {
            tab.setIcon(R.drawable.messages_selected);
            Intent intent = new Intent(this, MessagesIndexActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (tab.getPosition() == 1) {
            tab.setIcon(R.drawable.contacts_selected);
            Intent intent = new Intent(MyProfileActivity.this, ContactsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 2) {
            tab.setIcon(R.drawable.profile_unselected);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

}
