package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.triaged.badge.database.table.MessageThreadsTable;

/**
 * Created by Sadegh Kazemy on 9/29/14.
 */
public class ThreadProvider extends AbstractProvider {

    public ThreadProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.threads";
    static final String TABLE_THREAD = MessageThreadsTable.TABLE_NAME;

    static final String URI = "content://" + AUTHORITY + "/" + TABLE_THREAD;

    public static final Uri CONTENT_URI = Uri.parse(URI);

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_THREAD, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_THREAD + "/#", RECORD_ID);
    }

    @Override
    protected int uriType(Uri uri) {
        return uriMatcher.match(uri);
    }

    @Override
    protected String basePath() {
        return TABLE_THREAD;
    }

    @Override
    protected String authority() {
        return AUTHORITY;
    }

    @Override
    protected void checkColumns(String[] projection) {

    }
}
