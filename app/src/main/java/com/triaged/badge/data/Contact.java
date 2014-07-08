package com.triaged.badge.data;

import android.database.Cursor;

import com.triaged.badge.app.DataProviderService;

/**
 * POJO representation of a contact.
 *
 * @author Created by Will on 7/7/14.
 */
public class Contact {
    public int id;
    public String firstName;
    public String lastName;
    public String avatarUrl;
    public String name;

    public Contact() {

    }

    /**
     * Populate this contact pojo with values from a row in the local
     * SQLite database.
     *
     * @param contactCursor cursor in to sql lite db.
     */
    public void fromCursor( Cursor contactCursor ) {
        this.id = contactCursor.getInt( contactCursor.getColumnIndex( DataProviderService.COLUMN_CONTACT_ID ) );
        this.firstName = contactCursor.getString( contactCursor.getColumnIndex(DataProviderService.COLUMN_CONTACT_FIRST_NAME ) );
        this.lastName = contactCursor.getString( contactCursor.getColumnIndex( DataProviderService.COLUMN_CONTACT_LAST_NAME ) );
        this.avatarUrl = contactCursor.getString( contactCursor.getColumnIndex( DataProviderService.COLUMN_CONTACT_AVATAR_URL ) );
        this.name = String.format( "%s %s", firstName, lastName );
    }

    @Override
    public String toString() {
        return String.format( "Name: %s %s, avatar: %s, id: %d", firstName, lastName, avatarUrl, id );
    }
}
