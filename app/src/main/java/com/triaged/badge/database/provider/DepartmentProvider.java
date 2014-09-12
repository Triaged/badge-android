package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.triaged.badge.database.table.DepartmentsTable;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentProvider extends AbstractProvider {
    public DepartmentProvider() { }

    public static final String AUTHORITY = "com.triaged.badge.provider.departments";
    static final String TABLE_NAME = DepartmentsTable.TABLE_NAME;
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
