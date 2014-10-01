package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.triaged.badge.app.App;
import com.triaged.badge.database.DatabaseHelper;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.database.table.UsersTable;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class UserProvider extends AbstractProvider {
    public UserProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.users";
    static final String TABLE_NAME = UsersTable.TABLE_NAME;
    static final String URI = "content://" + AUTHORITY + "/" + TABLE_NAME;
    static final String URI_FULL_INFO = "content://" + AUTHORITY + "/" + TABLE_NAME + "/full_info";

    public static final Uri CONTENT_URI = Uri.parse(URI);
    public static final Uri CONTENT_URI_FULL_INFO = Uri.parse(URI_FULL_INFO);

    static final int RECORD_FULL_INFO = 3;

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", RECORD_ID);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "/full_info/#", RECORD_FULL_INFO);
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uriType(uri) == RECORD_FULL_INFO) {
            String query = "SELECT user.*," +
                    " office." + OfficeLocationsTable.CLM_NAME + " " + DatabaseHelper.JOINED_OFFICE_NAME +
                    ", department." + DepartmentsTable.CLM_NAME + " " + DatabaseHelper.JOINED_DEPARTMENT_NAME +
                    ", manager." + UsersTable.CLM_FIRST_NAME + " " + DatabaseHelper.JOINED_MANAGER_FIRST_NAME +
                    ", manager." + UsersTable.CLM_LAST_NAME + " " + DatabaseHelper.JOINED_MANAGER_LAST_NAME +
                    " FROM " + UsersTable.TABLE_NAME + " user LEFT JOIN " +
                        DepartmentsTable.TABLE_NAME + " department ON user." + UsersTable.CLM_DEPARTMENT_ID +
                        " =department." + DepartmentsTable.COLUMN_ID +
                    " LEFT JOIN " + UsersTable.TABLE_NAME + " manager ON user." + UsersTable.CLM_MANAGER_ID +
                    " = manager." + UsersTable.COLUMN_ID +
                    " LEFT JOIN " + OfficeLocationsTable.TABLE_NAME + " office ON user." + UsersTable.CLM_PRIMARY_OFFICE_LOCATION_ID +
                    " = office." + OfficeLocationsTable.COLUMN_ID +
                    " WHERE user." + UsersTable.COLUMN_ID + "=?";

            SQLiteDatabase database = databaseHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = database.rawQuery(query, new String[] {uri.getLastPathSegment()});
            } catch (Throwable throwable) {
                App.gLogger.e(throwable);
            }
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;

        } else {
            return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }
    }

    @Override
    protected int uriType(Uri uri) {
        return uriMatcher.match(uri);
    }

    @Override
    protected String basePath() {
        return TABLE_NAME;
    }

    @Override
    protected void checkColumns(String[] projection) {
        // for now, just return, without any error.
        return ;
    }

    @Override
    protected String authority() {
        return AUTHORITY;
    }
}
