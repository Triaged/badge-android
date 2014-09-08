package com.triaged.badge.ui.base;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;


public abstract class MixpanelFragment extends Fragment {


    protected MixpanelAPI mixpanel = null;

    public MixpanelFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mixpanel = MixpanelAPI.getInstance(getActivity(), App.MIXPANEL_TOKEN);
    }

    @Override
    public void onDestroy() {
        mixpanel.flush();
        super.onDestroy();
    }
}
