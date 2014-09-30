package com.triaged.badge.database.helper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.EmployeeInfo;
import com.triaged.badge.models.User;

/**
 * Created by Sadegh Kazemy on 9/20/14.
 */
public class UserHelper {

    public static ContentValues toContentValue(User user) {
        ContentValues values = new ContentValues();
        values.put(UsersTable.COLUMN_ID, user.getId());
        if (user.getFirstName() != null) values.put(UsersTable.CLM_FIRST_NAME, user.getFirstName());
        if (user.getLastName() != null) values.put(UsersTable.CLM_LAST_NAME, user.getLastName());
        if (user.getAvatarUrl() != null) values.put(UsersTable.CLM_AVATAR_URL, user.getAvatarFaceUrl());
        if (user.getEmail() != null) values.put(UsersTable.CLM_EMAIL, user.getEmail());
        values.put(UsersTable.CLM_MANAGER_ID, user.getManagerId());
        values.put(UsersTable.CLM_PRIMARY_OFFICE_LOCATION_ID, user.getPrimaryOfficeLocationId());
        values.put(UsersTable.CLM_CURRENT_OFFICE_LOCATION_ID, user.currentOfficeLocationId());
        values.put(UsersTable.CLM_DEPARTMENT_ID, user.getDepartmentId());
        values.put(UsersTable.CLM_IS_ARCHIVED, user.isArchived());
        values.put(UsersTable.CLM_SHARING_OFFICE_LOCATION, user.isSharingLocation());
//        if (json.has("sharing_office_location") && !json.isNull("sharing_office_location")) {
//            int sharingInt = json.getBoolean("sharing_office_location") ? Contact.SHARING_LOCATION_TRUE : Contact.SHARING_LOCATION_FALSE;
//            values.put(ContactsTable.CLM_SHARING_OFFICE_LOCATION, sharingInt);
//        } else {
//            values.put(ContactsTable.CLM_SHARING_OFFICE_LOCATION, Contact.SHARING_LOCATION_UNAVAILABLE);
//        }
        EmployeeInfo info = user.getEmployeeInfo();
        if ( info != null) {
            if (info.getJobTitle() != null)
                values.put(UsersTable.CLM_JOB_TITLE, info.getJobTitle());
            if (info.getJobStartDate() != null)
                values.put(UsersTable.CLM_START_DATE,
                        Contact.convertStartDateString(info.getJobStartDate()));
            if (info.getBirthDate() != null)
                values.put(UsersTable.CLM_BIRTH_DATE,
                        Contact.convertBirthDateString(info.getBirthDate()));
            if (info.getCellPhone() != null) values.put(UsersTable.CLM_CELL_PHONE, info.getCellPhone());
            if (info.getOfficePhone() != null) values.put(UsersTable.CLM_OFFICE_PHONE, info.getOfficePhone());
            if (info.getWebsite() != null) values.put(UsersTable.CLM_WEBSITE, info.getWebsite());
            if (info.getLinkedin() != null) values.put(UsersTable.CLM_LINKEDIN, info.getLinkedin());
        }


        return values;
    }

    public static User fromCursor(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.COLUMN_ID)));
        user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_FIRST_NAME)));
        user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_LAST_NAME)));
        user.setFullName(String.format("%s %s", user.getFirstName(), user.getLastName()));
        user.setAvatarFaceUrl(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_AVATAR_URL)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_EMAIL)));
        user.setManagerId(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_MANAGER_ID)));
        user.setDepartmentId(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_DEPARTMENT_ID)));
        user.setPrimaryOfficeLocationId(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_PRIMARY_OFFICE_LOCATION_ID)));
        user.setCurrentOfficeLocationId(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_CURRENT_OFFICE_LOCATION_ID)));
        user.setArchived(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_IS_ARCHIVED)) > 0);
        user.setSharingOfficeLocationStatus(cursor.getInt(cursor.getColumnIndexOrThrow(UsersTable.CLM_SHARING_OFFICE_LOCATION)) > 0);

        EmployeeInfo employeeInfo = new EmployeeInfo();
        user.setEmployeeInfo(employeeInfo);

        employeeInfo.setJobTitle(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_JOB_TITLE)));
        employeeInfo.setJobStartDate(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_START_DATE)));
        employeeInfo.setBirthDate(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_BIRTH_DATE)));
        employeeInfo.setCellPhone(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_CELL_PHONE)));
        employeeInfo.setOfficePhone(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_OFFICE_PHONE)));
        employeeInfo.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_WEBSITE)));
        employeeInfo.setLinkedin(cursor.getString(cursor.getColumnIndexOrThrow(UsersTable.CLM_LINKEDIN)));

        return user;
    }

    public static User getUser(Context context, int userId) {
         Cursor cursor = context.getContentResolver().query(
                 ContentUris.withAppendedId(UserProvider.CONTENT_URI, userId),
                null,  null, null, null);
        if (cursor.moveToFirst()) {
            return fromCursor(cursor);
        } else {
            return null;
        }
    }

    public static String getUserAvatar(Context context, int userId) {
        Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(UserProvider.CONTENT_URI, userId),
                new String[]{UsersTable.CLM_AVATAR_URL},  null, null, null);
        if (cursor.moveToFirst()) {
            return cursor.getString(0);
        } else {
            return null;
        }
    }

    /**
     *
     * @param context Application context.
     * @param userId The Id of user's that we want their name.
     * @return Return an array containing first and last name of person or NULL
     */
    public static String[] getUserName(Context context, int userId) {
        Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(UserProvider.CONTENT_URI, userId),
                new String[]{
                        UsersTable.CLM_FIRST_NAME,
                        UsersTable.CLM_LAST_NAME
                },
                null, null, null);
        if (cursor.moveToFirst()) {
            return new String[] {cursor.getString(0), cursor.getString(1)};
        } else {
            return null;
        }
    }
}
