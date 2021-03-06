package com.triaged.badge.database.provider;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.MessagesTable;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class MessageProvider extends AbstractProvider {
    public MessageProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.messages";
    static final String TABLE_NAME = MessagesTable.TABLE_NAME;

    static final String URI = "content://" + AUTHORITY + "/" + TABLE_NAME;
    static final String URI_WITH_CONTACTS_INFO = "content://" + AUTHORITY + "/" + TABLE_NAME + "_with_contacts_info";

    public static final Uri CONTENT_URI = Uri.parse(URI);
    public static final Uri CONTENT_URI_WITH_CONTACTS_INFO = Uri.parse(URI_WITH_CONTACTS_INFO);


    /**
     * integer values used in content URI
     */
    static final int RECORD_WITH_INFO = 3;

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", RECORD_ID);
        uriMatcher.addURI(AUTHORITY, TABLE_NAME + "_with_contacts_info" , RECORD_WITH_INFO);
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        int uriType = uriType(uri);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case RECORDS:
                id = database.insertWithOnConflict(basePath(), null, values, SQLiteDatabase.CONFLICT_REPLACE);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(basePath() + "/" + id);
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if ( uriType(uri) == RECORD_WITH_INFO) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

            /**
             * check if the caller has requested a column which does not exists
             */
            checkColumns(projection);

            queryBuilder.setTables(MessagesTable.TABLE_NAME + " LEFT OUTER JOIN " +
                    ContactsTable.TABLE_NAME + " ON " +
            MessagesTable.TABLE_NAME + "." + MessagesTable.COLUMN_MESSAGES_FROM_ID + " = " +
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
        return TABLE_NAME;
    }

    @Override
    protected void checkColumns(String[] projection) {
        // for now, just return, without any erro.
        return ;

//        String[] available = {
//
//        };
//        if (projection != null) {
//            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
//            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
//
//            if (!availableColumns.containsAll(requestedColumns)) {
//                throw new IllegalArgumentException("Unknown columns in projection");
//            }
//        }
    }

    @Override
    protected String authority() {
        return AUTHORITY;
    }


    @Override
    protected void notifyUris(Uri uri) {
        super.notifyUris(uri);
        getContext().getContentResolver().notifyChange(CONTENT_URI_WITH_CONTACTS_INFO, null);
    }
}
