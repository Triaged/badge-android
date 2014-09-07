package com.triaged.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.triaged.badge.app.App;


/**
 * Util class which provides easy access to shared preferences.
 * 
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class SharedPreferencesUtil {

    private static final Context CONTEXT = App.context();
    private static SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CONTEXT);

    public static boolean store(String key, Object value) {
        if (value instanceof Boolean) {
            return sharedPreferences.edit().putBoolean(key, (Boolean) value).commit();
        } else if (value instanceof Integer) {
            return sharedPreferences.edit().putInt(key, (Integer) value).commit();

        } else if (value instanceof Float) {
            return sharedPreferences.edit().putFloat(key, (Float) value).commit();
        } else if (value instanceof String) {
            return sharedPreferences.edit().putString(key, (String) value).commit();
        } else if (value instanceof Long) {
            return sharedPreferences.edit().putLong(key, (Long) value).commit();
        }
        return false;
    }

    public static boolean store(int keyResourceId, Object value) {
        return store(CONTEXT.getString(keyResourceId), value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public static int getInteger(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public static boolean getBoolean(int keyResourceId, boolean defaultValue) {
        return getBoolean(CONTEXT.getString(keyResourceId), defaultValue);
    }

    public static String getString(int keyResourceId, String defaultValue) {
        return getString(CONTEXT.getString(keyResourceId), defaultValue);
    }

    public static float getFloat(int keyResourceId, float defaultValue) {
        return getFloat(CONTEXT.getString(keyResourceId), defaultValue);
    }

    public static int getInteger(int keyResourceId, int defaultValue) {
        return getInteger(CONTEXT.getString(keyResourceId), defaultValue);
    }

    public static long getLong(int keyResourceId, long defaultValue) {
        return getLong(CONTEXT.getString(keyResourceId), defaultValue);
    }

    public static Boolean clearSharedPref() {
        return sharedPreferences.edit().clear().commit();
    }
}
