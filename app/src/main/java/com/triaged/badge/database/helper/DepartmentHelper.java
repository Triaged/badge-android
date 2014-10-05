package com.triaged.badge.database.helper;

import android.content.ContentValues;

import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.models.Department;

/**
 * Created by Sadegh Kazemy on 9/23/14.
 */
public class DepartmentHelper {

    private DepartmentHelper() { }

    public static ContentValues toContentValue(Department department) {
        ContentValues values = new ContentValues();
        values.put(DepartmentsTable.COLUMN_ID, department.id);
        values.put(DepartmentsTable.CLM_CONTACTS_NUMBER, department.usersCount);
        if (department.name != null) values.put(DepartmentsTable.CLM_NAME, department.name);
        return values;
    }
}
