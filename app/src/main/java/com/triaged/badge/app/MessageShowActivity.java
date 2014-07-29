package com.triaged.badge.app;

import android.content.Intent;
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

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    private ListView threadList;
    private MessageThreadAdapter adapter;
    private EditText postBox;
    private RelativeLayout postBoxWrapper;
    private float densityMultiplier = 1;
    private boolean expanded = false;
    private Contact counterPart;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        threadList = (ListView) findViewById(R.id.message_thread);

        String[] values = new String[] {
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,",
            "when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. ",
            "It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.",
            "Where?",
            "Contrary",
            "It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage,",
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,",
            "when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. ",
            "It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.",
            "Where does it come from?",
            "Contrary to popular belief, Lorem Ipsum is not simply random text.",
            "It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage,"
        };

        adapter = new MessageThreadAdapter(this, values);
        threadList.setAdapter(adapter);
        threadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getCurrentFocus() != null) {
                    getCurrentFocus().clearFocus();
                }
            }
        });

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

        postBox.addTextChangedListener(textWatcher);

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

        Intent intent = getIntent();
        int userId = intent.getIntExtra(MessageNewActivity.RECIPIENT_ID_EXTRA, 0);
        if (userId != 0) {
            counterPart = dataProviderServiceBinding.getContact(userId);
            backButton.setText(counterPart.name);
        } else {
            backButton.setText("Back");
        }
    }
}
