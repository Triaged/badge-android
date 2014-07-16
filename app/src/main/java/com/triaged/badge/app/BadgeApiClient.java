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
     * Make a GET /company request.
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param lastSynced It should request only contacts at that company modified since this time in milliseconds
     * @throws IOException if network issues occur during the process.
     */
    public HttpResponse downloadCompanyRequest(long lastSynced) throws IOException, JSONException {
        HttpGet getCompany = new HttpGet( String.format( "https://%s/v1/company", API_HOST ) );
        getCompany.setHeader( "Authorization", apiToken );
        return execute( httpHost, getCompany );
    }

    /**
     * Make an api POST request to /sessions to log in.
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param postData json object of form { "user_login": { "email": "foo@blah.com", "password" : "not4u" } }
     */
    public HttpResponse createSessionRequest( JSONObject postData ) throws IOException {
        HttpPost createSession = new HttpPost( String.format( "https://%s/v1/sessions", API_HOST ) );
        StringEntity body = new StringEntity( postData.toString(), "UTF-8" );
        body.setContentType( MIME_TYPE_JSON );
        createSession.setEntity( body );
        return execute( httpHost, createSession );
    }

    /**
     * Make an api PATCH /account request.
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param data patch json in the form of { "user" : { "first_name" : "Joe", "employee_info_attributes" :  { "job_title" : "Head Honcho" } } } , should only contain information you want to update
     * @return http response.
     * @throws IOException
     */
    public HttpResponse patchAccountRequest(JSONObject data) throws IOException {
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
    }

    /**
     * Make a POST /departments request
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.

     * @param department json object of form { "department" : { "name" : "New Dept" } }
     * @return
     * @throws IOException
     */
    public HttpResponse createDepartmentRequest( JSONObject department ) throws IOException {
        HttpPost createSession = new HttpPost( String.format( "https://%s/v1/departments", API_HOST ) );
        StringEntity body = new StringEntity( department.toString(), "UTF-8" );
        body.setContentType( MIME_TYPE_JSON );
        createSession.setEntity( body );
        createSession.setHeader( "Authorization", apiToken );
        return execute( httpHost, createSession );
    }
}
