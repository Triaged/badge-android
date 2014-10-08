package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.database.table.UsersTable;

/**
 * Created by Sadegh Kazemy on 9/17/14.
 */
public class ReceiptProvider extends AbstractProvider {

    public ReceiptProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.receipts";
    static final String TABLE_NAME = ReceiptTable.TABLE_NAME;
    static final String URI = "content://" + AUTHORITY + "/" + TABLE_NAME;
    public static final Uri CONTENT_URI = Uri.parse(URI);

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", RECORD_ID);
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(ReceiptTable.TABLE_NAME + " LEFT OUTER JOIN " +
                UsersTable.TABLE_NAME + " ON " +
                ReceiptTable.TABLE_NAME + "." + ReceiptTable.CLM_USER_ID + " = " +
                UsersTable.TABLE_NAME + "." + UsersTable.COLUMN_ID);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
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
    protected String authority() {
        return AUTHORITY;
    }

    @Override
    protected void checkColumns(String[] projection) {

    }
}
