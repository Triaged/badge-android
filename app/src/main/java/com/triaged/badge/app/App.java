package com.triaged.badge.app;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;

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
import com.triaged.badge.helpers.Foreground;
import com.triaged.badge.net.ApiClient;
import com.triaged.badge.net.DataProviderService;
import com.triaged.badge.net.FayeService;
import com.triaged.badge.net.api.RestService;
import com.triaged.logger.ILogger;
import com.triaged.logger.LoggerImp;
import com.triaged.utils.SharedPreferencesUtil;

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

    public static DataProviderService.LocalBinding dataProviderServiceBinding = null;
    public ServiceConnection dataProviderServiceConnnection = null;

    public Foreground appForeground;
    public Foreground.Listener foregroundListener;

    public static final ILogger gLogger = new LoggerImp(BuildConfig.DEBUG);
    public static Context mContext;
    public static RestAdapter restAdapterMessaging;
    public static RestAdapter restAdapter;
    private static int mAccountId;
    public static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Crashlytics.start(this);
        }
        EventBus.getDefault().register(this);
        mContext = this;

        mAccountId = SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1);
        setupRestAdapter();
        setupDataProviderServicebinding();
        initForeground();
        setupULI();
    }

    private void setupDataProviderServicebinding() {
        dataProviderServiceConnnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                dataProviderServiceBinding = (DataProviderService.LocalBinding) service;
                dataProviderServiceBinding.initDatabase();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        if (!bindService(new Intent(this, DataProviderService.class), dataProviderServiceConnnection, BIND_AUTO_CREATE)) {
            App.gLogger.e("Couldn't bind to data provider service.");
            unbindService(dataProviderServiceConnnection);
        }
    }

    private void initForeground() {
        appForeground = Foreground.get(this);

        foregroundListener = new Foreground.Listener() {
            @Override
            public void onBecameForeground() {
                MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(App.this, MIXPANEL_TOKEN);
                JSONObject props = new JSONObject();
                mixpanelAPI.track("app_foreground", props);
                mixpanelAPI.flush();

                startService(new Intent(getApplicationContext(), FayeService.class));
                if (dataProviderServiceBinding != null) {
                    dataProviderServiceBinding.syncMessagesAsync();
                    dataProviderServiceBinding.partialSyncContactsAsync();
                }
            }

            @Override
            public void onBecameBackground() {
                stopService(new Intent(getApplicationContext(), FayeService.class));
            }
        };
        appForeground.addListener(foregroundListener);
    }

    private void setupRestAdapter() {
        final String authorization = SharedPreferencesUtil.getString("apiToken", "");

        RestAdapter.Builder restBuilderMessaging = new RestAdapter.Builder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestInterceptor.RequestFacade request) {
                        request.addHeader("User-Agent", ApiClient.USER_AGENT);
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
                        request.addHeader("User-Agent", ApiClient.USER_AGENT);
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

    public static int accountId() {
        return mAccountId;
    }

    // Received events from the Event Bus.

    public void onEvent(LogedinSuccessfully event) {
        mAccountId = SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1);
        setupRestAdapter();
        RestService.prepare(restAdapterMessaging, restAdapter);
    }

}
