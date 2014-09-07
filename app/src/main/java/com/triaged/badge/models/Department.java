package com.triaged.badge.models;

import android.database.Cursor;

import com.triaged.badge.database.CompanySQLiteHelper;

/**
 * POJO representing a department.
 *
 * @author Created by jc on 7/14/14.
 */
public class Department {
    public int id;
    public String name;
    public int numContacts;

    /**
     * Populate this dept pojo with values from the current row in the
     * cursor from the local SQLite database.
     *
     * @param deptCursor cursor in to sql lite db.
     */
    public void fromCursor(Cursor deptCursor) {
        id = Contact.getIntSafelyFromCursor(deptCursor, CompanySQLiteHelper.COLUMN_DEPARTMENT_ID);
        name = Contact.getStringSafelyFromCursor(deptCursor, CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME);
        numContacts = Contact.getIntSafelyFromCursor(deptCursor, CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS);
    }
}
