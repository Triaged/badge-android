package com.triaged.badge.ui.messaging;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;


public class MessagingActivity extends Activity {

    public static final String THREAD_ID_EXTRA = "thread_id_extra";

    private String mThreadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThreadId = getIntent().getExtras().getString(THREAD_ID_EXTRA);
        setupActionbar();

        setContentView(R.layout.activity_messaging);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, MessagingFragment.newInstance(mThreadId))
                    .commit();
        }
    }

    private void setupActionbar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(App.dataProviderServiceBinding.getRecipientNames(mThreadId));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_show_sliding_menu:
                Toast.makeText(this, "Would show sliding menu!", Toast.LENGTH_LONG).show();
                return true;

            default:
                return false;
        }
    }
}
