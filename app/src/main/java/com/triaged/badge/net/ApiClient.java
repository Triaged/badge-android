package com.triaged.badge.net;

import com.triaged.badge.app.App;
import com.triaged.badge.app.BuildConfig;
import com.triaged.utils.GeneralUtils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * All requests to the badge api should be made through this client.
 * It represents a bridge between the device and the cloud data.
 * <p/>
 * This client reuses a read buffer and thus is not thread safe.
 *
 * @author Created by jc on 7/7/14.
 */
public class ApiClient extends DefaultHttpClient {
    public static final String MIME_TYPE_JSON = "application/json";

    public static final String API_MESSAGING_HOST = BuildConfig.API_MESSAGING_SERVER_URL;
    private static final String API_HOST = BuildConfig.API_URL;

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final String PATCH_ACCOUNT_URI = String.format("%s/v1/account", API_HOST);
    private static final String POST_AVATAR_URI = String.format("%s/v1/account/avatar", API_HOST);
    private static final String GET_COMPANY_URI_PATTERN = "%s/v1/company?timestamp=%d";
    private static final String GET_MSG_HISTORY_URI_FORMAT = "%s/api/v1/user/messages?timestamp=%d";
    private static final String CREATE_SESSION_URI = String.format("%s/v1/sessions", API_HOST);
    private static final String CREATE_DEPARTMENT_URI = String.format("%s/v1/departments", API_HOST);
    private static final String CREATE_THREAD_URI = String.format("%s/api/v1/message_threads", API_MESSAGING_HOST);
    private static final String CREATE_OFFICE_LOCATION_URI = String.format("%s/v1/office_locations", API_HOST);
    private static final String REGISTER_DEVICE_URI = String.format("%s/v1/devices", API_HOST);
    private static final String CHANGE_PASSWORD_URI = String.format("%s/v1/account/update_password", API_HOST);
    private static final String DELETE_DEVICE_URI_PATTERN = "%s/v1/devices/%d/sign_out";
    private static final String ENTER_OFFICE_URI_PATTERN = "%s/v1/office_locations/%d/entered";
    private static final String EXIT_OFFICE_URI_PATTERN = "%s/v1/office_locations/%d/exited";
    private static final String GET_CONTACT_URI_PATTERN = "%s/v1/users/%d";

    private static final String GET_OFFICE_URI_PATTERN = "%s/v1/office_locations/%d";
    private static final String GET_DEPARTMENT_URI_PATTERN = "%s/v1/departments/%d";

    private static final String REQUEST_RESET_PASSWORD_URI = String.format("%s/v1/account/reset_password", API_HOST);

    public static final String USER_AGENT = "Badge-android/" + GeneralUtils.getAppVersionName(App.context());

    private HttpHost httpHost;
    private HttpHost messagingHttpHost;
    String apiToken;

    public ApiClient(String apiToken) {
        super();
        this.getParams().setParameter( CoreProtocolPNames.USER_AGENT, USER_AGENT);

        URI uri = URI.create(String.format("%s", API_HOST));
        httpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        uri = URI.create(String.format("%s", API_MESSAGING_HOST));
        messagingHttpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        this.apiToken = apiToken;
    }

    /**
     * Make a GET /company request.
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param lastSynced It should request only contacts at that company modified since this time in milliseconds
     * @throws IOException if network issues occur during the process.
     */
    public HttpResponse downloadCompanyRequest(long lastSynced) throws IOException {
        HttpGet getCompany = new HttpGet(String.format(GET_COMPANY_URI_PATTERN, API_HOST, lastSynced));
        getCompany.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);

