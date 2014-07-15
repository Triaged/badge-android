package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * Created by Will on 7/15/14.
 */
public class MessagesIndexActivity extends BadgeActivity implements ActionBar.TabListener {

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_unselected).setTabListener(this), true);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_selected).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_unselected).setTabListener(this), false);

        setContentView(R.layout.activity_messages_index);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
        ActionBar actionBar = getActionBar();
        actionBar.getTabAt(0).setIcon(R.drawable.messages_selected).select();
        actionBar.getTabAt(1).setIcon(R.drawable.contacts_unselected);
        actionBar.getTabAt(2).setIcon(R.drawable.profile_unselected);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 1) {
            tab.setIcon(R.drawable.contacts_selected);
            Intent intent = new Intent(this, ContactsActivity.class);
            startActivity(intent);
        } else if (tab.getPosition() == 2) {
            tab.setIcon(R.drawable.profile_selected);
            Intent intent = new Intent( this, MyProfileActivity.class );
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("PROFILE_ID", dataProviderServiceBinding.getLoggedInUser().id);
            startActivity(intent);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 0) {
            tab.setIcon(R.drawable.messages_unselected);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }
}
