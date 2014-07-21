package com.triaged.badge.app;

import android.util.Log;

import com.triaged.badge.data.CompanySQLiteHelper;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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

    private static final String API_PROTOCOL = "http";
    private static final String PROD_API_HOST = "api.badge.co";
    private static final String STAGING_API_HOST = "api.badge-staging.com";
    private static final String API_HOST = STAGING_API_HOST;

    private static final String PATCH_ACCOUNT_URI = String.format("%s://%s/v1/account", API_PROTOCOL, API_HOST);
    private static final String GET_COMPANY_URI = String.format( "%s://%s/v1/company", API_PROTOCOL, API_HOST );
    private static final String CREATE_SESSION_URI = String.format( "%s://%s/v1/sessions", API_PROTOCOL, API_HOST );
    private static final String CREATE_DEPARTMENT_URI = String.format( "%s://%s/v1/departments", API_PROTOCOL, API_HOST );
    private static final String CREATE_OFFICE_LOCATION_URI = String.format( "%s://%s/v1/office_locations", API_PROTOCOL, API_HOST );
    private static final String REGISTER_DEVICE_URI = String.format( "%s://%s/v1/devices", API_PROTOCOL, API_HOST );
    private static final String DELETE_DEVICE_URI_PATTERN = "%s://%s/v1/devices/%d/sign_out";
    private static final String ENTER_OFFICE_URI_PATTERN = "%s://%s/v1/office_locations/%d/entered";
    private static final String EXIT_OFFICE_URI_PATTERN = "%s://%s/v1/office_locations/%d/exited";

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
        HttpGet getCompany = new HttpGet( GET_COMPANY_URI );
        getCompany.setHeader( "Authorization", apiToken );
        return execute( httpHost, getCompany );
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
            patch.setURI(new URI(PATCH_ACCOUNT_URI));
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
     * Uploads avatar data in a multipart form request.
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param base64Png image data as base 64 encoded png
     * @return
     * @throws IOException
     */
    public HttpResponse uploadNewAvatar( String base64Png ) throws IOException {
       //Builder
        // MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
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
        return postHelper(postData, CREATE_SESSION_URI);
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
        return postHelper(department, CREATE_DEPARTMENT_URI);
    }

    /**
     * Make a POST /office_locations request
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param location json object of form { "office_location" : {  "street_address" : "394 Broadway", "city" : "New York", ... } }
     * @return
     * @throws IOException
     */
    public HttpResponse createLocationRequest( JSONObject location ) throws IOException {
        return postHelper( location, CREATE_OFFICE_LOCATION_URI );
    }

    /**
     * Make a POST /devices request
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param device { "device" : { "token": "xxx", "": "os_version": 18, "service": "android" } }
     * @return
     * @throws IOException
     */
    public HttpResponse registerDeviceRequest( JSONObject device ) throws IOException {
        return postHelper(device, REGISTER_DEVICE_URI);
    }

    /**
     * Make a DELETE /devices/:id/sign_out request
     *
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param deviceId device id returned from POST /devices previously
     * @return
     * @throws IOException
     */
    public HttpResponse unregisterDeviceRequest( int deviceId ) throws IOException {
        HttpDelete delete = new HttpDelete( String.format( DELETE_DEVICE_URI_PATTERN, API_PROTOCOL, API_HOST, deviceId ) );
        delete.setHeader("Authorization", apiToken);
        return execute(httpHost, delete);
    }

    /**
     * PUTS to /offices/:id/entered to check in to an office
     *
     * @param officeId office to check in to
     * @return
     */
    public HttpResponse checkinRequest(int officeId) throws IOException {
        HttpPut put = new HttpPut( String.format( ENTER_OFFICE_URI_PATTERN, API_PROTOCOL, API_HOST, officeId ) );
        put.setHeader( "Authorization", apiToken );
        return execute( httpHost, put );
    }

    /**
     * PUTS to /offices/:id/exited to check out of an office
     *
     * @param officeId office to check out of
     * @return
     */
    public HttpResponse checkoutRequest(int officeId) throws IOException {
        HttpPut put = new HttpPut( String.format( EXIT_OFFICE_URI_PATTERN, API_PROTOCOL, API_HOST, officeId ) );
        put.setHeader( "Authorization", apiToken );
        return execute( httpHost, put );
    }





    private HttpResponse postHelper( JSONObject postData, String uri ) throws IOException {
        HttpPost post = new HttpPost( uri );
        StringEntity body = new StringEntity( postData.toString(), "UTF-8" );
        body.setContentType( MIME_TYPE_JSON );
        post.setEntity( body );
        if( apiToken != null && !apiToken.isEmpty() ) {
            post.setHeader("Authorization", apiToken);
        }
        return execute( httpHost, post );
    }
}
