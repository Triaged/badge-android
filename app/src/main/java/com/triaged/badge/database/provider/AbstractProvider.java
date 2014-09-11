package com.triaged.badge.database.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.triaged.badge.database.DatabaseHelper;


/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */

public abstract class AbstractProvider extends ContentProvider {
    public AbstractProvider() { }

    /**
     * integer values used in content URI
     */
    static final int RECORDS = 1;
    static final int RECORD_ID = 2;

    protected DatabaseHelper databaseHelper;


    /**
     * Initialize your content provider on startup.
     * This method is called for all registered content providers on the
     * application main thread at application launch time.  It must not perform
     * lengthy operations, or application startup will be delayed.
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return (databaseHelper.getWritableDatabase() != null);
    }

    /**
     * Handle requests to insert a new row.
     *
     * @param uri The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     *    This must not be null.
     * @return The number of values that were inserted.
     */
    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        int uriType = uriType(uri);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case RECORDS:
                id = database.insert(basePath(), null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyUris(uri);
        return Uri.parse(basePath() + "/" + id);
    }


    /**
     * Handle query requests from clients.
     * This method can be called from multiple threads.
     *
     * @param uri The URI to query. This will be the full URI sent by the client;
     *      if the client is requesting a specific record, the URI will end in a record number
     *      that the implementation should parse and add to a WHERE or HAVING clause, specifying
     *      that _id value.
     * @param projection The list of columns to put into the cursor. If
     *      null all columns are included.
     * @param selection A selection criteria to apply when filtering rows.
     *      If null then all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *      the values from selectionArgs, in order that they appear in the selection.
     *      The values will be bound as Strings.
     * @param sortOrder How the rows in the cursor should be sorted.
     *      If null then the provider is free to define the sort order.
     * @return a Cursor or null.
     */
    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        /**
         * check if the caller has requested a column which does not exists
         */
        checkColumns(projection);

        queryBuilder.setTables(basePath());

        int uriType = uriType(uri);
        switch (uriType) {

            case RECORDS:
                break;

            case RECORD_ID:
                /**
                 * adding the ID to the original query
                 */
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    /**
     * Handle requests to update one or more rows.
     * Update all rows matching the selection
     *
     * @param uri The URI to query. This can potentially have a record ID if this
     * is an update request for a specific record.
     * @param values A set of column_name/value pairs to update in the database.
     *     This must not be null.
     * @param selection An optional filter to match rows to update.
     * @return the number of rows affected.
     */
    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriType(uri);
        int updatedRows;
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        switch (uriType) {
            case RECORDS:
                updatedRows = database.update(basePath(),
                        values,
                        selection,
                        selectionArgs);
                break;
            case RECORD_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    updatedRows = database.update(basePath(),
                            values,
                            RECORD_ID + " =?", new String[]{id});
                } else {
                    updatedRows = database.update(basePath(),
                            values,
                            RECORD_ID + "=" + id +
                                    " and " + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknow URI: " + uri);
        }
        notifyUris(uri);

        return updatedRows;
    }


    /**
     * Handle requests to delete one or more rows.
     * The implementation should apply the selection clause when performing
     * deletion, allowing the operation to affect multiple rows in a directory.
     *
     * @param uri The full URI to query, including a row ID (if a specific record is requested).
     * @param selection An optional restriction to apply to rows when deleting.
     * @return The number of rows affected.
     * @throws android.database.SQLException
     */
    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriType(uri);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriType) {
            case RECORDS:
                rowsDeleted = database.delete(basePath(), selection, selectionArgs);
                break;

            case RECORD_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = database.delete(basePath(),
                           "_id=" + id, null);
                } else {
                    rowsDeleted = database.delete(basePath(),
                            "_id=" + id +
                                    " and " + selection, selectionArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyUris(uri);

        return rowsDeleted;
    }

    /**
     * handle requests for the MIME type of the data at the given URI
     * @param uri the URI to query.
     * @return a MIME type string, or null if there is no type.
     */
    @Override
    public String getType(Uri uri) {

        String result;
        switch (uriType(uri)) {
            case RECORDS:
                result = String.format("vnd.android.cursor.dir/vnd.%s.%s", authority(), basePath() );
                break;

            case RECORD_ID:
                result = String.format("vnd.android.cursor.item/vnd.%s.%s", authority(), basePath() );
                break;

            default:
                return null;
        }
        return result;
    }

    protected void notifyUris(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }


    protected abstract int uriType(Uri uri);

    /**
     * @return The base path which is the corresponding table name.
     */
    protected abstract String basePath();

    protected abstract String authority();

    /**
     * Check if all columns which are requested are available.
     * @param projection the list of column names that is requested by client.
     * @throws IllegalArgumentException if corresponding table does not have any of requested columns.
     * @throws IllegalArgumentException if corresponding tableru does not have any of requested columns.
     */
    protected abstract void checkColumns(String[] projection);

}
