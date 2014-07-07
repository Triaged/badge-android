package com.triaged.badge.data;

import android.database.Cursor;

import com.triaged.badge.app.DataProviderService;

/**
 * POJO representation of a contact.
 *
 * @author Created by Will on 7/7/14.
 */
public class Contact {
    public String firstName;
    public String lastName;
    public String avatarUrl;

    public Contact() {

    }

    public void fromCursor( Cursor contactCursor ) {
        this.firstName = contactCursor.getString( contactCursor.getColumnIndex(DataProviderService.COLUMN_CONTACT_FIRST_NAME ) );
        this.lastName = contactCursor.getString( contactCursor.getColumnIndex( DataProviderService.COLUMN_CONTACT_LAST_NAME ) );
        this.avatarUrl = contactCursor.getString( contactCursor.getColumnIndex( DataProviderService.COLUMN_CONTACT_AVATAR_URL ) );
    }
}
