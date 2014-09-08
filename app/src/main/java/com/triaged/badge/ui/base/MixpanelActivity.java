package com.triaged.badge.ui.base;

import android.app.Activity;
import android.os.Bundle;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.app.App;

/**
 *
 * Created by Sadegh Kazemy on 9/8/14.
 */
public abstract class MixpanelActivity extends Activity {

    protected MixpanelAPI mixpanel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mixpanel = MixpanelAPI.getInstance(this, App.MIXPANEL_TOKEN);
    }

    @Override
    protected void onDestroy() {
        mixpanel.flush();
        super.onDestroy();
    }
}
