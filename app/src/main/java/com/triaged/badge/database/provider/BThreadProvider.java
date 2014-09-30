package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.triaged.badge.database.table.BThreadsTable;

/**
 * Created by Sadegh Kazemy on 9/29/14.
 */
public class BThreadProvider extends AbstractProvider {

    public BThreadProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.bthreads";
    static final String TABLE_BTHREAD = BThreadsTable.TABLE_NAME;

    static final String URI = "content://" + AUTHORITY + "/" + TABLE_BTHREAD;

    public static final Uri CONTENT_URI = Uri.parse(URI);

    /**
     * maps content URI "patterns" to the integer values that were set above
     */
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, TABLE_BTHREAD, RECORDS);
        uriMatcher.addURI(AUTHORITY, TABLE_BTHREAD + "/#", RECORD_ID);
    }

    @Override
    protected int uriType(Uri uri) {
        return uriMatcher.match(uri);
    }

    @Override
    protected String basePath() {
        return TABLE_BTHREAD;
    }

    @Override
    protected String authority() {
        return AUTHORITY;
    }

    @Override
    protected void checkColumns(String[] projection) {

    }
}