        return execute(httpHost, getCompany);
    }

    /**
     * Make an api PATCH /account request.
     * <p/>
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
            StringEntity body = new StringEntity(data.toString(), "UTF-8");
            body.setContentType(MIME_TYPE_JSON);
            patch.setEntity(body);
            patch.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
            return execute(httpHost, patch);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Couldn't parse what should be a constant URL, bombing out.", e);
        }
    }

    /**
     * Uploads avatar data in a multipart form request as  POST to /account
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param png image data in memory
     * @return
     * @throws IOException
     */
    public HttpResponse uploadNewAvatar(byte[] png) throws IOException {
        //Builder
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        ByteArrayBody imgBody = new ByteArrayBody(png, "image/png", "avatar.png");
        reqEntity.addPart("user[avatar]", imgBody);
        HttpPost post = new HttpPost(POST_AVATAR_URI);
        post.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        post.setEntity(reqEntity);
        return execute(post);
    }

    /**
     * Make an api POST request to /sessions to log in.
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param postData json object of form { "user_login": { "email": "foo@blah.com", "password" : "not4u" } }
     */
    public HttpResponse createSessionRequest(JSONObject postData) throws IOException {
        return postHelper(postData, CREATE_SESSION_URI);
    }

    /**
     * Make a POST /departments request
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param department json object of form { "department" : { "name" : "New Dept" } }
     * @return
     * @throws IOException
     */
    public HttpResponse createDepartmentRequest(JSONObject department) throws IOException {
        return postHelper(department, CREATE_DEPARTMENT_URI);
    }

    /**
     * Make a POST /office_locations request
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param location json object of form { "office_location" : {  "street_address" : "394 Broadway", "city" : "New York", ... } }
     * @return
     * @throws IOException
     */
    public HttpResponse createLocationRequest(JSONObject location) throws IOException {
        return postHelper(location, CREATE_OFFICE_LOCATION_URI);
    }

    /**
     * Make a POST /devices request
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param device { "device" : { "token": "xxx", "": "os_version": 18, "service": "android" } }
     * @return
     * @throws IOException
     */
    public HttpResponse registerDeviceRequest(JSONObject device) throws IOException {
        return postHelper(device, REGISTER_DEVICE_URI);
    }

    /**
     * Make a DELETE /devices/:id/sign_out request
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param deviceId device id returned from POST /devices previously
     * @return
     * @throws IOException
     */
    public HttpResponse unregisterDeviceRequest(int deviceId) throws IOException {
        HttpDelete delete = new HttpDelete(String.format(DELETE_DEVICE_URI_PATTERN, API_HOST, deviceId));
        delete.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(httpHost, delete);
    }


    /**
     * PUTS to /offices/:id/entered to check in to an office
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param officeId office to check in to
     * @return
     */
    public HttpResponse checkinRequest(int officeId) throws IOException {
        HttpPut put = new HttpPut(String.format(ENTER_OFFICE_URI_PATTERN, API_HOST, officeId));
        put.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(httpHost, put);
    }

    /**
     * PUTS to /offices/:id/exited to check out of an office
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param officeId office to check out of
     * @return
     */
    public HttpResponse checkoutRequest(int officeId) throws IOException {
        HttpPut put = new HttpPut(String.format(EXIT_OFFICE_URI_PATTERN, API_HOST, officeId));
        put.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(httpHost, put);
    }

    /**
     * PUTS to /account/update_password
     * <p/>
     * The caller should make sure that it consumes all the entity content
     * and/or closes the stream for the response.
     *
     * @param postBody
     * @return
     */
    public HttpResponse changePasswordRequest(JSONObject postBody) throws IOException {
        HttpPut put = new HttpPut(CHANGE_PASSWORD_URI);
        put.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        StringEntity body = new StringEntity(postBody.toString(), "UTF-8");
        body.setContentType(MIME_TYPE_JSON);
        put.setEntity(body);
        return execute(httpHost, put);
    }

    /**
     * POSTS to the reset password uri (v1/account/reset_password) with
     * an email address
     *
     * @param postBody
     * @return
     * @throws IOException
     */
    public HttpResponse requestResetPasswordRequest(JSONObject postBody) throws IOException {
        HttpPost post = new HttpPost(REQUEST_RESET_PASSWORD_URI);
        StringEntity body = new StringEntity(postBody.toString(), "UTF-8");
        body.setContentType(MIME_TYPE_JSON);
        post.setEntity(body);
        return execute(httpHost, post);
    }

    /**
     * GETS /users/:id
     *
     * @param contactId
     * @return
     * @throws IOException
     */
    public HttpResponse getContact(int contactId) throws IOException {
        HttpGet get = new HttpGet(String.format(GET_CONTACT_URI_PATTERN, API_HOST, contactId));
        get.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(get);
    }

    public HttpResponse getOffice(int officeId) throws IOException {
        HttpGet get = new HttpGet(String.format(GET_OFFICE_URI_PATTERN, API_HOST, officeId));
        get.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(get);
    }

    public HttpResponse getDepartment(int departmentId) throws IOException {
        HttpGet get = new HttpGet(String.format(GET_DEPARTMENT_URI_PATTERN, API_HOST, departmentId));
        get.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        return execute(get);
    }

    /**
     * POSTS to the messaging api host /api/v1/message_threads with
     * a set of recipients
     *
     * @param postBody
     * @return
     * @throws IOException
     */
    public HttpResponse createThreadRequest(JSONObject postBody, int userId) throws IOException {
        HttpPost post = new HttpPost(CREATE_THREAD_URI);
        StringEntity body = new StringEntity(postBody.toString(), "UTF-8");
        body.setContentType(MIME_TYPE_JSON);
        post.setEntity(body);
        if (apiToken != null && !apiToken.isEmpty()) {
            post.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        }
        post.setHeader("Accept", MIME_TYPE_JSON);
        post.setHeader("User-Id", String.valueOf(userId));
        return execute(messagingHttpHost, post);
    }

    /**
     * GETS /api/v1/user/history from messaging api host
     *
     * @param sinceTimeNano retrieve only messages more recent than this timestamp in nanoseconds from epoch
     * @param userId        logged in user id.
     * @return
     * @throws IOException
     */
    public HttpResponse getMessageHistory(long sinceTimeNano, int userId) throws IOException {
        HttpGet get = new HttpGet(String.format(GET_MSG_HISTORY_URI_FORMAT, API_MESSAGING_HOST, sinceTimeNano / 1000000l));
        get.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        get.setHeader("Accept", MIME_TYPE_JSON);
        get.setHeader("User-Id", String.valueOf(userId));
        return execute(messagingHttpHost, get);
    }

    private HttpResponse postHelper(JSONObject postData, String uri) throws IOException {
        return postHelper(postData, uri, httpHost);
    }

    private HttpResponse postHelper(JSONObject postData, String uri, HttpHost host) throws IOException {
        HttpPost post = new HttpPost(uri);
        StringEntity body = new StringEntity(postData.toString(), "UTF-8");
        body.setContentType(MIME_TYPE_JSON);
        post.setEntity(body);
        if (apiToken != null && !apiToken.isEmpty()) {
            post.setHeader(AUTHORIZATION_HEADER_NAME, apiToken);
        }
        return execute(host, post);
    }

}
