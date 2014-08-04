package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.MessagesListAdapter;

import org.w3c.dom.Text;

import java.util.List;

/**
 * This is the list of active message threads.
 *
 * @author Created by Will on 7/15/14.
 */
public class MessagesIndexActivity extends BadgeActivity implements ActionBar.TabListener {

    protected ListView messagesList;
    protected MessagesListAdapter adapter;
    private BroadcastReceiver refreshReceiver;

    private ImageView noMessagesImage;
    private TextView noMessagesTitle;
    private TextView noMessagesInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_selected).setTabListener(this), true);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_unselected).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_unselected).setTabListener(this), false);

        setContentView(R.layout.activity_messages_index);
        messagesList = (ListView) findViewById(R.id.messages_list);

        messagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MessagesIndexActivity.this, MessageShowActivity.class);
                intent.putExtra( MessageShowActivity.THREAD_ID_EXTRA,  ((MessagesListAdapter.ViewHolder)view.getTag()).threadId );
                intent.putExtra(MessageShowActivity.SHOW_KEYBOARD_EXTRA, false);
                intent.setFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                startActivity(intent);
            }
        });

        IntentFilter messageUpdateFilter = new IntentFilter( DataProviderService.NEW_MSG_ACTION );
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.changeCursor( dataProviderServiceBinding.getThreads() );
                adapter.notifyDataSetChanged();
                toggleUI();
            }
        };
        localBroadcastManager.registerReceiver( refreshReceiver, messageUpdateFilter );

        ImageButton composeButton = (ImageButton) findViewById(R.id.compose_button);
        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessagesIndexActivity.this, MessageNewActivity.class);
                startActivity(intent);
            }
        });

        noMessagesImage = (ImageView) findViewById(R.id.no_messages_image);
        noMessagesTitle = (TextView) findViewById(R.id.no_messages_title);
        noMessagesInfo = (TextView) findViewById(R.id.no_messages_info);
    }

    protected void toggleUI() {
        if (adapter.getCount() == 0) {
            noMessagesImage.setVisibility(View.VISIBLE);
            noMessagesTitle.setVisibility(View.VISIBLE);
            noMessagesInfo.setVisibility(View.VISIBLE);
        } else {
            noMessagesImage.setVisibility(View.GONE);
            noMessagesTitle.setVisibility(View.GONE);
            noMessagesInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDatabaseReady() {
        adapter = new MessagesListAdapter(dataProviderServiceBinding, this, dataProviderServiceBinding.getThreads(), false );
        messagesList.setAdapter(adapter);
        toggleUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Notifier.clearNotifications( this );
        overridePendingTransition(0,0);
        ActionBar actionBar = getActionBar();
        actionBar.getTabAt(0).setIcon(R.drawable.messages_selected).select();
        actionBar.getTabAt(1).setIcon(R.drawable.contacts_unselected);
        actionBar.getTabAt(2).setIcon(R.drawable.profile_unselected);
    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver( refreshReceiver );
        adapter.destroy();
        super.onDestroy();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 1) {
            tab.setIcon(R.drawable.contacts_selected);
            Intent intent = new Intent(this, ContactsActivity.class);
            startActivity(intent);
            overridePendingTransition(0,0);
        } else if (tab.getPosition() == 2) {
            tab.setIcon(R.drawable.profile_selected);
            Intent intent = new Intent( this, MyProfileActivity.class );
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("PROFILE_ID", dataProviderServiceBinding.getLoggedInUser().id);
            startActivity(intent);
            overridePendingTransition(0,0);
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

    @Override
    protected void notifyNewMessage(Intent intent) {
        // Don't notify
    }
}
