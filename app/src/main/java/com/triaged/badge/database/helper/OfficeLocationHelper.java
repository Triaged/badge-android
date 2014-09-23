package com.triaged.badge.database.helper;

import android.content.ContentValues;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.models.OfficeLocation;

/**
 * Created by Sadegh Kazemy on 9/23/14.
 */
public class OfficeLocationHelper {

    private OfficeLocationHelper() { }

    public static ContentValues toContentValue(OfficeLocation officeLocation) {
        ContentValues values = new ContentValues();

        values.put(ContactsTable.COLUMN_ID, officeLocation.getId());
        if (officeLocation.getName() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME, officeLocation.getName());
        if (officeLocation.getCity() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_CITY, officeLocation.getCity());
        if (officeLocation.getState() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_STATE, officeLocation.getState());
        if (officeLocation.getCountry() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_COUNTRY, officeLocation.getCountry());
        if (officeLocation.getLatitude() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LAT, officeLocation.getLatitude());
        if (officeLocation.getLongitude() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LNG, officeLocation.getLongitude());
        if (officeLocation.getStreetAddress() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ADDRESS, officeLocation.getStreetAddress());
        if (officeLocation.getZipCode() != null)
            values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ZIP, officeLocation.getZipCode());

        return values;
    }
}
