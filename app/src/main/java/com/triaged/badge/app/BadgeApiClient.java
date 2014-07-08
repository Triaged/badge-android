package com.triaged.badge.app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.triaged.badge.data.Contact;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * All requests to the badge api should be made through this client.
 * It represents a bridge between the device and the cloud data.
 *
 * This client reuses a read buffer and thus is not thread safe.
 *
 * @author Created by jc on 7/7/14.
 */
public class BadgeApiClient extends DefaultHttpClient {
    private static final String LOG_TAG = BadgeApiClient.class.getName();
    private static final String API_HOST = "api.badge.co";

    private HttpHost httpHost;

    public BadgeApiClient() {
        super();
        httpHost = new HttpHost( API_HOST );


    }

    /**
     * This method connects to the api and requests the company json representation.
     * If it is successfully retrieved, that company's data is persisted to the local
     * sql lite store.
     *
     * @param db The sqlite database to write the data to.
     * @param lastSynced It should request only contacts at that company modified since this time in milliseconds
     * @throws IOException
     */
    public void downloadCompany( SQLiteDatabase db, long lastSynced ) throws IOException, JSONException {
        HttpGet getCompany = new HttpGet( String.format( "https://%s/v1/company", API_HOST ) );
        getCompany.setHeader( "Authorization", "8ekayof3x1P5kE_LvPFv" );
        HttpResponse response = execute( httpHost, getCompany );

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream( 256 * 1024 /* 256 k */ );
                response.getEntity().writeTo( jsonBuffer );
                JSONArray companyArr = new JSONArray( jsonBuffer.toString( "UTF-8" ) );
                JSONObject companyObj = companyArr.getJSONObject(0);
                // Allow immediate GC
                jsonBuffer = null;
                ContentValues values = new ContentValues();
                JSONArray contactsArr = companyObj.getJSONArray( "users" );
                for( int i = 0; i < contactsArr.length(); i++ ) {
                    JSONObject newContact = contactsArr.getJSONObject(i);
                    values.put( DataProviderService.COLUMN_CONTACT_ID, newContact.getInt( "id" ) );
                    if(newContact.has( "last_name" ) ) {
                        values.put(DataProviderService.COLUMN_CONTACT_LAST_NAME, newContact.getString("last_name"));
                    }
                    if( newContact.has( "first_name" ) ) {
                        values.put(DataProviderService.COLUMN_CONTACT_FIRST_NAME, newContact.getString("first_name"));
                    }
                    if( newContact.has( "avatar_face_url") ) {
                        values.put( DataProviderService.COLUMN_CONTACT_AVATAR_URL, newContact.getString( "avatar_face_url" ) );
                    }
                    db.insert( DataProviderService.TABLE_CONTACTS, "", values );
                    values.clear();
                }
            }
            else {
                Log.e( LOG_TAG, "Got status " + statusCode + " from API. Handle this appropriately!" );
            }
        }
        finally {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        }

    }

    public void shutdown() {
        getConnectionManager().shutdown();
    }
}
