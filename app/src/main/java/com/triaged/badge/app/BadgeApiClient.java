package com.triaged.badge.app;

import android.database.sqlite.SQLiteDatabase;

import com.triaged.badge.data.Contact;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.List;

/**
 * All requests to the badge api should be made through this client.
 * It represents a bridge between the device and the cloud data.
 *
 * @author Created by jc on 7/7/14.
 */
public class BadgeApiClient extends DefaultHttpClient {
    private static final String API_HOST = "api.badge.co";

    private HttpHost httpHost;

    public BadgeApiClient() {
        super();
        httpHost = new HttpHost( API_HOST );
    }

    public void downloadCompany( SQLiteDatabase db, long lastSynced ) throws IOException {
        // TODO for now ignore last Synced and just do it
        HttpGet getCompany = new HttpGet( String.format( "https://%s/v1/company", API_HOST ) );
        getCompany.setHeader( "Authorization", "8ekayof3x1P5kE_LvPFv" );
        HttpResponse response = execute( httpHost, getCompany );
        int statusCode = response.getStatusLine().getStatusCode();
        switch( statusCode ) {

        }
    }

    public void shutdown() {
        getConnectionManager().shutdown();
    }
}
