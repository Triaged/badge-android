package com.triaged.badge.ui.messaging;

import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.BThreadProvider;
import com.triaged.badge.database.table.BThreadsTable;


public class MessagingActivity extends SlidingActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String THREAD_ID_EXTRA = "thread_id_extra";

    private String mThreadId;
    MenuFragment menuFragment;

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        getSlidingMenu().setSlidingEnabled(true);
        getSlidingMenu().setTouchModeAbove(SlidingMenu.RIGHT);
        getSlidingMenu().setMode(SlidingMenu.RIGHT);
        setSlidingActionBarEnabled(false);

        setContentView(R.layout.activity_messaging_content_frame);
        setBehindContentView(R.layout.messaging_menu_frame);

        if (savedInstanceState == null) {
            mThreadId = getIntent().getExtras().getString(THREAD_ID_EXTRA);
            getFragmentManager().beginTransaction()
                    .add(R.id.content_frame, MessagingFragment.newInstance(mThreadId))
                    .commit();

            menuFragment = MenuFragment.newInstance(mThreadId);
            getFragmentManager().beginTransaction()
                    .add(R.id.menu_frame, menuFragment)
                    .commit();

            // customize the SlidingMenu
            SlidingMenu sm = getSlidingMenu();
            sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
            sm.setShadowWidthRes(R.dimen.shadow_width);
            sm.setShadowDrawable(R.drawable.shadowright);
            sm.setBehindScrollScale(0.25f);
            sm.setFadeDegree(0.25f);
        }
        getLoaderManager().initLoader(0, savedInstanceState, this);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, BThreadProvider.CONTENT_URI,
                null,
                BThreadsTable.CLM_BTHREAD_ID + "=?",
                new String[]{mThreadId},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String threadName = data.getString(data.getColumnIndexOrThrow(BThreadsTable.CLM_NAME));
            boolean isMute = data.getInt(data.getColumnIndexOrThrow(BThreadsTable.CLM_IS_MUTED)) > 0;
            getActionBar().setTitle(threadName);
            menuFragment.setThreadName(threadName);
            menuFragment.setMute(isMute);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
