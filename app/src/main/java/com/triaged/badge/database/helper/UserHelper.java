package com.triaged.badge.database.helper;

import android.content.ContentValues;
import android.database.Cursor;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.EmployeeInfo;
import com.triaged.badge.models.User;

/**
 * Created by Sadegh Kazemy on 9/20/14.
 */
public class UserHelper {

    public static ContentValues toContentValue(User user) {
        ContentValues values = new ContentValues();
        values.put(ContactsTable.COLUMN_ID, user.getId());
        if (user.getFirstName() != null) values.put(ContactsTable.COLUMN_CONTACT_FIRST_NAME, user.getFirstName());
        if (user.getLastName() != null) values.put(ContactsTable.COLUMN_CONTACT_LAST_NAME, user.getLastName());
        if (user.getAvatarUrl() != null) values.put(ContactsTable.COLUMN_CONTACT_AVATAR_URL, user.getAvatarFaceUrl());
        if (user.getEmail() != null) values.put(ContactsTable.COLUMN_CONTACT_EMAIL, user.getEmail());
        values.put(ContactsTable.COLUMN_CONTACT_MANAGER_ID, user.getManagerId());
        values.put(ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, user.getPrimaryOfficeLocationId());
        values.put(ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, user.currentOfficeLocationId());
        values.put(ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID, user.getDepartmentId());
        values.put(ContactsTable.COLUMN_CONTACT_IS_ARCHIVED, user.isArchived());
        values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, user.isSharingLocation());
//        if (json.has("sharing_office_location") && !json.isNull("sharing_office_location")) {
//            int sharingInt = json.getBoolean("sharing_office_location") ? Contact.SHARING_LOCATION_TRUE : Contact.SHARING_LOCATION_FALSE;
//            values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, sharingInt);
//        } else {
//            values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, Contact.SHARING_LOCATION_UNAVAILABLE);
//        }
        EmployeeInfo info = user.getEmployeeInfo();
        if ( info != null) {
            if (info.getJobTitle() != null)
                values.put(ContactsTable.COLUMN_CONTACT_JOB_TITLE, info.getJobTitle());
            if (info.getJobStartDate() != null)
                values.put(ContactsTable.COLUMN_CONTACT_START_DATE,
                        Contact.convertStartDateString(info.getJobStartDate()));
            if (info.getBirthDate() != null)
                values.put(ContactsTable.COLUMN_CONTACT_BIRTH_DATE,
                        Contact.convertBirthDateString(info.getBirthDate()));
            if (info.getCellPhone() != null) values.put(ContactsTable.COLUMN_CONTACT_CELL_PHONE, info.getCellPhone());
            if (info.getOfficePhone() != null) values.put(ContactsTable.COLUMN_CONTACT_OFFICE_PHONE, info.getOfficePhone());
            if (info.getWebsite() != null) values.put(ContactsTable.COLUMN_CONTACT_WEBSITE, info.getWebsite());
            if (info.getLinkedin() != null) values.put(ContactsTable.COLUMN_CONTACT_LINKEDIN, info.getLinkedin());
        }


        return values;
    }

    public static User fromCursor(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_ID)));
        user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_FIRST_NAME)));
        user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_LAST_NAME)));
        user.setFullName(String.format("%s %s", user.getFirstName(), user.getLastName()));
        user.setAvatarFaceUrl(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_AVATAR_URL)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_EMAIL)));
        user.setManagerId(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_MANAGER_ID)));
        user.setDepartmentId(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID)));
        user.setPrimaryOfficeLocationId(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID)));
        user.setCurrentOfficeLocationId(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID)));
        user.setArchived(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_IS_ARCHIVED)) > 0);
        user.setSharingOfficeLocationStatus(cursor.getInt(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION)) > 0);

        EmployeeInfo employeeInfo = new EmployeeInfo();
        user.setEmployeeInfo(employeeInfo);

        employeeInfo.setJobTitle(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_JOB_TITLE)));
        employeeInfo.setJobStartDate(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_START_DATE)));
        employeeInfo.setBirthDate(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_BIRTH_DATE)));
        employeeInfo.setCellPhone(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_CELL_PHONE)));
        employeeInfo.setOfficePhone(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_OFFICE_PHONE)));
        employeeInfo.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_WEBSITE)));
        employeeInfo.setLinkedin(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.COLUMN_CONTACT_LINKEDIN)));

        return user;
    }
}
