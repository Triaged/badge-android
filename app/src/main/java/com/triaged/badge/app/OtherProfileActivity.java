package com.triaged.badge.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Profile Activity for other users (not the logged-in-user)
 *
 * Created by Will on 7/10/14.
 */
public class OtherProfileActivity extends AbstractProfileActivity {

    private TextView backButton = null;
    private ImageButton makeCallButton = null;
    private ImageButton newEmailButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        ImageButton newMessageButton = (ImageButton) findViewById(R.id.new_message_button);
        newMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OtherProfileActivity.this, "New Message", Toast.LENGTH_SHORT).show();
            }
        });

        newEmailButton = (ImageButton) findViewById(R.id.new_email_button);
        newEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contact != null && contact.email != null) {
                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{contact.email});

                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "\n\n--\nsent via badge");

                    OtherProfileActivity.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                }
            }
        });

        makeCallButton = (ImageButton) findViewById(R.id.call_button);
        makeCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contact != null && contact.cellPhone != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.cellPhone));
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void setContentViewLayout() {
        setContentView(R.layout.activity_other_profile);
    }

    @Override
    protected void setupProfile() {
        super.setupProfile();
        backButton.setText(contact.name);
        if (contact.cellPhone == null) {
            makeCallButton.setVisibility(View.INVISIBLE);
        } else {
            makeCallButton.setVisibility(View.VISIBLE);
        }
        if (contact.email == null) {
            newEmailButton.setVisibility(View.INVISIBLE);
        } else {
            newEmailButton.setVisibility(View.VISIBLE);
        }

    }
}