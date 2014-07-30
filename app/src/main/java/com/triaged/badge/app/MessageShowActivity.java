package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.triaged.badge.app.views.MessageThreadAdapter;
import com.triaged.badge.data.Contact;

/**
 * Activity for a message thread.
 *
 * Created by Will on 7/15/14.
 */
public class MessageShowActivity extends BadgeActivity {

    public static final String THREAD_ID_EXTRA = "threadId";

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    private ListView threadList;
    private MessageThreadAdapter adapter;
    private EditText postBox;
    private RelativeLayout postBoxWrapper;
    private float densityMultiplier = 1;
    private boolean expanded = false;
    private Contact counterPart;
    private BroadcastReceiver refreshReceiver;
    protected ImageButton sendButton;
    protected String threadId;
    private TextView backButton;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();
        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        setContentView(R.layout.activity_message_show);

        backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageButton profileButton = (ImageButton) findViewById(R.id.thread_members_button);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessageShowActivity.this, OtherProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", counterPart.id);
                startActivity(intent);
            }
        });

        threadId = getIntent().getStringExtra( THREAD_ID_EXTRA );

        threadList = (ListView) findViewById(R.id.message_thread);
        adapter = new MessageThreadAdapter(this, threadId, dataProviderServiceBinding );
        threadList.setAdapter(adapter);
        threadList.setSelection(adapter.getCount() - 1);
        threadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getCurrentFocus() != null) {
                    getCurrentFocus().clearFocus();
                }
            }
        });


        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if( threadId.equals( intent.getStringExtra( DataProviderService.THREAD_ID_EXTRA ) ) ) {
                    adapter.changeCursor( dataProviderServiceBinding.getMessages(threadId) );
                    adapter.notifyDataSetChanged();
                }
            }
        };
        localBroadcastManager.registerReceiver( refreshReceiver, new IntentFilter( DataProviderService.NEW_MSG_ACTION ) );

        postBox = (EditText) findViewById(R.id.input_box);

        postBoxWrapper = (RelativeLayout) findViewById(R.id.post_box_wrapper);

        densityMultiplier = getResources().getDisplayMetrics().density;

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (postBox.getLineCount() > 2 && !expanded) {
                    expand(postBoxWrapper);
                    expanded = true;
                } else if (postBox.getLineCount() < 3 && expanded) {
                    collapse(postBoxWrapper);
                    expanded = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        sendButton = (ImageButton)findViewById( R.id.send_now_button );
        sendButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = postBox.getText().toString();
                dataProviderServiceBinding.sendMessageAsync( threadId, msg );
                postBox.setText( "" );
            }

        } );

        postBox.addTextChangedListener(textWatcher);

    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver( refreshReceiver );
        adapter.destroy();
        super.onDestroy();
    }

    public void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = (int) (64 * densityMultiplier);

        v.getLayoutParams().height = 0;
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = targetHeight;
                v.requestLayout();
            }
            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / densityMultiplier));
        v.startAnimation(a);
    }

    public void collapse(final View v) {
        final int initialHeight = (int) (48 * densityMultiplier);

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = initialHeight;
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / densityMultiplier));
        v.startAnimation(a);
    }

    @Override
    protected void onResume() {
        super.onResume();
        backButton.setText( dataProviderServiceBinding.getRecipientNames( threadId ) );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        threadId = intent.getStringExtra( THREAD_ID_EXTRA );
        super.onNewIntent(intent);
    }
}
