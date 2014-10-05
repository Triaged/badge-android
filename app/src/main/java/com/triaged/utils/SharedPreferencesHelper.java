package com.triaged.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;


/**
 * Helper class which provides easy access to shared preferences.
 * 
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class SharedPreferencesHelper {

    private static SharedPreferencesHelper mInstance;

    private static Context mContext;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public static void prepare(Context context) {
        mContext = context;
    }

    public static SharedPreferencesHelper instance() {
        if (mContext == null) {
            throw new IllegalStateException("Please call prepare() method first.");
        }
        if (mInstance == null) {
            synchronized (SharedPreferencesHelper.class) {
                if (mInstance == null) {
                    mInstance = new SharedPreferencesHelper();
                }
            }
        }
        return mInstance;
    }

    private SharedPreferencesHelper() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSharedPreferences.edit();
    }

    public SharedPreferencesHelper putBoolean(String key, boolean value) {
        mEditor.putBoolean(key, value);
        return this;
    }

    public SharedPreferencesHelper putInt(String key, int value) {
        mEditor.putInt(key, value);
        return this;
    }

    public SharedPreferencesHelper putFloat(String key, float value) {
        mEditor.putFloat(key, value);
        return this;
    }

    public SharedPreferencesHelper putLong(String key, long value) {
        mEditor.putLong(key, value);
        return this;
    }

    public SharedPreferencesHelper putString(String key, String value) {
        mEditor.putString(key, value);
        return this;
    }

    public SharedPreferencesHelper putStringSet(String key, Set<String> values) {
        mEditor.putStringSet(key, values);
        return this;
    }

    public SharedPreferencesHelper putBoolean(int keyResourceId, boolean value) {
        putBoolean(mContext.getString(keyResourceId), value);
        return this;
    }

    public SharedPreferencesHelper putInt(int keyResourceId, int value) {
        putInt(mContext.getString(keyResourceId), value);
        return this;
    }

    public SharedPreferencesHelper putFloat(int keyResourceId, float value) {
        putFloat(mContext.getString(keyResourceId), value);
        return this;
    }

    public SharedPreferencesHelper putLong(int keyResourceId, long value) {
        putLong(mContext.getString(keyResourceId), value);
        return this;
    }

    public SharedPreferencesHelper putString(int keyResourceId, String value) {
        putString(mContext.getString(keyResourceId), value);
        return this;
    }

    public SharedPreferencesHelper putStringSet(int keyResourceId, Set<String> values) {
        putStringSet(mContext.getString(keyResourceId), values);
        return this;
    }

    public boolean commit() {
        return mEditor.commit();
    }

    public void apply() {
        mEditor.apply();
    }


    public boolean getBoolean(String key, boolean defaultValue) {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return mSharedPreferences.getString(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return mSharedPreferences.getFloat(key, defaultValue);
    }

    public int getInteger(String key, int defaultValue) {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return mSharedPreferences.getLong(key, defaultValue);
    }

    public boolean getBoolean(int keyResourceId, boolean defaultValue) {
        return getBoolean(mContext.getString(keyResourceId), defaultValue);
    }

    public String getString(int keyResourceId, String defaultValue) {
        return getString(mContext.getString(keyResourceId), defaultValue);
    }

    public float getFloat(int keyResourceId, float defaultValue) {
        return getFloat(mContext.getString(keyResourceId), defaultValue);
    }

    public int getInteger(int keyResourceId, int defaultValue) {
        return getInteger(mContext.getString(keyResourceId), defaultValue);
    }

    public long getLong(int keyResourceId, long defaultValue) {
        return getLong(mContext.getString(keyResourceId), defaultValue);
    }

    public Boolean clearSharedPref() {
        return mSharedPreferences.edit().clear().commit();
    }
}
