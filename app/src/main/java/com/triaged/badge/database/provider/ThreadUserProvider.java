package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.ThreadUserTable;

/**
 * Created by Sadegh Kazemy on 9/29/14.
 */
public class ThreadUserProvider extends AbstractProvider {

    public ThreadUserProvider() {}

    public static final String AUTHORITY = "com.triaged.badge.provider.thread_users";
    static final String TABLE_THREAD_USER = ThreadUserTable.TABLE_NAME;

    static final String URI_THREAD_USER = "content://" + AUTHORITY + "/" + TABLE_THREAD_USER;
    static final String URI_THREAD_USER_INFO = "content://" + AUTHORITY + "/" + TABLE_THREAD_USER + "_contact_info";

    public static final Uri CONTENT_URI = Uri.parse(URI_THREAD_USER);
    public static final Uri CONTENT_URI_CONTACT_INFO = Uri.parse(URI_THREAD_USER_INFO);

    /**
     * integer values used in content URI
     */
    static final int RECORDS_WITH_CONTACTS = 3;

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_THREAD_USER, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_THREAD_USER + "/#", RECORD_ID);
        uriMatcher.addURI(AUTHORITY, TABLE_THREAD_USER + "_contact_info", RECORDS_WITH_CONTACTS);
    }


    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uriType(uri) == RECORDS_WITH_CONTACTS) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            checkColumns(projection);
            queryBuilder.setTables(ThreadUserTable.TABLE_NAME + " LEFT OUTER JOIN " +
                    ContactsTable.TABLE_NAME + " ON " +
                    ThreadUserTable.TABLE_NAME + "." + ThreadUserTable.CLM_USER_ID + " = " +
                    ContactsTable.TABLE_NAME + "." + ContactsTable.COLUMN_ID);
            SQLiteDatabase database = databaseHelper.getReadableDatabase();
            Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
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
        return TABLE_THREAD_USER;
    }

    @Override
    protected String authority() {
        return AUTHORITY;
    }

    @Override
    protected void checkColumns(String[] projection) {

    }
}
