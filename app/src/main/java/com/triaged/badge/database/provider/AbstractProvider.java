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
     * {@inheritDoc}
     */
    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return (databaseHelper.getWritableDatabase() != null);
    }

    /**
     * {@inheritDoc}
     */
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
        notifyUris(uri);
        return Uri.parse(basePath() + "/" + id);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        /**
         * check if the caller has requested a column which does not exists
         */
        checkColumns(projection);

        queryBuilder.setTables(basePath());

        Cursor cursor;
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        int uriType = uriType(uri);
        switch (uriType) {

            case RECORDS:
                cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case RECORD_ID:
                /**
                 * adding the ID to the original query
                 */
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder, "1");
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
