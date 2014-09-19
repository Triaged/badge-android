package com.triaged.utils;

import android.database.Cursor;
import android.util.Log;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class DatabaseUtils {

    public static void logCursor(Cursor c, String tag) {
        while (c.moveToNext()) {
            Log.d(tag, "-------------------------------------------------------------------");
            for (String column : c.getColumnNames()) {
                Log.d(tag, String.format("%s: %s", column, c.getString(c.getColumnIndex(column))));
            }
            Log.d(tag, "-------------------------------------------------------------------");
        }
    }
}
