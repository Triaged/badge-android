package com.triaged.badge.database.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.triaged.badge.database.table.MessagesTable;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class MessageProvider extends AbstractProvider {
    public MessageProvider() { }

    static final String AUTHORITY = "com.triaged.badge.provider.messages";
    static final String TABLE_NAME = MessagesTable.TABLE_NAME;
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
}
