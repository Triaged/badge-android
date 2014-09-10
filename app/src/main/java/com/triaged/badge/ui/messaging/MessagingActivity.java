package com.triaged.badge.ui.messaging;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;


public class MessagingActivity extends SlidingActivity {

    public static final String THREAD_ID_EXTRA = "thread_id_extra";

    private String mThreadId;

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThreadId = getIntent().getExtras().getString(THREAD_ID_EXTRA);

        setupActionbar();

//        setTitle("Chatting!!");

        setContentView(R.layout.activity_messaging_content_frame);

        setBehindContentView(R.layout.messaging_menu_frame);

        getSlidingMenu().setSlidingEnabled(true);
        getSlidingMenu().setTouchModeAbove(SlidingMenu.RIGHT);
        getSlidingMenu().setMode(SlidingMenu.RIGHT);
        setSlidingActionBarEnabled(false);


        if (savedInstanceState == null) {
//            getFragmentManager().beginTransaction()
//                    .add(R.id.container, MessagingFragment.newInstance(mThreadId))
//                    .commit();

            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, MessagingFragment.newInstance(mThreadId))
                    .commit();

            getFragmentManager().beginTransaction()
                    .replace(R.id.menu_frame, MenuFragment.newInstance(mThreadId))
                    .commit();


            // customize the SlidingMenu
            SlidingMenu sm = getSlidingMenu();
            sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
            sm.setShadowWidthRes(R.dimen.shadow_width);
            sm.setShadowDrawable(R.drawable.shadowright);
            sm.setBehindScrollScale(0.25f);
            sm.setFadeDegree(0.25f);
        }
    }

    private void setupActionbar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(App.dataProviderServiceBinding.getRecipientNames(mThreadId));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messaging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_show_sliding_menu:
                if (getSlidingMenu().isSecondaryMenuShowing()) {
                    showContent();
                } else {
                    showSecondaryMenu();
                }
                return true;

            default:
                return false;
        }
    }
}
