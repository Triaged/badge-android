package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.triaged.badge.app.views.MessageThreadAdapter;
import com.triaged.badge.data.Contact;

/**
 * Created by Will on 7/15/14.
 */
public class MessageShowActivity extends BadgeActivity {

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    private ListView threadList;
    private MessageThreadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        setContentView(R.layout.activity_message_show);

        TextView backButton;
        backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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

        Intent intent = getIntent();
        int userId = intent.getIntExtra("CONTACT_ID", 0);
        if (userId != 0) {
            Contact counterPart = dataProviderServiceBinding.getContact(userId);
            backButton.setText(counterPart.name);
        } else {
            backButton.setText("Back");
        }
    }
}
