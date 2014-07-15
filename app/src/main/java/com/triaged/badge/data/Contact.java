package com.triaged.badge.data;

import android.database.Cursor;
import android.util.Log;

import com.triaged.badge.app.WelcomeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * POJO representation of a contact.
 *
 * @author Created by Will on 7/7/14.
 */
public class Contact {
    public static final TimeZone GMT = TimeZone.getTimeZone( "GMT" );

    private static final String LOG_TAG = Contact.class.getName();


    public int id;
    public String firstName;
    public String lastName;
    public String avatarUrl;
    public String name;
    public String jobTitle;
    public String departmentName;
    public String managerName;
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
    public String initials;

    public static String getStringSafelyFromCursor( Cursor contactCursor, String columnName ) {
        int index = contactCursor.getColumnIndex( columnName );
        if( index != -1 ) {
            return contactCursor.getString( index );
        }
        return null;
    }

    public static int getIntSafelyFromCursor( Cursor contactCursor, String columnName ) {
        int index = contactCursor.getColumnIndex(columnName);
        if (index != -1) {
            return contactCursor.getInt(index);
        }
        return -1;
    }

    /**
     * Strings come from the api in iso 8601 format, we show and store dates
     * as just a human readable month and year.
     *
     * @param bday string representing date in iso 8601 format a la rails
     * @return "August 3"
     */
    public static String convertBirthdayString( String bday ) {
        try {
            DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000Z");
            Date birthDate = iso8601.parse(bday.replace( "+00:00", "+0000" ) );
            SimpleDateFormat ui = new SimpleDateFormat( WelcomeActivity.BIRTHDAY_FORMAT_STRING );
            ui.setTimeZone( GMT );
            return( ui.format( birthDate ) );
        }
        catch( ParseException e ) {
            Log.w(LOG_TAG, "Error parsing date from server as iso 8601 " + bday, e );
        }
        return null;
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
        this.id = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_ID );
        /** STRING FIELDS */
        firstName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME );
        lastName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME );
        avatarUrl = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_AVATAR_URL );
        email = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_EMAIL );
        startDateString = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_START_DATE );
        birthDateString = getStringSafelyFromCursor( contactCursor,  CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE );
        cellPhone = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_CELL_PHONE );
        officePhone = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_OFFICE_PHONE );
        jobTitle = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE );
        departmentName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.JOINED_DEPARTMENT_NAME );
        String managerFirstName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.JOINED_MANAGER_FIRST_NAME);
        String managerLastName = getStringSafelyFromCursor( contactCursor, CompanySQLiteHelper.JOINED_MANAGER_LAST_NAME);

        /** INTEGER FIELDS */
        sharingOfficeLocationInt = getIntSafelyFromCursor(contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_SHARING_OFFICE_LOCATION);
        managerId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID );
        primaryOfficeLocationId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID );
        currentOfficeLocationId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID );
        departmentId = getIntSafelyFromCursor( contactCursor, CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID );

        if( managerId > 0) {
            managerName = String.format("%s %s", managerFirstName, managerLastName);
        }

        /** DYNAMIC FIELDS */
        constructName();
        constructInitials();
        sharingOfficeLocation = sharingOfficeLocationInt == 1;
    }

    public void fromJSON( JSONObject contactJson ) throws JSONException {

        id = contactJson.getInt( "id" );
        if( !contactJson.isNull( "first_name" ) ) {
            firstName = contactJson.getString( "first_name" );
        }
        if( !contactJson.isNull( "last_name" ) ) {
            lastName = contactJson.getString( "last_name" );
        }
        JSONObject employeeInfo  = contactJson.getJSONObject("employee_info" );
        if( !employeeInfo.isNull( "cell_phone" ) ) {
            cellPhone = employeeInfo.getString( "cell_phone" );
        }
        if( !employeeInfo.isNull( "birth_date" ) ) {
            birthDateString = convertBirthdayString( employeeInfo.getString("birth_date") );
        }
        constructName();
    }

    private void constructName() {
        name = String.format( "%s %s", firstName, lastName );
    }

    private void constructInitials() {
        initials = String.valueOf(firstName.substring(0,1) + lastName.substring(0,1)).toUpperCase();
    }

    @Override
    public String toString() {
        return String.format( "Name: %s %s, avatar: %s, id: %d", firstName, lastName, avatarUrl, id );
    }

}
