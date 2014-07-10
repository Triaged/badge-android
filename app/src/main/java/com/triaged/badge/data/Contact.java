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
    public String jobTitle;
    public String email;
    public String startDateString;
    public String birthDateString;
    public String cellPhone;
    public String officePhone;
    public int managerId;
    public int primaryOfficeLocationId;
    public int currentOfficeLocationId;
    public int departmentId;
    public int sharingOfficeLocationInt;
    public boolean sharingOfficeLocation;

    public static String getStringSafelyFromCursor( Cursor contactCursor, String columnName ) {
        int index = contactCursor.getColumnIndex( columnName );
        if( index != -1 ) {
            return contactCursor.getString( index );
        }
        return null;
    }

    public static int getIntSafelyFromCursor( Cursor contactCursor, String columnName ) {
        int index = contactCursor.getColumnIndex( columnName );
        if( index != -1 ) {
            return contactCursor.getInt( index );
        }
        return -1;
    }


    public Contact() {

    }

    /**
     * Populate this contact pojo with values from a row in the local
     * SQLite database.
     *
     * @param contactCursor cursor in to sql lite db.
     */
    public void fromCursor( Cursor contactCursor ) {
        int index;
        index = contactCursor.getColumnIndex( CompanySQLiteHelper.COLUMN_CONTACT_ID );
        if( index != -1 ) {
            this.id = contactCursor.getInt( index );
        }

        /** STRING FIELDS */
        this.firstName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME );
        this.lastName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME );
        this.avatarUrl = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_AVATAR_URL );
        this.email = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_EMAIL );
        this.startDateString = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_START_DATE );
        this.birthDateString = getStringSafelyFromCursor( contactCursor,  CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE );
        this.cellPhone = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_CELL_PHONE );
        this.officePhone = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_OFFICE_PHONE );
        this.jobTitle = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE );

        /** INTEGER FIELDS */
        this.sharingOfficeLocationInt = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_SHARING_OFFICE_LOCATION );
        this.managerId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID );
        this.primaryOfficeLocationId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID );
        this.currentOfficeLocationId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID );
        this.departmentId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID );

        /** DYNAMIC FIELDS */
        this.name = String.format( "%s %s", firstName, lastName );
        this.sharingOfficeLocation = this.sharingOfficeLocationInt == 1;
    }

    @Override
    public String toString() {
        return String.format( "Name: %s %s, avatar: %s, id: %d", firstName, lastName, avatarUrl, id );
    }

}
