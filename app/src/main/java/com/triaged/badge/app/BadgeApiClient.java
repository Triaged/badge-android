package com.triaged.badge.app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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
    public static final String MIME_TYPE_JSON = "application/json";

    protected static final String CLEAR_DEPARTMENTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_DEPARTMENTS );
    protected static final String CLEAR_CONTACTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_CONTACTS );

    protected static final String LOG_TAG = BadgeApiClient.class.getName();
    private static final String API_HOST = "api.badge.co";



    private HttpHost httpHost;
    String apiToken;

    public BadgeApiClient( String apiToken ) {
        super();
        httpHost = new HttpHost( API_HOST );
        this.apiToken = apiToken;
    }

    /**
     * This method connects to the api and requests the company json representation.
     * If it is successfully retrieved, that company's data is persisted to the local
     * sql lite store.
     *
     * @param db The sqlite database to write the data to.
     * @param lastSynced It should request only contacts at that company modified since this time in milliseconds
     * @return true if sync successful, false if credentials not honored.
     * @throws IOException if network issues prevent syncing
     */
    public boolean downloadCompany( SQLiteDatabase db, long lastSynced ) throws IOException, JSONException {
        HttpGet getCompany = new HttpGet( String.format( "https://%s/v1/company", API_HOST ) );
        getCompany.setHeader( "Authorization", apiToken );
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

                HashMap<Integer, String> departmentMap = new HashMap<Integer, String>( 50 );
                if( companyObj.has( "uses_departments" ) && companyObj.getBoolean( "uses_departments" ) ) {
                    JSONArray deptsArr = companyObj.getJSONArray( "departments" );
                    for( int i = 0; i < deptsArr.length(); i++ ) {
                        JSONObject dept = deptsArr.getJSONObject( i );
                        departmentMap.put( dept.getInt( "id" ), dept.getString( "name" ) );
                    }
                }

                JSONArray contactsArr = companyObj.getJSONArray( "users" );
                for( int i = 0; i < contactsArr.length(); i++ ) {
                    JSONObject newContact = contactsArr.getJSONObject(i);
                    values.put( CompanySQLiteHelper.COLUMN_CONTACT_ID, newContact.getInt( "id" ) );
                    if(newContact.has( "last_name" ) ) {
                        values.put(CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME, newContact.getString("last_name"));
                    }
                    if( newContact.has( "first_name" ) ) {
                        values.put(CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME, newContact.getString("first_name"));
                    }
                    if( newContact.has( "avatar_face_url") ) {
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_AVATAR_URL, newContact.getString( "avatar_face_url" ) );
                    }
                    if( newContact.has( "email" ) ) {
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_EMAIL, newContact.getString( "email" ) );
                    }
                    if( newContact.has( "manager_id" ) && !newContact.get("manager_id").equals("") ) {
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID, newContact.getInt( "manager_id" ) );
                    }
                    if( newContact.has( "primary_office_location_id" ) && !newContact.get("primary_office_location_id").equals("") ) {
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, newContact.getInt( "primary_office_location_id" ) );
                    }
                    if( newContact.has( "current_office_location_id" ) && !newContact.get("current_office_location_id").equals("") ) {
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, newContact.getInt( "current_office_location_id" ) );
                    }
                    if( newContact.has( "department_id" ) && !newContact.get("department_id").equals("") ) {
                        int departmentId = newContact.getInt( "department_id" );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID, departmentId );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_NAME, departmentMap.get( departmentId ) );
                    }
                    if( newContact.has( "sharing_office_location" ) && !newContact.isNull("sharing_office_location") ) {
                        boolean isSharing = newContact.getBoolean( "sharing_office_location" );
                        int sharingInt = isSharing ? 1 : 0;
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, sharingInt );
                    }
                    if( newContact.has("employee_info") ) {
                        JSONObject employeeInfo = newContact.getJSONObject("employee_info");
                        if ( employeeInfo.has( "job_title" ) && !employeeInfo.isNull("job_title") ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE, employeeInfo.getString( "job_title" ) );
                        }
                        if ( employeeInfo.has( "start_date" ) && !employeeInfo.isNull("start_date") ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_START_DATE, employeeInfo.getString( "start_date" ) );
                        }
                        if ( employeeInfo.has( "birth_date" ) && !employeeInfo.isNull("birth_date") ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthdayString( employeeInfo.getString( "birth_date" ) ) );
                        }
                        if ( employeeInfo.has( "cell_phone" ) && !employeeInfo.isNull("cell_phone") ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_CELL_PHONE, employeeInfo.getString( "cell_phone" ) );
                        }
                        if ( employeeInfo.has( "office_phone" ) && !employeeInfo.isNull("office_phone") ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_OFFICE_PHONE, employeeInfo.getString( "office_phone" ) );
                        }
                    }
                    db.insert( CompanySQLiteHelper.TABLE_CONTACTS, "", values );
                    values.clear();
                }
                return true;
            }
            else if( statusCode == HttpStatus.SC_UNAUTHORIZED ) {
                // Wipe DB, we're locked/logged out
                db.execSQL( CLEAR_CONTACTS_SQL );
                db.execSQL( CLEAR_DEPARTMENTS_SQL );
            }
            else {
                Log.e( LOG_TAG, "Got status " + statusCode + " from API. Handle this appropriately!" );
            }
            return false;
        }
        finally {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        }

    }

    /**
     * Make an api request to create a new session using email/pass creds
     *
     * The caller should make sure that it consumes all the entity content
     * and closes the stream for the response.
     *
     * @param email
     * @param password
     */
    public HttpResponse executeLogin( String email, String password ) throws IOException {
        HttpPost createSession = new HttpPost( String.format( "https://%s/v1/sessions", API_HOST ) );
        JSONObject req = new JSONObject();
        JSONObject creds = new JSONObject();
        try {
            creds.put( "email", email );
            creds.put( "password", password );
            req.put( "user_login", creds );
        }
        catch( JSONException e ) {
            Log.e( LOG_TAG, "Unexpected json exception creating /session post entity.", e );
        }
        StringEntity body = new StringEntity( req.toString(), "UTF-8" );
        body.setContentType( MIME_TYPE_JSON );
        createSession.setEntity( body );
        return execute( httpHost, createSession );
    }

    public HttpResponse executeAccountPatch( JSONObject data ) throws IOException {
        HttpEntityEnclosingRequestBase patch = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return "PATCH";
            }
        };
        try {
            patch.setURI(new URI(String.format("https://%s/v1/account", API_HOST)));
            StringEntity body = new StringEntity( data.toString(), "UTF-8" );
            body.setContentType( MIME_TYPE_JSON );
            patch.setEntity( body );
            patch.setHeader( "Authorization", apiToken );
            return execute( httpHost, patch );
        }
        catch( URISyntaxException e ) {
            throw new IllegalStateException( "Couldn't parse what should be a constant URL, bombing out.", e );
        }
        catch( UnsupportedEncodingException e ) {
            throw new IllegalStateException( "Unsupported encoding UTF-8. WTF?", e );
        }
    }

    public void shutdown() {
        getConnectionManager().shutdown();
    }
}
