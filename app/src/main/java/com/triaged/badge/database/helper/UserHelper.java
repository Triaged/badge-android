package com.triaged.badge.database.helper;

import android.content.ContentValues;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.EmployeeInfo;
import com.triaged.badge.models.User;

import org.json.JSONObject;

/**
 * Created by Sadegh Kazemy on 9/20/14.
 */
public class UserHelper {

    public static ContentValues toContentValue(User user) {
        ContentValues values = new ContentValues();
        values.put(ContactsTable.COLUMN_ID, user.getId());
        if (user.getFirstName() != null) values.put(ContactsTable.COLUMN_CONTACT_FIRST_NAME, user.getFirstName());
        if (user.getLastName() != null) values.put(ContactsTable.COLUMN_CONTACT_LAST_NAME, user.getLastName());
        if (user.getAvatarUrl() != null) values.put(ContactsTable.COLUMN_CONTACT_AVATAR_URL, user.getAvatarUrl());
        if (user.getEmail() != null) values.put(ContactsTable.COLUMN_CONTACT_EMAIL, user.getEmail());
        if (user.getManagerId() != null) values.put(ContactsTable.COLUMN_CONTACT_MANAGER_ID, user.getManagerId());
        if (user.getPrimaryOfficeLocationid() != null) values.put(ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, user.getPrimaryOfficeLocationid());
        if (user.getCurrentOfficeLocaitonId() != null) values.put(ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, user.getCurrentOfficeLocaitonId());
        if (user.getDepartmentId() != null) values.put(ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID, user.getDepartmentId());

        values.put(ContactsTable.COLUMN_CONTACT_IS_ARCHIVED, user.isArchived());
        values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, user.getSharingOfficeLocationStatus());

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
}
