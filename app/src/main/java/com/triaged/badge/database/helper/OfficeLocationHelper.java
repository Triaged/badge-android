package com.triaged.badge.database.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.OfficeLocation;

/**
 * Created by Sadegh Kazemy on 9/23/14.
 */
public class OfficeLocationHelper {

    private OfficeLocationHelper() { }

    public static ContentValues toContentValue(OfficeLocation officeLocation) {
        ContentValues values = new ContentValues();

        values.put(UsersTable.COLUMN_ID, officeLocation.getId());
        if (officeLocation.getName() != null)
            values.put(OfficeLocationsTable.CLM_NAME, officeLocation.getName());
        if (officeLocation.getCity() != null)
            values.put(OfficeLocationsTable.CLM_CITY, officeLocation.getCity());
        if (officeLocation.getState() != null)
            values.put(OfficeLocationsTable.CLM_STATE, officeLocation.getState());
        if (officeLocation.getCountry() != null)
            values.put(OfficeLocationsTable.CLM_COUNTRY, officeLocation.getCountry());
        if (officeLocation.getLatitude() != null)
            values.put(OfficeLocationsTable.CLM_LAT, officeLocation.getLatitude());
        if (officeLocation.getLongitude() != null)
            values.put(OfficeLocationsTable.CLM_LNG, officeLocation.getLongitude());
        if (officeLocation.getStreetAddress() != null)
            values.put(OfficeLocationsTable.CLM_ADDRESS, officeLocation.getStreetAddress());
        if (officeLocation.getZipCode() != null)
            values.put(OfficeLocationsTable.CLM_ZIP, officeLocation.getZipCode());

        return values;
    }

    public static String getOfficeLocationName(Context context, String officeLocationId) {
        Cursor cursor = context.getContentResolver().query(
                OfficeLocationProvider.CONTENT_URI,
                new String[]{OfficeLocationsTable.CLM_NAME},
                OfficeLocationsTable.COLUMN_ID + " =?",
                new String[]{officeLocationId},
                null);
        if (cursor.moveToFirst()) {
            return Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_NAME);
        }
        return null;
    }

    public static Cursor getOfficeLocationsCursor(Context context) {
        return context.getContentResolver().query(OfficeLocationProvider.CONTENT_URI,
                null, null, null, OfficeLocationsTable.CLM_NAME);
    }
}
