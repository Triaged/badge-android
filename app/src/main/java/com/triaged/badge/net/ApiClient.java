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

    public static final String API_MESSAGING_HOST = BuildConfig.API_MESSAGING_SERVER_URL;
    private static final String API_HOST = BuildConfig.API_URL;

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private static final String POST_AVATAR_URI = String.format("%s/v1/account/avatar", API_HOST);

    public static final String USER_AGENT = "Badge-android/" + GeneralUtils.getAppVersionName(App.context());

    String apiToken;

    public ApiClient(String apiToken) {
        super();
        this.getParams().setParameter( CoreProtocolPNames.USER_AGENT, USER_AGENT);
        URI uri = URI.create(String.format("%s", API_MESSAGING_HOST));
        this.apiToken = apiToken;
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
}
