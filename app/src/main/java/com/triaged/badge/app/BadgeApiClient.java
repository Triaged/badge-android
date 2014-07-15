package com.triaged.badge.app;

import android.util.Log;

import com.triaged.badge.data.CompanySQLiteHelper;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

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
     * @param lastSynced It should request only contacts at that company modified since this time in milliseconds
     * @throws IOException if network issues occur during the process.
     */
    public HttpResponse downloadCompany( long lastSynced ) throws IOException, JSONException {
        HttpGet getCompany = new HttpGet( String.format( "https://%s/v1/company", API_HOST ) );
        getCompany.setHeader( "Authorization", apiToken );
        return execute( httpHost, getCompany );
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
