package com.triaged.badge.app;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.MessageThreadAdapter;
import com.triaged.badge.data.Contact;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Activity for a message thread.
 *
 * Created by Will on 7/15/14.
 */
public class MessageShowActivity extends BadgeActivity {

    public static final String THREAD_ID_EXTRA = "threadId";
    private static final String LOG_TAG = MessageShowActivity.class.getName();

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
    private BroadcastReceiver dataAvailableReceiver;

    private int userCount = 2;
    private int soleCounterpartId = 0;
    private LinearLayout threadMembersWrapper = null;
    private LayoutInflater inflater;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();
        final BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        threadId = getIntent().getStringExtra( THREAD_ID_EXTRA );

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        inflater = LayoutInflater.from(this);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View backButtonBar = inflater.inflate(R.layout.actionbar_show_message, null);

        backButton = (TextView) backButtonBar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageButton profileButton = (ImageButton) backButtonBar.findViewById(R.id.thread_members_button);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userCount > 2) {
                    if (threadMembersWrapper.getVisibility() == View.VISIBLE) {
                        threadMembersWrapper.setVisibility(View.GONE);
                    } else {
                        threadMembersWrapper.setVisibility(View.VISIBLE);
                    }
                } else {
                    Intent intent = new Intent(MessageShowActivity.this, OtherProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("PROFILE_ID", soleCounterpartId);
                    startActivity(intent);
                }
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_message_show);

        threadList = (ListView) findViewById(R.id.message_thread);

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

        threadMembersWrapper = (LinearLayout) findViewById(R.id.thread_members);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        dataAvailableReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dataProviderServiceBinding = app.dataProviderServiceBinding;
                showThread();
            }
        };
        localBroadcastManager.registerReceiver( dataAvailableReceiver, new IntentFilter( DataProviderService.DB_AVAILABLE_ACTION) );

        if( dataProviderServiceBinding != null && dataProviderServiceBinding.isInitialized() ) {
            showThread();
        }

    }

    /**
     * This needs to be abstracted because it may need to happen asynchronously if
     * headed straight here from a push notification and database not set up yet.
     */
    protected void showThread() {
        dataProviderServiceBinding.markAsRead( threadId );
        adapter = new MessageThreadAdapter(this, threadId, dataProviderServiceBinding );
        threadList.setAdapter(adapter);
        threadList.setSelection(adapter.getCount() - 1);
        backButton.setText(dataProviderServiceBinding.getRecipientNames(threadId));
        setupContactsMenu();
    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver( refreshReceiver );
        localBroadcastManager.unregisterReceiver(dataAvailableReceiver);
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
        a.setDuration((int) (targetHeight / densityMultiplier));
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

    private void setupContactsMenu() {
        threadMembersWrapper.removeAllViews();
        String usersJsonString = prefs.getString(threadId, "[]");
        try {
            JSONArray users = new JSONArray(usersJsonString);
            userCount = users.length();
            for (int i=0; i<userCount; i++) {
                String user = (String) users.get(i);
                int userId = Integer.parseInt(user);
                if (userId != dataProviderServiceBinding.getLoggedInUser().id) {
                    final Contact c = dataProviderServiceBinding.getContact(userId);
                    if (userCount == 2) {
                        soleCounterpartId = c.id;
                    } else {
                        RelativeLayout contactView = (RelativeLayout) inflater.inflate(R.layout.item_contact_with_msg, null);
                        TextView contactName = (TextView) contactView.findViewById(R.id.contact_name);
                        contactName.setText(c.name);
                        TextView contactTitle = (TextView) contactView.findViewById(R.id.contact_title);
                        contactTitle.setText(c.jobTitle);
                        ImageView thumbImage = (ImageView) contactView.findViewById(R.id.contact_thumb);
                        TextView noPhotoThumb = (TextView) contactView.findViewById(R.id.no_photo_thumb);
                        noPhotoThumb.setText(c.initials);
                        noPhotoThumb.setVisibility(View.VISIBLE);
                        if (c.avatarUrl != null) {
                            dataProviderServiceBinding.setSmallContactImage(c, thumbImage, noPhotoThumb);
                        }
                        contactView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MessageShowActivity.this, OtherProfileActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                intent.putExtra("PROFILE_ID", c.id);
                                startActivity(intent);
                            }
                        });
                        threadMembersWrapper.addView(contactView);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Malformed users json");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String oldThreadId = threadId;
        threadId = intent.getStringExtra( THREAD_ID_EXTRA );
        if( !oldThreadId.equals( threadId ) ) {
            dataProviderServiceBinding.markAsRead( threadId );
            adapter.changeCursor( dataProviderServiceBinding.getMessages( threadId ) );
            adapter.notifyDataSetChanged();
            backButton.setText(dataProviderServiceBinding.getRecipientNames(threadId));
            setupContactsMenu();
        }
        super.onNewIntent(intent);
    }

    protected void scrollMyListViewToBottom() {
        threadList.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                threadList.setSelection(adapter.getCount() - 1);
            }
        });
    }
}
