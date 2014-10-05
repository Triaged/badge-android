package com.triaged.badge.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.events.SyncContactPartiallyEvent;
import com.triaged.badge.events.SyncMessageEvent;
import com.triaged.badge.helpers.Foreground;
import com.triaged.badge.net.FayeService;
import com.triaged.badge.net.LongDeserializer;
import com.triaged.badge.net.api.RestService;
import com.triaged.logger.ILogger;
import com.triaged.logger.LoggerImp;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesHelper;

import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.converter.GsonConverter;

/**
 * Custom implementation of the Android Application class that sets up global services and
 * plugins such as Google Analytics and Crashlytics.
 * <p/>
 * Created by Will on 7/7/14.
 */
public class App extends Application {

    public static final String MIXPANEL_TOKEN = "ec6f12813c52d6dc6709aab1bf5cb1b9";

    private static App mInstance;
    private static Handler mHandler;

    public static final ILogger gLogger = new LoggerImp(BuildConfig.DEBUG);
    private static Context mContext;
    public static RestAdapter restAdapterMessaging;
    public static RestAdapter restAdapter;
    private static int mAccountId;
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(long.class, new LongDeserializer())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        if (!BuildConfig.DEBUG) {
            Crashlytics.start(this);
        }
        EventBus.getDefault().register(this);
        mContext = this;
        mHandler = new Handler(getMainLooper());

        SharedPreferencesHelper.prepare(this);
        mAccountId = SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1);
        setupRestAdapter();
        initForeground();
        setupULI();
    }

    private void initForeground() {
        Foreground appForeground = Foreground.get(this);
        appForeground.addListener(new Foreground.Listener() {
            @Override
            public void onBecameForeground() {
                MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(App.this, MIXPANEL_TOKEN);
                JSONObject props = new JSONObject();
                mixpanelAPI.track("app_foreground", props);
                mixpanelAPI.flush();

                startService(new Intent(getApplicationContext(), FayeService.class));

                EventBus.getDefault().post(new SyncMessageEvent());
                EventBus.getDefault().post(new SyncContactPartiallyEvent());
            }

            @Override
            public void onBecameBackground() {
                stopService(new Intent(getApplicationContext(), FayeService.class));
            }
        });
    }

    private void setupRestAdapter() {
        final String authorization = SharedPreferencesHelper.instance().getString(R.string.pref_api_token, "");
        final String userAgent = "Badge-android/" + GeneralUtils.getAppVersionName(App.context());
        RestAdapter.Builder restBuilderMessaging = new RestAdapter.Builder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestInterceptor.RequestFacade request) {
                        request.addHeader("User-Agent", userAgent);
                        request.addHeader("User-Id", mAccountId + "");
                        request.addHeader("Authorization", authorization);
                        request.addHeader("Accept", "*/*");
                    }
                })
                .setEndpoint(BuildConfig.API_MESSAGING_SERVER_URL)
                .setConverter(new GsonConverter(gson))
                .setLog(new AndroidLog("retrofit"));

        RestAdapter.Builder restBuilder = new RestAdapter.Builder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestInterceptor.RequestFacade request) {
                        request.addHeader("User-Agent", userAgent);
                        request.addHeader("User-Id", mAccountId + "");
                        request.addHeader("Authorization", authorization);
                        request.addHeader("Accept", "*/*");
                    }
                })
                .setEndpoint(BuildConfig.API_URL)
                .setConverter(new GsonConverter(gson))
                .setLog(new AndroidLog("retrofit"));

        if (BuildConfig.DEBUG) {
            restBuilderMessaging.setLogLevel(RestAdapter.LogLevel.FULL);
            restBuilder.setLogLevel(RestAdapter.LogLevel.FULL);
        } else {
            restBuilderMessaging.setLogLevel(RestAdapter.LogLevel.NONE);
            restBuilder.setLogLevel(RestAdapter.LogLevel.NONE);
        }

        restAdapterMessaging = restBuilderMessaging.build();
        restAdapter = restBuilder.build();
        RestService.prepare(restAdapterMessaging, restAdapter);
    }

    private void setupULI() {
        DisplayImageOptions displayOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .considerExifParams(true)
                .build();

        ImageLoaderConfiguration.Builder imageLoaderConfigurationBuilder = new ImageLoaderConfiguration.Builder(this);
        if (BuildConfig.DEBUG) {
            imageLoaderConfigurationBuilder.writeDebugLogs();
        }
        ImageLoaderConfiguration imgLoaderConf = imageLoaderConfigurationBuilder
                .defaultDisplayImageOptions(displayOptions)
                .memoryCache(new LruMemoryCache(8 * 1024 * 1024))
                .threadPoolSize(1)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .build();
        ImageLoader.getInstance().init(imgLoaderConf);
    }

    public static Context context() {
        return mContext;
    }

    public static Handler uiHandler() {
        return mHandler;
    }

    public static int accountId() {
        return mAccountId;
    }

    public static App getInstance(){
        return mInstance;
    }

    // Received events from the Event Bus.

    public void onEvent(LogedinSuccessfully event) {
        SyncManager.instance();
        mAccountId = SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1);
        setupRestAdapter();
    }

    public static void toast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

}
