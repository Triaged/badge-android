package com.triaged.badge.app;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;

import com.crashlytics.android.Crashlytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.triaged.badge.common.Config;
import com.triaged.badge.logger.ILogger;
import com.triaged.badge.logger.LoggerImp;

import org.json.JSONObject;

/**
 * Custom implementation of the Android Application class that sets up global services and
 * plugins such as Google Analytics and Crashlytics.
 *
 * Created by Will on 7/7/14.
 */
public class App extends Application {

    private static final String TAG = App.class.getName();
    public static final String MIXPANEL_TOKEN = "ec6f12813c52d6dc6709aab1bf5cb1b9";

    public volatile DataProviderService.LocalBinding dataProviderServiceBinding = null;
    public ServiceConnection dataProviderServiceConnnection = null;

    public Foreground appForeground;
    public Foreground.Listener foregroundListener;

    public static final ILogger gLogger = new LoggerImp(Config.IS_LOGGING_ENABLE);


    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.start(this);

        final Intent fayeServiceIntent = new Intent( getApplicationContext(), FayeService.class );
        appForeground = Foreground.get(this);
        foregroundListener = new Foreground.Listener() {
            @Override
            public void onBecameForeground() {
                MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(App.this, MIXPANEL_TOKEN);
                JSONObject props = new JSONObject();
                mixpanelAPI.track("app_foreground", props);
                mixpanelAPI.flush();
                startService( fayeServiceIntent );
                if( dataProviderServiceBinding != null ) {
                    dataProviderServiceBinding.syncMessagesAsync();
                    dataProviderServiceBinding.partialSyncContactsAsync();
                }
            }

            @Override
            public void onBecameBackground() {
                stopService( fayeServiceIntent );
            }
        };
        appForeground.addListener(foregroundListener);

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
            App.gLogger.e( "Couldn't bind to data provider service." );
            unbindService(dataProviderServiceConnnection);
        }


        setupULI();
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
        if (Config.IS_LOGGING_ENABLE) {
            imageLoaderConfigurationBuilder.writeDebugLogs();
        }
        ImageLoaderConfiguration imgLoaderConf =  imageLoaderConfigurationBuilder
                .defaultDisplayImageOptions(displayOptions)
                .memoryCache(new LruMemoryCache(8 * 1024 * 1024))
                .threadPoolSize(1)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .build();
        ImageLoader.getInstance().init(imgLoaderConf);
    }

    @Override
    public void onTerminate() {
        unbindService(dataProviderServiceConnnection);
        appForeground.removeListener(foregroundListener);
        super.onTerminate();
    }
}
